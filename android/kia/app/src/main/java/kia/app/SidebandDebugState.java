package kia.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

final class SidebandDebugState {
    static final String ACTION_STATE = "com.kia.navi.SIDEBAND_DEBUG_STATE";

    private static final int MAX_PREVIEW_LINES = 600;
    private static final int MAX_CAN_CAPTURE_FRAMES = 50_000;
    private static final ArrayDeque<String> canM = new ArrayDeque<>();
    private static final ArrayDeque<String> canC = new ArrayDeque<>();
    private static final ArrayDeque<String> canCapture = new ArrayDeque<>();
    private static final ArrayDeque<String> uartRx = new ArrayDeque<>();
    private static final ArrayDeque<String> uartTx = new ArrayDeque<>();
    private static boolean canRecording;
    private static boolean uartRecording;
    private static int canCaptureFrameCount;
    private static int canCaptureMode = 2;
    private static String canCaptureStatus = "";
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
                last(canM), last(canC), last(uartRx), last(uartTx), lastSaved,
                canCaptureFrameCount, MAX_CAN_CAPTURE_FRAMES, canCaptureMode, canCaptureStatus);
    }

    static synchronized boolean canRecording() {
        return canRecording;
    }

    static synchronized void setCanRecording(Context context, boolean value) {
        if (value) {
            canCapture.clear();
            canCaptureFrameCount = 0;
            canCaptureMode = context == null ? 2 : AppPrefs.canLogMode(context);
            canCaptureStatus = "запись " + modeLabel(canCaptureMode) + " до " + MAX_CAN_CAPTURE_FRAMES + " кадров";
            String start = stamp() + " CAN START mode=" + modeLabel(canCaptureMode)
                    + " limit=" + MAX_CAN_CAPTURE_FRAMES;
            canCapture.addLast(start);
            line(canM, start);
            line(canC, start);
        } else if (canRecording) {
            String stop = stamp() + " CAN STOP frames=" + canCaptureFrameCount;
            canCapture.addLast(stop);
            line(canM, stop);
            line(canC, stop);
            canCaptureStatus = "остановлено, кадров: " + canCaptureFrameCount;
        }
        canRecording = value;
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
        String name = isM ? "M-CAN" : "C-CAN";
        String value = stamp() + " " + name + " " + frame.text();
        boolean captured = canRecording && matchesCanCaptureMode(frame);
        if (captured) {
            canCapture.addLast(value);
            canCaptureFrameCount++;
            canCaptureStatus = "запись " + canCaptureFrameCount + "/" + MAX_CAN_CAPTURE_FRAMES
                    + " " + modeLabel(canCaptureMode);
        }

        if (!captured && !previewDue) return;

        if (previewDue) {
            if (isM) lastMPreviewAt = now;
            else lastCPreviewAt = now;
        }

        line(target, value);
        if (captured && canCaptureFrameCount >= MAX_CAN_CAPTURE_FRAMES) {
            autoStopCanCapture(context);
            return;
        }
        if (now - lastCanBroadcastAt > 500L) {
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

    static synchronized String canExportText(int mode) {
        if (!canCapture.isEmpty()) return canCaptureText();
        return canText(mode);
    }

    static synchronized String uartText() {
        StringBuilder out = new StringBuilder();
        append(out, "UART TX", uartTx);
        append(out, "UART RX", uartRx);
        return out.toString();
    }

    static synchronized File save(Context context, String prefix, String text) throws Exception {
        return saveInternal(context, prefix, text, false);
    }

    static synchronized File saveCompressed(Context context, String prefix, String text) throws Exception {
        return saveInternal(context, prefix, text, true);
    }

    private static File saveInternal(Context context, String prefix, String text, boolean compressed) throws Exception {
        String ext = compressed ? ".log.gz" : ".log";
        String name = prefix + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ext;
        byte[] data = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
        File file = writeToDownloads(context, name, data, compressed);
        lastSaved = file.getAbsolutePath();
        broadcast(context);
        return file;
    }

    private static File writeToDownloads(Context context, String name, byte[] data, boolean compressed) throws Exception {
        File expected = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name);
        if (context == null) {
            writeToFile(expected, data, compressed);
            return expected;
        }

        Uri uri = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, compressed ? "application/gzip" : "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Downloads недоступны");
            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null) throw new IOException("Downloads output недоступен");
                writePayload(out, data, compressed);
            }
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
            return expected;
        } catch (Exception mediaError) {
            if (context != null && uri != null) {
                try {
                    context.getContentResolver().delete(uri, null, null);
                } catch (Exception ignored) {
                }
            }
            File fallback = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (fallback == null) fallback = context.getFilesDir();
            File file = new File(fallback, name);
            writeToFile(file, data, compressed);
            return file;
        }
    }

    private static void writeToFile(File file, byte[] data, boolean compressed) throws Exception {
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            writePayload(out, data, compressed);
        }
    }

    private static void writePayload(OutputStream out, byte[] data, boolean compressed) throws Exception {
        if (compressed) {
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(data);
            }
        } else {
            out.write(data);
        }
    }

    private static void autoStopCanCapture(Context context) {
        canRecording = false;
        String stop = stamp() + " CAN AUTO STOP limit=" + MAX_CAN_CAPTURE_FRAMES
                + " frames=" + canCaptureFrameCount;
        canCapture.addLast(stop);
        line(canM, stop);
        line(canC, stop);
        try {
            File file = saveInternal(context, "can_auto_50000", canCaptureText(), true);
            canCaptureStatus = "лимит 50000, сохранено: " + file.getName();
        } catch (Exception e) {
            lastSaved = "CAN auto-save failed: " + e.getMessage();
            canCaptureStatus = "лимит 50000, ошибка сохранения";
        }
        if (context != null) {
            AppPrefs.setDebugCan(context, false);
            CanbusControl.stopCanStream(context);
        }
        broadcast(context);
    }

    private static void line(ArrayDeque<String> list, String value) {
        list.addLast(value);
        while (list.size() > MAX_PREVIEW_LINES) list.removeFirst();
    }

    private static void append(StringBuilder out, String title, ArrayDeque<String> lines) {
        if (out.length() > 0) out.append('\n');
        out.append("== ").append(title).append(" ==\n");
        for (String line : lines) out.append(line).append('\n');
    }

    private static String last(ArrayDeque<String> lines) {
        return lines.peekLast() == null ? "" : lines.peekLast();
    }

    private static String canCaptureText() {
        StringBuilder out = new StringBuilder();
        out.append("== CAN recording ==\n");
        out.append("mode=").append(modeLabel(canCaptureMode))
                .append(" frames=").append(canCaptureFrameCount)
                .append(" limit=").append(MAX_CAN_CAPTURE_FRAMES)
                .append('\n');
        for (String line : canCapture) out.append(line).append('\n');
        return out.toString();
    }

    private static boolean matchesCanCaptureMode(CanSideband.Frame frame) {
        if (canCaptureMode == 0) return frame.bus == 1;
        if (canCaptureMode == 1) return frame.bus == 0;
        return true;
    }

    private static String modeLabel(int mode) {
        if (mode == 0) return "M-CAN";
        if (mode == 1) return "C-CAN";
        return "оба CAN";
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
        final int canCaptureCount;
        final int canCaptureLimit;
        final int canCaptureMode;
        final String canCaptureStatus;

        Snapshot(boolean canRecording, boolean uartRecording, int uartAvailable, int uartDropped,
                 int mSlotCount, int cSlotCount, long mAgeMs, long cAgeMs, int uartTxCounter,
                 String lastMCan, String lastCCan, String lastUartRx, String lastUartTx, String lastSaved,
                 int canCaptureCount, int canCaptureLimit, int canCaptureMode, String canCaptureStatus) {
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
            this.canCaptureCount = canCaptureCount;
            this.canCaptureLimit = canCaptureLimit;
            this.canCaptureMode = canCaptureMode;
            this.canCaptureStatus = canCaptureStatus;
        }
    }
}
