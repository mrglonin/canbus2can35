package kia.app;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.List;

final class TpmsMonitor {
    static final int TPMS_VENDOR_ID = 1027;
    static final int TPMS_PRODUCT_ID = 24597;
    private static final String ACTION_TPMS_PERMISSION = "kia.app.TPMS_USB_PERMISSION";
    private static final Object LOCK = new Object();
    private static final byte[] WAKE = new byte[]{(byte) 0xAA, 0x41, (byte) 0xA1, 0x07, 0x66, (byte) 0xFF, (byte) 0xF8};

    private static Thread worker;
    private static volatile boolean stop;

    private TpmsMonitor() {
    }

    static void start(Context context) {
        Context app = context.getApplicationContext();
        if (!AppPrefs.tpmsEnabled(app)) {
            stop();
            TpmsState.status(app, "TPMS: раздел выключен в настройках", false);
            return;
        }
        synchronized (LOCK) {
            if (worker != null && worker.isAlive()) return;
            stop = false;
            worker = new Thread(() -> runLoop(app), "KiaTpmsMonitor");
            worker.start();
        }
    }

    static void stop() {
        stop = true;
        synchronized (LOCK) {
            if (worker != null) worker.interrupt();
            worker = null;
        }
    }

    static void restart(Context context) {
        stop();
        start(context);
    }

    static boolean isTpmsDevice(UsbDevice device) {
        return device != null
                && device.getVendorId() == TPMS_VENDOR_ID
                && device.getProductId() == TPMS_PRODUCT_ID;
    }

    private static void runLoop(Context context) {
        while (!stop) {
            UsbSerialPort port = null;
            try {
                UsbManager usb = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                if (usb == null) {
                    TpmsState.status(context, "TPMS: USB-служба недоступна", false);
                    sleep(4000);
                    continue;
                }
                UsbSerialDriver driver = findTpmsDriver(usb);
                if (driver == null) {
                    TpmsState.status(context, "", false);
                    sleep(4000);
                    continue;
                }
                UsbDevice device = driver.getDevice();
                if (!usb.hasPermission(device)) {
                    PendingIntent pi = PendingIntent.getBroadcast(
                            context,
                            11,
                            new Intent(context, UsbPermissionReceiver.class).setAction(ACTION_TPMS_PERMISSION),
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                    );
                    usb.requestPermission(device, pi);
                    TpmsState.status(context, "TPMS: запрошено разрешение USB", false);
                    sleep(4000);
                    continue;
                }
                UsbDeviceConnection connection = usb.openDevice(device);
                if (connection == null) {
                    TpmsState.status(context, "TPMS: не удалось открыть USB-датчик", false);
                    sleep(3000);
                    continue;
                }
                port = driver.getPorts().get(0);
                port.open(connection);
                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                TpmsState.status(context, "TPMS: датчик подключён 9600", true);
                readLoop(context, port);
            } catch (Exception e) {
                TpmsState.status(context, "TPMS: " + e.getClass().getSimpleName() + " " + safe(e.getMessage()), false);
                sleep(3000);
            } finally {
                if (port != null) {
                    try {
                        port.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private static UsbSerialDriver findTpmsDriver(UsbManager usb) {
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usb);
        if (drivers == null) return null;
        for (UsbSerialDriver driver : drivers) {
            if (isTpmsDevice(driver.getDevice())) return driver;
        }
        return null;
    }

    private static void readLoop(Context context, UsbSerialPort port) throws Exception {
        Parser parser = new Parser(context);
        byte[] buffer = new byte[128];
        long nextWake = 0L;
        while (!stop) {
            long now = System.currentTimeMillis();
            if (now >= nextWake) {
                try {
                    port.write(WAKE, 300);
                } catch (Exception ignored) {
                }
                nextWake = now + 5000L;
            }
            int read = port.read(buffer, 600);
            if (read > 0) {
                parser.accept(buffer, read);
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private static final class Parser {
        private final Context context;
        private final byte[] frame = new byte[50];
        private int pos;
        private int checksum;

        Parser(Context context) {
            this.context = context;
        }

        void accept(byte[] data, int length) {
            for (int i = 0; i < length; i++) {
                accept(data[i]);
            }
        }

        private void accept(byte value) {
            int unsigned = value & 0xff;
            switch (pos) {
                case 0:
                    if (unsigned == 0xAA) {
                        frame[pos++] = value;
                    }
                    return;
                case 1:
                    if (unsigned == 0xA1) {
                        frame[pos++] = value;
                    } else {
                        pos = 0;
                    }
                    return;
                case 2:
                    if (unsigned == 0x41) {
                        frame[pos++] = value;
                    } else {
                        pos = 0;
                    }
                    return;
                case 3:
                    if (unsigned == 7 || unsigned == 14 || unsigned == 18) {
                        frame[pos++] = value;
                        checksum = 0xAA + 0xA1 + 0x41 + unsigned;
                    } else {
                        pos = 0;
                    }
                    return;
                default:
                    if (pos >= frame.length) {
                        pos = 0;
                        return;
                    }
                    frame[pos++] = value;
                    int expectedLength = frame[3] & 0xff;
                    if (pos == expectedLength) {
                        if ((checksum & 0xff) == unsigned) {
                            process(expectedLength);
                        }
                        pos = 0;
                    } else {
                        checksum += unsigned;
                    }
            }
        }

        private void process(int length) {
            if (length == 14 && (frame[4] == 0x63 || frame[4] == 0x66)) {
                int index = (frame[5] & 0xff) - 1;
                if (index < 0 || index > 3) return;
                boolean noData = (frame[12] & 0x40) == 0x40 || (frame[11] & 0xff) == 0;
                if (noData) return;
                float pressure = 0.025f * (((frame[9] & 0x03) * 256) + (frame[10] & 0xff));
                int temp = (frame[11] & 0xff) - 50;
                int warning = warning(frame[12] & 0xff);
                boolean lowBattery = (frame[12] & 0x80) == 0x80;
                TpmsState.tire(context, index, pressure, temp, warning, lowBattery);
            }
        }

        private int warning(int flags) {
            if ((flags & 0x03) == 1) return 1;
            if ((flags & 0x10) == 0x10) return 2;
            if ((flags & 0x04) == 0x04) return 3;
            if ((flags & 0x08) == 0x08) return 4;
            if ((flags & 0x80) == 0x80) return 5;
            return 0;
        }
    }
}
