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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class UsbCdcManager {
    private static final String ACTION_USB_PERMISSION = "com.canbox2can35.lab.USB_PERMISSION";

    private final Context context;
    private final UsbManager manager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbInterface dataInterface;
    private UsbEndpoint epIn;
    private UsbEndpoint epOut;
    private Thread rxThread;
    private volatile boolean running;
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
        if (candidate.getVendorId() == 0x1d50 && candidate.getProductId() == 0x606f) return true;
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
        setLineCoding115200();
        startRx();
        lastError = "";
        return true;
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

    private void startRx() {
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

    public synchronized boolean writeLine(String line) {
        if (connection == null || epOut == null) {
            if (!openFirst()) return false;
        }
        byte[] bytes = (line + "\r").getBytes(StandardCharsets.US_ASCII);
        int n = connection.bulkTransfer(epOut, bytes, bytes.length, 500);
        if (n > 0) txCount += n;
        return n == bytes.length;
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
    }

    public boolean isOpen() {
        return connection != null;
    }

    public String label() {
        if (device == null) return lastError.isEmpty() ? "no USB" : lastError;
        return String.format("USB %04x:%04x", device.getVendorId(), device.getProductId());
    }

    public long rxCount() {
        return rxCount;
    }

    public long txCount() {
        return txCount;
    }
}
