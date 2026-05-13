package kia.app;

import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class NavDebugState {
    static final String ACTION_STATE = "kia.app.NAV_DEBUG_STATE";

    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static String lastEvent = "";
    private static String lastFrame = "";
    private static String lastTeyes = "";

    private NavDebugState() {
    }

    static synchronized Snapshot snapshot() {
        return new Snapshot(lastEvent, lastFrame, lastTeyes);
    }

    static synchronized void event(Context context, String value) {
        lastEvent = stamp() + " " + safe(value);
        broadcast(context);
    }

    static synchronized void frame(Context context, String value) {
        lastFrame = stamp() + " " + safe(value);
        broadcast(context);
    }

    static synchronized void teyes(Context context, String value) {
        lastTeyes = stamp() + " " + safe(value);
        AppLog.line(context, "TeyesNavInfo: " + safe(value));
        broadcast(context);
    }

    private static String stamp() {
        return TIME.format(new Date());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void broadcast(Context context) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    static final class Snapshot {
        final String lastEvent;
        final String lastFrame;
        final String lastTeyes;

        Snapshot(String lastEvent, String lastFrame, String lastTeyes) {
            this.lastEvent = lastEvent;
            this.lastFrame = lastFrame;
            this.lastTeyes = lastTeyes;
        }
    }
}
