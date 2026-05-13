package kia.app;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

final class SidebandDebugState {
    static final String ACTION_STATE = "com.kia.navi.SIDEBAND_DEBUG_STATE";

    private static final int MAX_LINES = 600;
    private static final ArrayDeque<String> canM = new ArrayDeque<>();
    private static final ArrayDeque<String> canC = new ArrayDeque<>();
    private static final ArrayDeque<String> uartRx = new ArrayDeque<>();
    private static final ArrayDeque<String> uartTx = new ArrayDeque<>();
    private static boolean canRecording;
    private static boolean uartRecording;
    private static int uartAvailable;
    private static int uartDropped;
    private static int uartTxCounter;
    private static int mFrameCounter;
    private static int cFrameCounter;
    private static long lastMAt;
    private static long lastCAt;
    private static long lastMPreviewAt;
    private static long lastCPreviewAt;
    private static long lastCanBroadcastAt;
    private static String lastSaved = "";

    private SidebandDebugState() {
    }

    static synchronized Snapshot snapshot() {
        return new Snapshot(canRecording, uartRecording, uartAvailable, uartDropped,
                mFrameCounter, cFrameCounter, age(lastMAt), age(lastCAt), uartTxCounter,
                last(canM), last(canC), last(uartRx), last(uartTx), lastSaved);
    }

    static synchronized void setCanRecording(Context context, boolean value) {
        canRecording = value;
        line(canM, stamp() + " CAN " + (value ? "START" : "STOP"));
        broadcast(context);
    }

    static synchronized void setUartRecording(Context context, boolean value) {
        uartRecording = value;
        line(uartRx, stamp() + " UART " + (value ? "START" : "STOP"));
        broadcast(context);
    }

    static synchronized void can(Context context, CanSideband.Frame frame) {
        if (frame == null) return;
        boolean isM = frame.bus == 1;
        ArrayDeque<String> target = isM ? canM : canC;
        long now = System.currentTimeMillis();
        if (isM) {
            lastMAt = now;
            mFrameCounter++;
        } else {
            lastCAt = now;
            cFrameCounter++;
        }

        boolean previewDue = isM ? now - lastMPreviewAt > 300L : now - lastCPreviewAt > 300L;
        if (!canRecording && !previewDue) return;

        if (isM) lastMPreviewAt = now;
        else lastCPreviewAt = now;

        String name = isM ? "M-CAN" : "C-CAN";
        line(target, stamp() + " " + name + " " + frame.text());
        if (canRecording || now - lastCanBroadcastAt > 500L) {
            lastCanBroadcastAt = now;
            broadcast(context);
        }
    }

    static synchronized void uartStatus(Context context, int available, int dropped) {
        uartAvailable = available;
        uartDropped = dropped;
        broadcast(context);
    }

    static synchronized void uartRx(Context context, byte[] data) {
        if (data == null || data.length == 0) return;
        line(uartRx, stamp() + " UART RX " + CanbusControl.hex(data));
        if (uartRecording) broadcast(context);
    }

    static synchronized void uartTx(Context context, byte[] data) {
        if (data == null || data.length == 0) return;
        uartTxCounter++;
        line(uartTx, stamp() + " UART TX " + CanbusControl.hex(data));
        broadcast(context);
    }

    static synchronized String canText(int mode) {
        StringBuilder out = new StringBuilder();
        if (mode == 0 || mode == 2) append(out, "M-CAN bus1 raw", canM);
        if (mode == 1 || mode == 2) append(out, "C-CAN bus0 raw", canC);
        return out.toString();
    }

    static synchronized String uartText() {
        StringBuilder out = new StringBuilder();
        append(out, "UART TX", uartTx);
        append(out, "UART RX", uartRx);
        return out.toString();
    }

    static synchronized File save(Context context, String prefix, String text) throws Exception {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();
        String name = prefix + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".log";
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
        }
        lastSaved = file.getAbsolutePath();
        broadcast(context);
        return file;
    }

    private static void line(ArrayDeque<String> list, String value) {
        list.addLast(value);
        while (list.size() > MAX_LINES) list.removeFirst();
    }

    private static void append(StringBuilder out, String title, ArrayDeque<String> lines) {
        if (out.length() > 0) out.append('\n');
        out.append("== ").append(title).append(" ==\n");
        for (String line : lines) out.append(line).append('\n');
    }

    private static String last(ArrayDeque<String> lines) {
        return lines.peekLast() == null ? "" : lines.peekLast();
    }

    private static long age(long time) {
        return time == 0 ? -1L : Math.max(0L, System.currentTimeMillis() - time);
    }

    private static String stamp() {
        return new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
    }

    private static void broadcast(Context context) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    static final class Snapshot {
        final boolean canRecording;
        final boolean uartRecording;
        final int uartAvailable;
        final int uartDropped;
        final int mSlotCount;
        final int cSlotCount;
        final long mAgeMs;
        final long cAgeMs;
        final int uartTxCounter;
        final String lastMCan;
        final String lastCCan;
        final String lastUartRx;
        final String lastUartTx;
        final String lastSaved;

        Snapshot(boolean canRecording, boolean uartRecording, int uartAvailable, int uartDropped,
                 int mSlotCount, int cSlotCount, long mAgeMs, long cAgeMs, int uartTxCounter,
                 String lastMCan, String lastCCan, String lastUartRx, String lastUartTx, String lastSaved) {
            this.canRecording = canRecording;
            this.uartRecording = uartRecording;
            this.uartAvailable = uartAvailable;
            this.uartDropped = uartDropped;
            this.mSlotCount = mSlotCount;
            this.cSlotCount = cSlotCount;
            this.mAgeMs = mAgeMs;
            this.cAgeMs = cAgeMs;
            this.uartTxCounter = uartTxCounter;
            this.lastMCan = lastMCan;
            this.lastCCan = lastCCan;
            this.lastUartRx = lastUartRx;
            this.lastUartTx = lastUartTx;
            this.lastSaved = lastSaved;
        }
    }
}
