package kia.app;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

final class QaState {
    static final String ACTION_STATE = "kia.app.QA_STATE";

    private static final int MAX_LINES = 120;
    private static final ArrayDeque<String> events = new ArrayDeque<>();
    private static final ArrayDeque<String> frames = new ArrayDeque<>();
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private QaState() {
    }

    static synchronized void event(Context context, String value) {
        String row = stamp() + " " + value;
        Log.i("KiaQa", row);
        line(events, row);
        broadcast(context);
    }

    static synchronized void frame(Context context, byte[] frame) {
        if (frame == null || frame.length < 5) return;
        int id = frame[4] & 0xff;
        if (id == 0x70 || id == 0x76) return;
        if (id != 0x20 && id != 0x21 && id != 0x22
                && id != 0x44 && id != 0x45 && id != 0x47 && id != 0x48 && id != 0x4A
                && id != 0x7A && id != 0x78 && id != 0x79) return;
        String row = stamp() + " " + label(id) + " " + CanbusControl.hex(frame);
        Log.i("KiaQa", row);
        line(frames, row);
        broadcast(context);
    }

    static synchronized String text() {
        StringBuilder out = new StringBuilder();
        append(out, "events", events);
        append(out, "frames", frames);
        return out.toString();
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

    private static String label(int id) {
        switch (id) {
            case 0x20:
                return "radio-text";
            case 0x21:
                return "media-text";
            case 0x22:
                return "title";
            case 0x44:
                return "speed-limit";
            case 0x45:
                return "nav/compass";
            case 0x47:
                return "eta";
            case 0x48:
                return "nav";
            case 0x4A:
                return "street";
            case 0x78:
                return "raw-tx";
            case 0x79:
                return "v21";
            case 0x7A:
                return "source";
            default:
                return "0x" + Integer.toHexString(id).toUpperCase(Locale.US);
        }
    }

    private static String stamp() {
        return TIME.format(new Date());
    }

    private static void broadcast(Context context) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(context.getPackageName());
        intent.putExtra("qa", text());
        context.sendBroadcast(intent);
    }
}
