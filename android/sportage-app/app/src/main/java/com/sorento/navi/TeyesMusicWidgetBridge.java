package com.sorento.navi;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.util.Locale;

final class TeyesMusicWidgetBridge {
    private static final String PACKAGE = "com.teyes.music.widget";
    private static final String SERVICE = "com.teyes.music.service.MediaService";
    private static final String START_ACTION = "com.teyes.widget.action.START_SERVICE";
    private static final Uri PROGRESS_URI = Uri.parse("content://com.teyes.music.provider/progress");
    private static final long POLL_MS = 1000L;
    private static final long HINT_TTL_MS = 2500L;
    private static final int HINT_PRIORITY = 180;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static Context appContext;
    private static long lastPokeAt;
    private static long lastLogAt;
    private static String lastSongType = "";

    private static final Runnable POLL = new Runnable() {
        @Override
        public void run() {
            scanNow(appContext);
            if (appContext != null) HANDLER.postDelayed(this, POLL_MS);
        }
    };

    private TeyesMusicWidgetBridge() {
    }

    static synchronized void start(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        pokeWidget(appContext);
        HANDLER.removeCallbacks(POLL);
        HANDLER.post(POLL);
    }

    static synchronized void stop() {
        HANDLER.removeCallbacks(POLL);
        appContext = null;
        lastSongType = "";
    }

    static synchronized boolean scanNow(Context context) {
        if (context == null) return false;
        appContext = context.getApplicationContext();
        pokeWidget(appContext);
        WidgetState state = queryWidgetState(appContext);
        if (state == null || TextUtils.isEmpty(state.source)) return false;
        MediaMonitor.reportSourceHint(appContext, state.source, PACKAGE, HINT_PRIORITY, HINT_TTL_MS);
        if (!TextUtils.equals(lastSongType, state.songType)) {
            lastSongType = state.songType;
            AppLog.line(appContext, "TEYES widget: источник=" + state.source + " songType=" + state.songType);
        }
        return true;
    }

    private static void pokeWidget(Context context) {
        long now = System.currentTimeMillis();
        if (now - lastPokeAt < 30_000L) return;
        lastPokeAt = now;
        try {
            Intent broadcast = new Intent(START_ACTION);
            broadcast.setPackage(PACKAGE);
            context.sendBroadcast(broadcast);
        } catch (Exception ignored) {
        }
        try {
            Intent service = new Intent();
            service.setComponent(new ComponentName(PACKAGE, SERVICE));
            context.startService(service);
        } catch (Exception ignored) {
        }
    }

    private static WidgetState queryWidgetState(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(PROGRESS_URI, null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) return null;
            WidgetState best = null;
            do {
                String songType = value(cursor, "songType");
                String source = sourceFromSongType(songType);
                if (!TextUtils.isEmpty(source)) {
                    best = new WidgetState();
                    best.songType = clean(songType);
                    best.source = source;
                    best.progress = intValue(cursor, "progress", -1);
                }
            } while (cursor.moveToNext());
            return best;
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now - lastLogAt > 30_000L) {
                lastLogAt = now;
                AppLog.line(context, "TEYES widget: provider недоступен " + e.getClass().getSimpleName());
            }
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String value(Cursor cursor, String column) {
        int idx = cursor.getColumnIndex(column);
        if (idx < 0) return null;
        try {
            return cursor.getString(idx);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int intValue(Cursor cursor, String column, int fallback) {
        int idx = cursor.getColumnIndex(column);
        if (idx < 0) return fallback;
        try {
            return cursor.getInt(idx);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String sourceFromSongType(String songType) {
        String type = clean(songType);
        if (TextUtils.isEmpty(type)) return null;
        String t = type.toLowerCase(Locale.US);
        if (t.contains("bluetooth")) return "Bluetooth";
        if (t.contains("local_radio")) return "Радио";
        if (t.contains("network_radio")) return "Интернет-радио";
        if (t.contains("local_music")) return "USB";
        if (t.contains("cloud_music") || t.contains("network_music")) return "TEYES Music";
        if (t.contains("carplay")) return "CarPlay";
        if (t.contains("android_auto") || t.contains("androidauto")) return "Android Auto";
        if (t.contains("radio")) return "Радио";
        if (t.contains("music")) return "TEYES Media";
        return null;
    }

    private static String clean(String value) {
        if (value == null) return null;
        String out = value.replace('\n', ' ').replace('\r', ' ').trim();
        while (out.contains("  ")) out = out.replace("  ", " ");
        return out.length() == 0 ? null : out;
    }

    private static final class WidgetState {
        String songType;
        String source;
        int progress;
    }
}
