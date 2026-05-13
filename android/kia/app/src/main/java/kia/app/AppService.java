package kia.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class AppService extends Service {
    private static final String CHANNEL = "sportage_connection";
    private static final int NOTIFICATION_ID = 41;
    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_QUIET = "quiet";
    private static final String ACTION_REFRESH_OVERLAYS = "kia.app.REFRESH_OVERLAYS";
    private static final String ACTION_USB_PERMISSION = "kia.app.USB_PERMISSION";
    private static final int FTDI_VENDOR_ID = 1027;
    private static final int FTDI_CANBOX_PRODUCT_ID = 24577;
    private static final int STM_VENDOR_ID = 1155;
    private static final int STM_CDC_PRODUCT_ID = 22336;
    private static final int RAW_CAN_BURST = 3;
    private static final Queue<byte[]> PENDING = new ArrayDeque<>();
    private static volatile AppService active;

    private UsbManager usbManager;
    private WindowManager windowManager;
    private UsbSerialPort port;
    private Thread readThread;
    private volatile boolean readRunning;
    private volatile boolean serviceRunning;
    private long lastDebugCanStartAt;
    private long lastOverlayPermissionLogAt;
    private long lastQuietConnectAt;
    private UartOverlayView systemUartOverlay;
    private MusicOverlayView systemMusicOverlay;
    private NavOverlayView systemNavOverlay;
    private BlindSpotOverlayView systemBlindSpotOverlay;
    private final ByteArrayOutputStream incoming = new ByteArrayOutputStream();
    private final Handler debugHandler = new Handler(Looper.getMainLooper());
    private HandlerThread ioThread;
    private Handler ioHandler;
    private final Runnable debugPoll = new Runnable() {
        @Override
        public void run() {
            ensureDebugCanStream();
            refreshSystemOverlays();
            debugHandler.postDelayed(this, 300L);
        }
    };
    private final Runnable rawCanPoll = new Runnable() {
        @Override
        public void run() {
            if (!serviceRunning) return;
            boolean needed = AppPrefs.obdEnabled(AppService.this)
                    || AppPrefs.debugCan(AppService.this)
                    || AppPrefs.blindSpotEnabled(AppService.this);
            if (needed) {
                for (int i = 0; i < RAW_CAN_BURST; i++) {
                    writeOrQueue(sidebandPacket(0x76, null), true);
                }
            }
            Handler handler = ioHandler == null ? debugHandler : ioHandler;
            handler.postDelayed(this, needed ? 20L : 500L);
        }
    };

    static void start(Context context) {
        Intent intent = new Intent(context, AppService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            AppLog.line(context, "Сервис: запуск заблокирован " + e.getClass().getSimpleName());
        }
    }

    static void sendFrame(Context context, byte[] data) {
        sendFrame(context, data, false);
    }

    static void sendFrameQuiet(Context context, byte[] data) {
        sendFrame(context, data, true);
    }

    private static void sendFrame(Context context, byte[] data, boolean quiet) {
        AppService service = active;
        if (service != null) {
            service.enqueueWrite(data, quiet);
            return;
        }
        Intent intent = new Intent(context, AppService.class);
        intent.putExtra(EXTRA_DATA, data);
        intent.putExtra(EXTRA_QUIET, quiet);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            AppLog.line(context, "Сервис CAN: запуск заблокирован " + e.getClass().getSimpleName());
        }
    }

    static void refreshOverlays(Context context) {
        Intent intent = new Intent(context, AppService.class);
        intent.setAction(ACTION_REFRESH_OVERLAYS);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            AppLog.line(context, "Overlay: запуск сервиса заблокирован " + e.getClass().getSimpleName());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        active = this;
        serviceRunning = true;
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        AppPrefs.setObdEmulation(this, false);
        ObdEmulator.stop();
        startForeground(NOTIFICATION_ID, notification("Запуск подключения"));
        TeyesMediaBridge.start(this);
        MediaMonitor.start(this);
        CompassBridge.start(this);
        ObdMonitor.start(this);
        ioThread = new HandlerThread("kia-canbox-io");
        ioThread.start();
        ioHandler = new Handler(ioThread.getLooper());
        ensureDebugCanStream();
        TpmsMonitor.start(this);
        connectUsb();
        refreshSystemOverlays();
        debugHandler.postDelayed(debugPoll, 300L);
        ioHandler.postDelayed(rawCanPoll, 100L);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, notification("Kia Canbus / TPMS / мультимедиа активны"));
        if (intent != null && ACTION_REFRESH_OVERLAYS.equals(intent.getAction())) {
            refreshSystemOverlays();
        } else if (intent != null && intent.hasExtra(EXTRA_DATA)) {
            byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
            boolean quiet = intent.getBooleanExtra(EXTRA_QUIET, false);
            if (data != null) writeOrQueue(data, quiet);
        } else {
            connectUsb();
            MediaMonitor.scanNow(this);
            ensureDebugCanStream();
            TpmsMonitor.start(this);
            refreshSystemOverlays();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        serviceRunning = false;
        active = null;
        debugHandler.removeCallbacks(debugPoll);
        if (ioHandler != null) ioHandler.removeCallbacks(rawCanPoll);
        debugHandler.removeCallbacks(rawCanPoll);
        if (ioThread != null) {
            ioThread.quitSafely();
            ioThread = null;
            ioHandler = null;
        }
        removeSystemOverlays();
        closePort();
        TeyesMediaBridge.stop(this);
        MediaMonitor.stop();
        CompassBridge.stop();
        ObdMonitor.stop(this);
        TpmsMonitor.stop();
        super.onDestroy();
    }

    private Notification notification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Kia connection", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }
        PendingIntent open = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Kia")
                .setContentText(text)
                .setContentIntent(open)
                .setOngoing(true)
                .build();
    }

    private void ensureDebugCanStream() {
        if (!AppPrefs.debugCan(this) && !AppPrefs.obdEnabled(this) && !AppPrefs.blindSpotEnabled(this)) return;
        long now = System.currentTimeMillis();
        if (now - lastDebugCanStartAt < 5000L) return;
        lastDebugCanStartAt = now;
        writeOrQueue(sidebandPacket(0x70, new byte[]{0x01}), true);
    }

    private void refreshSystemOverlays() {
        boolean uart = AppPrefs.debugUart(this) && AppPrefs.uartOverlay(this);
        boolean music = AppPrefs.mediaOverlay(this);
        boolean nav = AppPrefs.navOverlay(this);
        boolean blind = AppPrefs.blindSpotEnabled(this) && AppPrefs.blindSpotOverlay(this);
        if (!uart) removeOverlay(systemUartOverlay);
        if (!music) removeOverlay(systemMusicOverlay);
        if (!nav) removeOverlay(systemNavOverlay);
        if (!blind) removeOverlay(systemBlindSpotOverlay);
        if (!uart) systemUartOverlay = null;
        if (!music) systemMusicOverlay = null;
        if (!nav) systemNavOverlay = null;
        if (!blind) systemBlindSpotOverlay = null;
        if (!uart && !music && !nav && !blind) return;

        if (!PermissionHelper.canDrawOverlays(this)) {
            long now = System.currentTimeMillis();
            if (now - lastOverlayPermissionLogAt > 15000L) {
                lastOverlayPermissionLogAt = now;
                AppLog.line(this, "Overlay: нужно разрешение Android поверх других окон");
            }
            return;
        }
        if (uart && systemUartOverlay == null) {
            systemUartOverlay = new UartOverlayView(this);
            addOverlay(systemUartOverlay);
        }
        if (music && systemMusicOverlay == null) {
            systemMusicOverlay = new MusicOverlayView(this);
            addOverlay(systemMusicOverlay);
        }
        if (nav && systemNavOverlay == null) {
            systemNavOverlay = new NavOverlayView(this);
            addOverlay(systemNavOverlay);
        }
        if (blind && systemBlindSpotOverlay == null) {
            systemBlindSpotOverlay = new BlindSpotOverlayView(this);
            addOverlay(systemBlindSpotOverlay);
        }
        if (systemUartOverlay != null) systemUartOverlay.invalidate();
        if (systemMusicOverlay != null) systemMusicOverlay.invalidate();
        if (systemNavOverlay != null) systemNavOverlay.invalidate();
        if (systemBlindSpotOverlay != null) systemBlindSpotOverlay.refreshFromState();
    }

    private void addOverlay(View view) {
        if (windowManager == null || view == null || view.getParent() != null) return;
        try {
            int type = Build.VERSION.SDK_INT >= 26
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            lp.gravity = Gravity.START | Gravity.TOP;
            windowManager.addView(view, lp);
        } catch (Exception e) {
            AppLog.line(this, "Overlay: ошибка " + e.getClass().getSimpleName());
        }
    }

    private void removeSystemOverlays() {
        removeOverlay(systemUartOverlay);
        removeOverlay(systemMusicOverlay);
        removeOverlay(systemNavOverlay);
        removeOverlay(systemBlindSpotOverlay);
        systemUartOverlay = null;
        systemMusicOverlay = null;
        systemNavOverlay = null;
        systemBlindSpotOverlay = null;
    }

    private void removeOverlay(View view) {
        if (windowManager == null || view == null || view.getParent() == null) return;
        try {
            windowManager.removeView(view);
        } catch (Exception ignored) {
        }
    }

    private synchronized void connectUsb() {
        if (port != null || usbManager == null) return;
        try {
            List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
            if (drivers == null || drivers.isEmpty()) {
                AppLog.setUsb(this, "");
                return;
            }
            UsbSerialDriver driver = findCanboxDriver(drivers);
            if (driver == null) {
                AppLog.setUsb(this, "");
                return;
            }
            UsbDevice device = driver.getDevice();
            if (!usbManager.hasPermission(device)) {
                Intent intent = new Intent(this, UsbPermissionReceiver.class);
                intent.setAction(ACTION_USB_PERMISSION);
                PendingIntent pi = PendingIntent.getBroadcast(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                );
                usbManager.requestPermission(device, pi);
                AppLog.setUsb(this, "Kia Canbus: запрошено разрешение USB " + device.getDeviceName());
                return;
            }
            UsbDeviceConnection connection = usbManager.openDevice(device);
            if (connection == null) {
                AppLog.setUsb(this, "Kia Canbus: не удалось открыть USB-адаптер");
                return;
            }
            port = driver.getPorts().get(0);
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            AppLog.setUsb(this, "Kia Canbus: подключено " + device.getDeviceName() + " 115200 8N1");
            startReader();
            flushQueue();
        } catch (Exception e) {
            closePort();
            AppLog.setUsb(this, "Kia Canbus: ошибка " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    private UsbSerialDriver findCanboxDriver(List<UsbSerialDriver> drivers) {
        UsbSerialDriver fallback = null;
        for (UsbSerialDriver candidate : drivers) {
            UsbDevice device = candidate.getDevice();
            if (TpmsMonitor.isTpmsDevice(device)) continue;
            if (isKnownCanbox(device)) return candidate;
            if (fallback == null) fallback = candidate;
        }
        return fallback;
    }

    private boolean isKnownCanbox(UsbDevice device) {
        if (device == null) return false;
        int vendor = device.getVendorId();
        int product = device.getProductId();
        return (vendor == FTDI_VENDOR_ID && product == FTDI_CANBOX_PRODUCT_ID)
                || (vendor == STM_VENDOR_ID && product == STM_CDC_PRODUCT_ID);
    }

    private void enqueueWrite(byte[] data, boolean quiet) {
        if (data == null) return;
        byte[] copy = Arrays.copyOf(data, data.length);
        Handler handler = ioHandler;
        if (handler != null) {
            handler.post(() -> writeOrQueue(copy, quiet));
        } else {
            writeOrQueue(copy, quiet);
        }
    }

    private void writeOrQueue(byte[] data) {
        writeOrQueue(data, false);
    }

    private synchronized void writeOrQueue(byte[] data, boolean quiet) {
        if (data == null) return;
        if (port == null) {
            if (quiet) {
                long now = System.currentTimeMillis();
                if (now - lastQuietConnectAt > 1500L) {
                    lastQuietConnectAt = now;
                    connectUsb();
                }
                return;
            }
            if (!quiet) {
                synchronized (PENDING) {
                    if (PENDING.size() > 60) PENDING.poll();
                    PENDING.offer(data);
                }
            }
            connectUsb();
            if (!quiet) AppLog.line(this, "CAN кадр в очереди " + hex(data));
            return;
        }
        try {
            port.write(data, 300);
            if (!quiet) AppLog.line(this, "CAN кадр отправлен " + hex(data));
        } catch (IOException e) {
            if (!quiet) {
                synchronized (PENDING) {
                    if (PENDING.size() > 60) PENDING.poll();
                    PENDING.offer(data);
                }
            }
            closePort();
            if (!quiet) AppLog.setUsb(this, "Kia Canbus: запись не прошла, переподключение");
            connectUsb();
        }
    }

    private void flushQueue() {
        while (true) {
            byte[] frame;
            synchronized (PENDING) {
                frame = PENDING.poll();
            }
            if (frame == null) return;
            writeOrQueue(frame);
            if (port == null) return;
        }
    }

    private synchronized void closePort() {
        readRunning = false;
        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }
        if (port != null) {
            try {
                port.close();
            } catch (Exception ignored) {
            }
            port = null;
        }
        synchronized (incoming) {
            incoming.reset();
        }
    }

    private void startReader() {
        if (readThread != null && readThread.isAlive()) return;
        UsbSerialPort active = port;
        if (active == null) return;
        readRunning = true;
        readThread = new Thread(() -> {
            byte[] buffer = new byte[128];
            while (readRunning && active == port) {
                try {
                    int read = active.read(buffer, 500);
                    if (read > 0) appendIncoming(buffer, read);
                } catch (IOException e) {
                    if (readRunning) {
                        AppLog.setUsb(this, "Kia Canbus: чтение остановлено, переподключение");
                        closePort();
                        connectUsb();
                    }
                    return;
                }
            }
        }, "kia-canbus-reader");
        readThread.start();
    }

    private void appendIncoming(byte[] data, int count) {
        synchronized (incoming) {
            incoming.write(data, 0, count);
            parseIncoming();
        }
    }

    private void parseIncoming() {
        byte[] data = incoming.toByteArray();
        int index = 0;
        while (index <= data.length - 5) {
            int start = findHeader(data, index);
            if (start < 0) {
                index = data.length;
                break;
            }
            if (data.length - start < 5) {
                index = start;
                break;
            }
            int len = data[start + 3] & 0xff;
            if (len < 6 || len > 64) {
                index = start + 1;
                continue;
            }
            if (data.length - start < len) {
                index = start;
                break;
            }
            byte[] frame = Arrays.copyOfRange(data, start, start + len);
            if (validChecksum(frame)) {
                int id = frame[4] & 0xff;
                if (id != 0x76 && id != 0x71 && id != 0x72) {
                    AppLog.line(this, "CAN кадр получен " + CanbusControl.hex(frame));
                }
                CanbusControl.handleIncomingFrame(this, frame);
                index = start + len;
            } else {
                AppLog.line(this, "CAN кадр с неверной суммой " + CanbusControl.hex(frame));
                index = start + 1;
            }
        }
        incoming.reset();
        if (index < data.length) {
            incoming.write(data, index, data.length - index);
        }
    }

    private int findHeader(byte[] data, int from) {
        for (int i = from; i <= data.length - 3; i++) {
            if ((data[i] & 0xff) != 0xBB) continue;
            int second = data[i + 1] & 0xff;
            int third = data[i + 2] & 0xff;
            if ((second == 0xA1 && third == 0x41) || (second == 0x41 && third == 0xA1)) {
                return i;
            }
        }
        return -1;
    }

    private boolean validChecksum(byte[] frame) {
        if (frame == null || frame.length < 2) return false;
        int sum = 0;
        for (int i = 0; i < frame.length - 1; i++) {
            sum += frame[i] & 0xff;
        }
        return (sum & 0xff) == (frame[frame.length - 1] & 0xff);
    }

    private static byte[] sidebandPacket(int id, byte[] payload) {
        int size = payload == null ? 0 : payload.length;
        byte[] data = new byte[6 + size];
        data[0] = (byte) 0xBB;
        data[1] = 0x41;
        data[2] = (byte) 0xA1;
        data[3] = (byte) data.length;
        data[4] = (byte) id;
        if (size > 0) System.arraycopy(payload, 0, data, 5, size);
        int sum = 0;
        for (int i = 0; i < data.length - 1; i++) {
            sum += data[i] & 0xff;
        }
        data[data.length - 1] = (byte) sum;
        return data;
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            if (sb.length() > 0) sb.append(' ');
            int v = b & 0xff;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v).toUpperCase());
        }
        return sb.toString();
    }
}
