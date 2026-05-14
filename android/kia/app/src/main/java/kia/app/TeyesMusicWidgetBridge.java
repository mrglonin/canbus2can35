package kia.app;

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
    private static String currentSource = "";
    private static String currentSongType = "";
    private static String currentTitle = "";
    private static String currentArtist = "";
    private static String currentFrequency = "";
    private static String currentAppName = "";
    private static int currentProgress = -1;
    private static long currentAt;

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
        currentSource = state.source;
        currentSongType = clean(state.songType);
        currentTitle = clean(state.title);
        currentArtist = clean(state.artist);
        currentFrequency = clean(state.frequency);
        currentAppName = clean(state.appName);
        currentProgress = state.progress;
        currentAt = System.currentTimeMillis();
        MediaMonitor.reportSourceHint(appContext, state.source, PACKAGE, HINT_PRIORITY, HINT_TTL_MS);
        if (state.hasDisplayText()) {
            MediaMonitor.reportExternal(appContext, state.source, PACKAGE,
                    state.displayArtist(), state.displayTitle(), -1, state.priority());
        }
        if (!TextUtils.equals(lastSongType, state.songType)) {
            lastSongType = state.songType;
            AppLog.line(appContext, "TEYES widget: источник=" + state.source
                    + " songType=" + state.songType
                    + " title=" + state.displayTitle()
                    + " artist=" + state.displayArtist());
        }
        return true;
    }

    static synchronized String currentSource() {
        return currentSource;
    }

    static synchronized String currentSongType() {
        return currentSongType;
    }

    static synchronized String currentTitle() {
        return currentTitle;
    }

    static synchronized String currentArtist() {
        return currentArtist;
    }

    static synchronized String currentFrequency() {
        return currentFrequency;
    }

    static synchronized String currentAppName() {
        return currentAppName;
    }

    static synchronized int currentProgress() {
        return currentProgress;
    }

    static synchronized long currentAgeMs() {
        return currentAt <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() - currentAt;
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
                String appName = firstValue(cursor, "app_name", "appName", "player", "sourceName");
                String source = sourceFromSongType(songType, appName);
                if (!TextUtils.isEmpty(source)) {
                    best = new WidgetState();
                    best.songType = clean(songType);
                    best.source = source;
                    best.progress = intValue(cursor, "progress", -1);
                    best.title = firstValue(cursor,
                            "song_title", "songTitle", "title", "name", "track", "track_title", "music_title");
                    best.artist = firstValue(cursor,
                            "song_artist", "songArtist", "artist", "singer", "author", "station", "station_name");
                    best.frequency = firstValue(cursor,
                            "freq", "frequency", "radio_freq", "fm", "radio_frequency");
                    best.appName = clean(appName);
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

    private static String firstValue(Cursor cursor, String... columns) {
        if (columns == null) return null;
        for (String column : columns) {
            String value = clean(value(cursor, column));
            if (!TextUtils.isEmpty(value)) return value;
        }
        return null;
    }

    private static String sourceFromSongType(String songType, String appName) {
        String type = clean(songType);
        if (TextUtils.isEmpty(type)) return null;
        String t = type.toLowerCase(Locale.US);
        if (t.contains("bluetooth")) return "Bluetooth";
        if (t.contains("local_radio")) return "Радио";
        if (t.contains("network_radio")) return "Интернет-радио";
        if (t.contains("local_music")) return "USB";
        if (t.contains("cloud_music") || t.contains("network_music")) {
            String app = clean(appName);
            String p = app == null ? "" : app.toLowerCase(Locale.US);
            if (p.contains("янд") || p.contains("yandex") || p.contains("я.")) return "Яндекс Музыка";
            return "TEYES Music";
        }
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
        String title;
        String artist;
        String frequency;
        String appName;
        int progress;

        boolean hasDisplayText() {
            return !TextUtils.isEmpty(displayTitle()) || !TextUtils.isEmpty(displayArtist());
        }

        String displayTitle() {
            String cleanTitle = clean(title);
            String cleanFreq = clean(frequency);
            if (!TextUtils.isEmpty(cleanFreq) && !TextUtils.isEmpty(cleanTitle)
                    && !cleanTitle.toLowerCase(Locale.US).contains(cleanFreq.toLowerCase(Locale.US))) {
                return cleanTitle + cleanFreq;
            }
            return first(cleanTitle, clean(appName), source);
        }

        String displayArtist() {
            return first(clean(artist), clean(appName));
        }

        int priority() {
            String s = clean(source);
            String p = s == null ? "" : s.toLowerCase(Locale.US);
            if (p.contains("радио") || p.contains("radio")) return 135;
            if (p.contains("bluetooth") || p.contains("bt")) return 128;
            if (p.contains("янд") || p.contains("teyes music") || p.contains("internet")) return 126;
            if (p.contains("usb")) return 115;
            return 105;
        }

        private static String first(String... values) {
            if (values == null) return null;
            for (String value : values) {
                String clean = clean(value);
                if (!TextUtils.isEmpty(clean)) return clean;
            }
            return null;
        }
    }
}
