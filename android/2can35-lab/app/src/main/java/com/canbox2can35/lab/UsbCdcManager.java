package com.canbox2can35.lab;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class UsbCdcManager {
    private static final String ACTION_USB_PERMISSION = "com.canbox2can35.lab.USB_PERMISSION";
    private static final int GS_VID = 0x1D50;
    private static final int GS_PID = 0x606F;
    private static final int GS_USB_BREQ_HOST_FORMAT = 0;
    private static final int GS_USB_BREQ_BITTIMING = 1;
    private static final int GS_USB_BREQ_MODE = 2;
    private static final int GS_USB_BREQ_BT_CONST = 4;
    private static final int GS_USB_BREQ_DEVICE_CONFIG = 5;
    private static final int GS_USB_BREQ_2CAN35_UART_STATUS = 0x70;
    private static final int GS_USB_BREQ_2CAN35_UART_READ = 0x71;
    private static final int GS_USB_BREQ_2CAN35_UART_WRITE = 0x72;
    private static final int GS_USB_BREQ_2CAN35_UART_INIT = 0x73;
    private static final int GS_USB_BREQ_2CAN35_EXIT_MODE1 = 0x7F;
    private static final int GS_CAN_MODE_RESET = 0;
    private static final int GS_CAN_MODE_START = 1;
    private static final int GS_CAN_FEATURE_HW_TIMESTAMP = 1 << 4;
    private static final int CAN_EFF_FLAG = 0x80000000;
    private static final int CAN_RTR_FLAG = 0x40000000;
    private static final int CAN_ERR_FLAG = 0x20000000;

    private final Context context;
    private final UsbManager manager;
    private final Object ioLock = new Object();
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbInterface dataInterface;
    private UsbEndpoint epIn;
    private UsbEndpoint epOut;
    private Thread rxThread;
    private volatile boolean running;
    private volatile boolean gsUsbMode;
    private volatile int gsFclk = 36000000;
    private volatile String lastError = "";
    private volatile long rxCount = 0;
    private volatile long txCount = 0;

    public interface LineListener {
        void onLine(String line);
    }

    private LineListener listener;

    public UsbCdcManager(Context context) {
        this.context = context.getApplicationContext();
        this.manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    public synchronized void setLineListener(LineListener listener) {
        this.listener = listener;
    }

    public synchronized boolean openFirst() {
        close();
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        for (UsbDevice candidate : devices.values()) {
            if (looksLike2Can35(candidate) && open(candidate)) return true;
        }
        lastError = "2CAN35 USB device not found";
        return false;
    }

    private boolean looksLike2Can35(UsbDevice candidate) {
        if (candidate.getVendorId() == 0x0483 && candidate.getProductId() == 0x5740) return true;
        if (candidate.getVendorId() == GS_VID && candidate.getProductId() == GS_PID) return true;
        for (int i = 0; i < candidate.getInterfaceCount(); i++) {
            UsbInterface itf = candidate.getInterface(i);
            if (itf.getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA ||
                    itf.getInterfaceClass() == UsbConstants.USB_CLASS_COMM) return true;
        }
        return false;
    }

    private boolean open(UsbDevice candidate) {
        if (!manager.hasPermission(candidate)) {
            Intent intent = new Intent(ACTION_USB_PERMISSION);
            int flags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
            manager.requestPermission(candidate, permissionIntent);
            lastError = "USB permission requested";
            return false;
        }
        if (candidate.getVendorId() == GS_VID && candidate.getProductId() == GS_PID) {
            return openGsUsb(candidate);
        }
        return openCdc(candidate);
    }

    private boolean openCdc(UsbDevice candidate) {
        UsbInterface best = null;
        UsbEndpoint in = null;
        UsbEndpoint out = null;
        for (int i = 0; i < candidate.getInterfaceCount(); i++) {
            UsbInterface itf = candidate.getInterface(i);
            UsbEndpoint maybeIn = null;
            UsbEndpoint maybeOut = null;
            for (int e = 0; e < itf.getEndpointCount(); e++) {
                UsbEndpoint ep = itf.getEndpoint(e);
                if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) continue;
                if (ep.getDirection() == UsbConstants.USB_DIR_IN) maybeIn = ep;
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) maybeOut = ep;
            }
            if (maybeIn != null && maybeOut != null) {
                best = itf;
                in = maybeIn;
                out = maybeOut;
                break;
            }
        }
        if (best == null) {
            lastError = "CDC bulk endpoints not found";
            return false;
        }

        UsbDeviceConnection conn = manager.openDevice(candidate);
        if (conn == null || !conn.claimInterface(best, true)) {
            lastError = "cannot claim USB interface";
            return false;
        }

        device = candidate;
        connection = conn;
        dataInterface = best;
        epIn = in;
        epOut = out;
        gsUsbMode = false;
        setLineCoding115200();
        startCdcRx();
        lastError = "";
        return true;
    }

    private boolean openGsUsb(UsbDevice candidate) {
        UsbInterface best = null;
        UsbEndpoint in = null;
        UsbEndpoint out = null;
        for (int i = 0; i < candidate.getInterfaceCount(); i++) {
            UsbInterface itf = candidate.getInterface(i);
            UsbEndpoint maybeIn = null;
            UsbEndpoint maybeOut = null;
            for (int e = 0; e < itf.getEndpointCount(); e++) {
                UsbEndpoint ep = itf.getEndpoint(e);
                if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) continue;
                if (ep.getDirection() == UsbConstants.USB_DIR_IN) maybeIn = ep;
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) maybeOut = ep;
            }
            if (maybeIn != null && maybeOut != null) {
                best = itf;
                in = maybeIn;
                out = maybeOut;
                break;
            }
        }
        if (best == null) {
            lastError = "gs_usb bulk endpoints not found";
            return false;
        }

        UsbDeviceConnection conn = manager.openDevice(candidate);
        if (conn == null || !conn.claimInterface(best, true)) {
            lastError = "cannot claim gs_usb interface";
            return false;
        }

        device = candidate;
        connection = conn;
        dataInterface = best;
        epIn = in;
        epOut = out;
        gsUsbMode = true;
        try {
            initGsUsb();
            startGsUsbRx();
            lastError = "";
            return true;
        } catch (Exception ex) {
            lastError = "gs_usb init failed: " + ex.getMessage();
            close();
            return false;
        }
    }

    private void setLineCoding115200() {
        if (connection == null) return;
        byte[] lineCoding = new byte[]{
                0x00, (byte) 0xC2, 0x01, 0x00,
                0x00,
                0x00,
                0x08
        };
        connection.controlTransfer(0x21, 0x20, 0, 0, lineCoding, lineCoding.length, 500);
        connection.controlTransfer(0x21, 0x22, 3, 0, null, 0, 500);
    }

    private void startCdcRx() {
        running = true;
        rxThread = new Thread(() -> {
            byte[] buf = new byte[256];
            StringBuilder line = new StringBuilder();
            while (running && connection != null && epIn != null) {
                int n = connection.bulkTransfer(epIn, buf, buf.length, 200);
                if (n <= 0) continue;
                rxCount += n;
                String chunk = new String(buf, 0, n, StandardCharsets.US_ASCII);
                for (int i = 0; i < chunk.length(); i++) {
                    char c = chunk.charAt(i);
                    if (c == '\n' || c == '\r') {
                        if (line.length() > 0) {
                            LineListener current = listener;
                            if (current != null) current.onLine(line.toString());
                            line.setLength(0);
                        }
                    } else {
                        line.append(c);
                    }
                }
            }
        }, "2can35-usb-rx");
        rxThread.setDaemon(true);
        rxThread.start();
    }

    private void startGsUsbRx() {
        running = true;
        rxThread = new Thread(() -> {
            byte[] packet = new byte[64];
            long lastUartPoll = 0;
            while (running && connection != null && epIn != null) {
                long now = System.currentTimeMillis();
                if (now - lastUartPoll >= 50) {
                    lastUartPoll = now;
                    pollUartSideband();
                }
                int n;
                synchronized (ioLock) {
                    if (!running || connection == null || epIn == null) break;
                    n = connection.bulkTransfer(epIn, packet, packet.length, 50);
                }
                if (n <= 0) continue;
                String line = formatGsUsbFrame(packet, n);
                if (line != null) {
                    LineListener current = listener;
                    if (current != null) current.onLine(line);
                }
            }
        }, "2can35-gsusb-rx");
        rxThread.setDaemon(true);
        rxThread.start();
    }

    public synchronized boolean writeLine(String line) {
        if (connection == null || epOut == null) {
            if (!openFirst()) return false;
        }
        if (gsUsbMode) return writeGsUsbLine(line);
        byte[] bytes = (line + "\r").getBytes(StandardCharsets.US_ASCII);
        int n = connection.bulkTransfer(epOut, bytes, bytes.length, 500);
        if (n > 0) txCount += n;
        return n == bytes.length;
    }

    private boolean writeGsUsbLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        try {
            if (trimmed.equalsIgnoreCase("mode normal") || trimmed.equalsIgnoreCase("mode canbox")) {
                synchronized (ioLock) {
                    gsCtrlOut(GS_USB_BREQ_2CAN35_EXIT_MODE1, 0, new byte[0]);
                }
                return true;
            }
            if (trimmed.startsWith("U ")) {
                byte[] payload = parseHex(trimmed.substring(2));
                synchronized (ioLock) {
                    int n = gsCtrlOut(GS_USB_BREQ_2CAN35_UART_WRITE, 0, payload);
                    if (n >= 0) txCount += payload.length;
                    return n >= 0;
                }
            }
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length == 2 && parts[0].matches("[01]")) {
                byte[] packet = packGsHostFrame(Integer.parseInt(parts[0]), parts[1], 1);
                synchronized (ioLock) {
                    int n = connection.bulkTransfer(epOut, packet, packet.length, 500);
                    return n == packet.length;
                }
            }
        } catch (Exception ex) {
            lastError = "write failed: " + ex.getMessage();
        }
        return false;
    }

    public synchronized void close() {
        running = false;
        if (connection != null && dataInterface != null) {
            try {
                connection.releaseInterface(dataInterface);
            } catch (Exception ignored) {
            }
        }
        if (connection != null) connection.close();
        connection = null;
        dataInterface = null;
        epIn = null;
        epOut = null;
        device = null;
        gsUsbMode = false;
    }

    public boolean isOpen() {
        return connection != null;
    }

    public String label() {
        if (device == null) return lastError.isEmpty() ? "no USB" : lastError;
        String prefix = gsUsbMode ? "gs_usb" : "USB";
        return String.format(Locale.ROOT, "%s %04x:%04x", prefix, device.getVendorId(), device.getProductId());
    }

    public long rxCount() {
        return rxCount;
    }

    public long txCount() {
        return txCount;
    }

    public boolean isGsUsb() {
        return gsUsbMode;
    }

    private void initGsUsb() {
        synchronized (ioLock) {
            gsCtrlOut(GS_USB_BREQ_HOST_FORMAT, 1, le32(0x0000BEEF));
            byte[] config = gsCtrlIn(GS_USB_BREQ_DEVICE_CONFIG, 1, 12);
            if (config.length < 12) throw new IllegalStateException("short device config");
            int channels = (config[3] & 0xFF) + 1;
            if (channels < 2) throw new IllegalStateException("expected 2 CAN channels, got " + channels);
            byte[] bt = gsCtrlIn(GS_USB_BREQ_BT_CONST, 0, 40);
            if (bt.length < 40) throw new IllegalStateException("short bittiming const");
            gsFclk = le32At(bt, 4);
            initGsChannel(0, 100000);
            initGsChannel(1, 500000);
            gsCtrlOut(GS_USB_BREQ_2CAN35_UART_INIT, 0, new byte[0]);
        }
    }

    private void initGsChannel(int channel, int bitrate) {
        gsCtrlOut(GS_USB_BREQ_MODE, channel, le32(GS_CAN_MODE_RESET, 0));
        gsCtrlOut(GS_USB_BREQ_BITTIMING, channel, gsBitTiming(bitrate));
        gsCtrlOut(GS_USB_BREQ_MODE, channel, le32(GS_CAN_MODE_START, GS_CAN_FEATURE_HW_TIMESTAMP));
    }

    private byte[] gsBitTiming(int bitrate) {
        int tq = 18;
        int brp = gsFclk / (bitrate * tq);
        if (brp < 1 || gsFclk != bitrate * tq * brp) {
            brp = Math.max(1, Math.round(gsFclk / (float) (bitrate * tq)));
        }
        return le32(1, 12, 4, 2, brp);
    }

    private int gsCtrlOut(int request, int value, byte[] data) {
        if (connection == null) return -1;
        return connection.controlTransfer(0x41, request, value, 0, data, data.length, 1000);
    }

    private byte[] gsCtrlIn(int request, int value, int length) {
        if (connection == null) return new byte[0];
        byte[] out = new byte[length];
        int n = connection.controlTransfer(0xC1, request, value, 0, out, length, 1000);
        if (n <= 0) return new byte[0];
        return n == length ? out : Arrays.copyOf(out, n);
    }

    private void pollUartSideband() {
        try {
            byte[] status;
            synchronized (ioLock) {
                status = gsCtrlIn(GS_USB_BREQ_2CAN35_UART_STATUS, 0, 12);
            }
            if (status.length < 12 || status[0] != 'U' || status[1] != 'A' || status[2] != 'R' || status[3] != 'T') {
                return;
            }
            int available = ((status[7] & 0xFF) << 8) | (status[6] & 0xFF);
            while (available > 0) {
                int count = Math.min(64, available);
                byte[] data;
                synchronized (ioLock) {
                    data = gsCtrlIn(GS_USB_BREQ_2CAN35_UART_READ, 0, count);
                }
                if (data.length == 0) break;
                rxCount += data.length;
                LineListener current = listener;
                if (current != null) current.onLine("U " + spacedHex(data, data.length));
                available -= data.length;
            }
        } catch (Exception ignored) {
        }
    }

    private String formatGsUsbFrame(byte[] packet, int n) {
        if (n < 20) return null;
        int canId = le32At(packet, 4);
        int dlc = packet[8] & 0x0F;
        int channel = packet[9] & 0xFF;
        if (channel > 1 || dlc > 8) return null;
        if ((canId & CAN_ERR_FLAG) != 0) return null;
        boolean ext = (canId & CAN_EFF_FLAG) != 0;
        boolean rtr = (canId & CAN_RTR_FLAG) != 0;
        int cleanId = canId & (ext ? 0x1FFFFFFF : 0x7FF);
        StringBuilder out = new StringBuilder();
        out.append(channel).append(' ');
        out.append(ext ? 'T' : 't');
        out.append(String.format(Locale.ROOT, ext ? "%08X" : "%03X", cleanId));
        out.append(Integer.toHexString(dlc).toUpperCase(Locale.ROOT));
        if (rtr) return null;
        for (int i = 0; i < dlc && 12 + i < n; i++) {
            out.append(String.format(Locale.ROOT, "%02X", packet[12 + i] & 0xFF));
        }
        return out.toString();
    }

    private byte[] packGsHostFrame(int channel, String slcan, int echoId) {
        String frame = slcan.trim();
        boolean ext = frame.charAt(0) == 'T';
        boolean rtr = frame.charAt(0) == 'R' || frame.charAt(0) == 'r';
        int idLen = ext ? 8 : 3;
        int id = Integer.parseInt(frame.substring(1, 1 + idLen), 16);
        int dlc = Integer.parseInt(frame.substring(1 + idLen, 2 + idLen), 16);
        byte[] data = rtr ? new byte[0] : parseHex(frame.substring(2 + idLen));
        dlc = Math.min(Math.min(dlc, data.length), 8);
        int wireId = id;
        if (ext) wireId |= CAN_EFF_FLAG;
        if (rtr) wireId |= CAN_RTR_FLAG;
        ByteBuffer bb = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(echoId);
        bb.putInt(wireId);
        bb.put((byte) dlc);
        bb.put((byte) channel);
        bb.put((byte) 0);
        bb.put((byte) 0);
        for (int i = 0; i < 8; i++) bb.put(i < dlc ? data[i] : 0);
        bb.putInt(0);
        return bb.array();
    }

    private static byte[] parseHex(String value) {
        String clean = value == null ? "" : value.replaceAll("[^0-9A-Fa-f]", "");
        int len = clean.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static byte[] le32(int... values) {
        ByteBuffer bb = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int value : values) bb.putInt(value);
        return bb.array();
    }

    private static int le32At(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static String spacedHex(byte[] data, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (out.length() > 0) out.append(' ');
            out.append(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
        }
        return out.toString();
    }
}
