package com.sorento.navi;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ObdMonitor {
    private static final Object LOCK = new Object();
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final Pattern VOLTAGE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*V", Pattern.CASE_INSENSITIVE);

    private static Thread worker;
    private static volatile boolean stop;
    private static volatile boolean clearDtc;
    private static volatile int generation;

    private ObdMonitor() {
    }

    static void start(Context context) {
        Context app = context.getApplicationContext();
        if (!AppPrefs.obdEnabled(app)) {
            stop();
            if (!AppPrefs.debugCan(app)) CanbusControl.stopCanStream(app);
            ObdEmulator.stop();
            ObdState.status(app, "OBD: раздел выключен в настройках", false);
            return;
        }
        if (AppPrefs.obdEmulation(app)) {
            stop();
            ObdEmulator.start(app);
            return;
        }
        ObdEmulator.stop();
        stop = true;
        synchronized (LOCK) {
            if (worker != null) worker.interrupt();
            worker = null;
        }
        AppService.start(app);
        CanbusControl.startCanStream(app);
        ObdState.status(app, "OBD: raw CAN logger 0x70/0x76 активен", true);
        return;
        /*
        synchronized (LOCK) {
            if (worker != null && worker.isAlive()) return;
            stop = false;
            int token = ++generation;
            worker = new Thread(() -> runLoop(app, token), "KiaObdMonitor");
            worker.start();
        }
        */
    }

    static void stop() {
        stop = true;
        generation++;
        ObdEmulator.stop();
        synchronized (LOCK) {
            if (worker != null) worker.interrupt();
            worker = null;
        }
    }

    static void stop(Context context) {
        stop();
        if (context != null && !AppPrefs.debugCan(context)) CanbusControl.stopCanStream(context);
    }

    static void restart(Context context) {
        stop();
        start(context);
    }

    static void clearDtc(Context context) {
        if (context != null && !AppPrefs.obdEmulation(context)) {
            clearDtc = false;
            ObdState.dtc(context, new ArrayList<>());
            ObdState.status(context, "DTC: список ошибок очищен локально", true);
            AppLog.line(context, "DTC: локальная очистка; ECU clear через CAN sideband пока не задан протоколом");
            return;
        }
        clearDtc = true;
        start(context);
        ObdState.status(context, "Kia Canbus: команда очистки ошибок поставлена в очередь", false);
    }

    private static boolean active(int token) {
        return !stop && token == generation;
    }

    private static void runLoop(Context context, int token) {
        while (active(token)) {
            BluetoothSocket socket = null;
            try {
                if (!hasBluetoothPermission(context)) {
                    ObdState.status(context, "Kia Canbus: нет разрешения подключения", false);
                    sleep(3000);
                    continue;
                }
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null) {
                    ObdState.status(context, "Kia Canbus: адаптер недоступен", false);
                    return;
                }
                if (!adapter.isEnabled()) {
                    ObdState.status(context, "Kia Canbus: включите адаптер подключения", false);
                    sleep(3000);
                    continue;
                }
                BluetoothDevice target = findObd(adapter);
                if (target == null) {
                    ObdState.status(context, "", false);
                    sleep(4000);
                    continue;
                }
                ObdState.status(context, "Kia Canbus: подключение к " + safeName(target), false);
                adapter.cancelDiscovery();
                socket = target.createRfcommSocketToServiceRecord(SPP);
                socket.connect();
                ObdState.status(context, "Kia Canbus: подключено " + safeName(target), true);
                initElm(socket);
                int cycle = 0;
                while (active(token) && socket.isConnected()) {
                    poll(context, socket, cycle++);
                    sleep(900);
                }
            } catch (SecurityException e) {
                if (active(token)) ObdState.status(context, "Kia Canbus: подключение заблокировано разрешениями", false);
                sleep(3000);
            } catch (Exception e) {
                if (active(token)) ObdState.status(context, "Kia Canbus: " + e.getClass().getSimpleName() + " " + safeMessage(e), false);
                sleep(3000);
            } finally {
                close(socket);
            }
        }
    }

    private static boolean hasBluetoothPermission(Context context) {
        return Build.VERSION.SDK_INT < 31
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private static BluetoothDevice findObd(BluetoothAdapter adapter) {
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        BluetoothDevice fallback = null;
        if (devices == null) return null;
        for (BluetoothDevice device : devices) {
            String name = safeName(device).toUpperCase(Locale.US);
            if (fallback == null) fallback = device;
            if (name.contains("OBD") || name.contains("ELM")) return device;
        }
        return null;
    }

    private static void initElm(BluetoothSocket socket) throws IOException {
        command(socket, "ATZ", 1600);
        command(socket, "ATE0", 900);
        command(socket, "ATL0", 900);
        command(socket, "ATS0", 900);
        command(socket, "ATH0", 900);
        command(socket, "ATAT1", 900);
        command(socket, "ATSP0", 1200);
    }

    private static void poll(Context context, BluetoothSocket socket, int cycle) throws IOException {
        parseSpeed(context, command(socket, "010D", 900));
        parseRpm(context, command(socket, "010C", 900));
        parseRuntime(context, command(socket, "011F", 900));
        parseVoltage(context, command(socket, "ATRV", 900));
        parseCoolant(context, command(socket, "0105", 900));
        parseLoad(context, command(socket, "0104", 900));
        parseThrottle(context, command(socket, "0111", 900));
        parseIntake(context, command(socket, "010F", 900));
        parseFuelRate(context, command(socket, "015E", 900));
        if (clearDtc) {
            clearDtc = false;
            command(socket, "04", 1200);
            ObdState.dtc(context, new ArrayList<>());
            AppLog.line(context, "Kia Canbus: команда очистки DTC отправлена");
        } else if (cycle % 8 == 0) {
            parseDtc(context, command(socket, "03", 1200));
        }
    }

    private static String command(BluetoothSocket socket, String value, int timeoutMs) throws IOException {
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        try {
            while (in.available() > 0) in.read();
        } catch (IOException ignored) {
        }
        out.write((value + "\r").getBytes("US-ASCII"));
        out.flush();
        StringBuilder response = new StringBuilder();
        long until = SystemClock.uptimeMillis() + timeoutMs;
        byte[] buffer = new byte[128];
        while (SystemClock.uptimeMillis() < until && !stop) {
            int available = in.available();
            if (available > 0) {
                int read = in.read(buffer, 0, Math.min(buffer.length, available));
                if (read > 0) {
                    String part = new String(buffer, 0, read, "US-ASCII");
                    response.append(part);
                    if (part.indexOf('>') >= 0) break;
                }
            } else {
                sleep(20);
            }
        }
        return response.toString();
    }

    private static void parseSpeed(Context context, String response) {
        int a = byteAfter(response, "410D", 0);
        if (a >= 0) ObdState.speed(context, a);
    }

    private static void parseRpm(Context context, String response) {
        int a = byteAfter(response, "410C", 0);
        int b = byteAfter(response, "410C", 1);
        if (a >= 0 && b >= 0) ObdState.rpm(context, ((a * 256) + b) / 4);
    }

    private static void parseRuntime(Context context, String response) {
        int a = byteAfter(response, "411F", 0);
        int b = byteAfter(response, "411F", 1);
        if (a >= 0 && b >= 0) ObdState.runtime(context, (a * 256) + b);
    }

    private static void parseVoltage(Context context, String response) {
        Matcher matcher = VOLTAGE.matcher(response);
        if (matcher.find()) {
            try {
                ObdState.voltage(context, Float.parseFloat(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static void parseCoolant(Context context, String response) {
        int a = byteAfter(response, "4105", 0);
        if (a >= 0) ObdState.coolant(context, a - 40);
    }

    private static void parseLoad(Context context, String response) {
        int a = byteAfter(response, "4104", 0);
        if (a >= 0) ObdState.load(context, Math.round(a * 100f / 255f));
    }

    private static void parseThrottle(Context context, String response) {
        int a = byteAfter(response, "4111", 0);
        if (a >= 0) ObdState.throttle(context, Math.round(a * 100f / 255f));
    }

    private static void parseIntake(Context context, String response) {
        int a = byteAfter(response, "410F", 0);
        if (a >= 0) ObdState.intake(context, a - 40);
    }

    private static void parseFuelRate(Context context, String response) {
        int a = byteAfter(response, "415E", 0);
        int b = byteAfter(response, "415E", 1);
        if (a >= 0 && b >= 0) ObdState.fuelRate(context, ((a * 256) + b) / 20f);
    }

    private static void parseDtc(Context context, String response) {
        String hex = hexOnly(response);
        int idx = hex.indexOf("43");
        if (idx < 0) return;
        List<String> codes = new ArrayList<>();
        for (int i = idx + 2; i + 3 < hex.length(); i += 4) {
            int a = parseByte(hex, i);
            int b = parseByte(hex, i + 2);
            if (a <= 0 && b <= 0) break;
            codes.add(toDtc(a, b));
        }
        ObdState.dtc(context, codes);
    }

    private static int byteAfter(String response, String marker, int offset) {
        String hex = hexOnly(response);
        int idx = hex.indexOf(marker);
        if (idx < 0) return -1;
        int pos = idx + marker.length() + offset * 2;
        if (pos + 2 > hex.length()) return -1;
        return parseByte(hex, pos);
    }

    private static String toDtc(int first, int second) {
        char family;
        switch ((first & 0xC0) >> 6) {
            case 1:
                family = 'C';
                break;
            case 2:
                family = 'B';
                break;
            case 3:
                family = 'U';
                break;
            default:
                family = 'P';
                break;
        }
        int d1 = (first & 0x30) >> 4;
        int d2 = first & 0x0F;
        int d3 = (second & 0xF0) >> 4;
        int d4 = second & 0x0F;
        return "" + family + d1 + Integer.toHexString(d2).toUpperCase(Locale.US)
                + Integer.toHexString(d3).toUpperCase(Locale.US)
                + Integer.toHexString(d4).toUpperCase(Locale.US);
    }

    private static int parseByte(String hex, int pos) {
        try {
            return Integer.parseInt(hex.substring(pos, pos + 2), 16);
        } catch (Exception e) {
            return -1;
        }
    }

    private static String hexOnly(String value) {
        return value == null ? "" : value.toUpperCase(Locale.US).replaceAll("[^0-9A-F]", "");
    }

    private static String safeName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name == null ? device.getAddress() : name;
        } catch (SecurityException e) {
            return "Kia Canbus";
        }
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null ? "" : message;
    }

    private static void close(BluetoothSocket socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
