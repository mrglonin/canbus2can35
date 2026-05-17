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
    private static long lastEmptyLogAt;
    private static String lastSongType = "";
    private static String lastWidgetLogKey = "";
    private static String currentSource = "";
    private static String currentSongType = "";
    private static String currentTitle = "";
    private static String currentDisplayTitle = "";
    private static String currentArtist = "";
    private static String currentFrequency = "";
    private static String currentSearchState = "";
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
        if (state == null || TextUtils.isEmpty(state.source)) {
            logEmptyState(appContext);
            return false;
        }
        currentSource = state.source;
        currentSongType = clean(state.songType);
        currentTitle = clean(state.title);
        currentDisplayTitle = clean(state.displayTitle());
        currentArtist = clean(state.artist);
        currentFrequency = clean(state.frequency);
        currentSearchState = clean(state.searchState);
        currentAppName = clean(state.appName);
        currentProgress = state.progress;
        currentAt = System.currentTimeMillis();
        MediaMonitor.reportSourceHint(appContext, state.source, PACKAGE, HINT_PRIORITY, HINT_TTL_MS);
        if (state.hasDisplayText()) {
            MediaMonitor.reportExternal(appContext, state.source, PACKAGE,
                    state.displayArtist(), state.displayTitle(), -1, state.priority());
        }
        String logKey = state.source + "|" + state.songType + "|" + state.displayTitle() + "|" + state.searchState;
        if (!TextUtils.equals(lastSongType, state.songType) || !TextUtils.equals(lastWidgetLogKey, logKey)) {
            lastSongType = state.songType;
            lastWidgetLogKey = logKey;
            AppLog.line(appContext, "TEYES widget: источник=" + state.source
                    + " songType=" + state.songType
                    + " title=" + state.displayTitle()
                    + " artist=" + state.displayArtist()
                    + " freq=" + state.frequency
                    + " search=" + state.searchState);
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

    static synchronized String currentDisplayTitle() {
        return currentDisplayTitle;
    }

    static synchronized String currentArtist() {
        return currentArtist;
    }

    static synchronized String currentFrequency() {
        return currentFrequency;
    }

    static synchronized String currentSearchState() {
        return currentSearchState;
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
                            "freq", "frequency", "radio_freq", "fm", "radio_frequency",
                            "freq_text", "frequency_text", "radio_text", "radioText",
                            "station_freq", "stationFrequency", "current_freq", "currentFrequency");
                    best.searchState = firstValue(cursor,
                            "search", "scan", "seek", "isSearching", "isSeeking", "isScanning",
                            "searching", "seeking", "scanning", "search_state", "seek_state", "scan_state",
                            "radio_search", "radio_seek", "radio_scan", "auto_search", "auto_scan");
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
        String appSource = sourceFromAppName(appName);
        if (TextUtils.isEmpty(type)) return appSource;
        String t = type.toLowerCase(Locale.US);
        if (isBluetoothText(t)) return "Bluetooth";
        if (t.contains("local_radio")) return "FM радио";
        if (t.contains("dab") || t.contains("dts")) return "DTS радио";
        if (t.contains("network_radio")) return "Интернет-радио";
        if (t.contains("local_music")) return "USB";
        if (t.contains("cloud_music") || t.contains("network_music") || t.contains("net_music")) {
            String app = clean(appName);
            String p = app == null ? "" : app.toLowerCase(Locale.US);
            if (p.contains("янд") || p.contains("yandex") || p.contains("я.")) return "Яндекс Музыка";
            return "TEYES Music";
        }
        if (t.contains("carplay")) return "CarPlay";
        if (t.contains("android_auto") || t.contains("androidauto")) return "Android Auto";
        if (t.contains("radio")) return t.contains("am") ? "AM 24" : "FM радио";
        if (t.contains("music")) return "TEYES Media";
        return appSource;
    }

    private static String sourceFromAppName(String appName) {
        String app = clean(appName);
        if (TextUtils.isEmpty(app)) return null;
        String p = app.toLowerCase(Locale.US);
        if (isBluetoothText(p)) return "Bluetooth";
        if (p.contains("янд") || p.contains("yandex")) return "Яндекс Музыка";
        if (p.contains("carplay")) return "CarPlay";
        if (p.contains("android auto") || p.contains("androidauto")) return "Android Auto";
        if (p.contains("dab") || p.contains("dts") || p.contains("digital radio")) return "DTS радио";
        if (p.contains("am ") || p.equals("am") || p.contains("am radio")) return "AM 24";
        if (p.contains("radio") || p.contains("радио") || p.contains("fm")) return "FM радио";
        return null;
    }

    private static boolean isBluetoothText(String value) {
        String text = clean(value);
        if (TextUtils.isEmpty(text)) return false;
        String p = text.toLowerCase(Locale.US);
        return p.contains("bluetooth")
                || p.equals("bt")
                || p.startsWith("bt ")
                || p.contains("btmusic")
                || p.contains("bt_music")
                || p.contains("bt-music")
                || p.contains("bt audio")
                || p.contains("bt_audio")
                || p.contains("bt-audio")
                || p.contains("bt музыка")
                || p.contains("a2dp")
                || p.contains("avrcp");
    }

    private static void logEmptyState(Context context) {
        long now = System.currentTimeMillis();
        if (now - lastEmptyLogAt < 30_000L) return;
        lastEmptyLogAt = now;
        AppLog.line(context, "TEYES widget: источник не найден");
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
        String searchState;
        String artist;
        String frequency;
        String appName;
        int progress;

        boolean hasDisplayText() {
            return hasMeaningfulText(title, source, appName)
                    || hasMeaningfulText(artist, source, appName)
                    || !TextUtils.isEmpty(clean(frequency))
                    || isRadioSource() && isSearching();
        }

        String displayTitle() {
            String cleanTitle = clean(title);
            String cleanFreq = clean(frequency);
            if (isRadioSource() && isSearching()) {
                return TextUtils.isEmpty(cleanFreq) ? "Поиск станции" : "Поиск " + cleanFreq;
            }
            if (!TextUtils.isEmpty(cleanFreq) && !TextUtils.isEmpty(cleanTitle)
                    && !cleanTitle.toLowerCase(Locale.US).contains(cleanFreq.toLowerCase(Locale.US))) {
                return cleanTitle + " " + cleanFreq;
            }
            return first(cleanTitle, cleanFreq, clean(appName), source);
        }

        String displayArtist() {
            return first(clean(artist), clean(appName));
        }

        int priority() {
            String s = clean(source);
            String p = s == null ? "" : s.toLowerCase(Locale.US);
            if (p.contains("радио") || p.contains("radio") || p.contains("dab") || p.contains("dts")) return 135;
            if (p.contains("bluetooth") || p.contains("bt")) return 128;
            if (p.contains("янд") || p.contains("teyes music") || p.contains("internet")) return 126;
            if (p.contains("usb")) return 115;
            return 105;
        }

        private boolean isRadioSource() {
            String s = clean(source);
            String p = s == null ? "" : s.toLowerCase(Locale.US);
            return p.contains("радио") || p.contains("radio") || p.contains("dab") || p.contains("dts")
                    || p.startsWith("fm") || p.startsWith("am");
        }

        private boolean isSearching() {
            String state = clean(searchState);
            if (TextUtils.isEmpty(state)) return false;
            String p = state.toLowerCase(Locale.US);
            return p.equals("1") || p.equals("true") || p.equals("yes")
                    || p.contains("search") || p.contains("seek") || p.contains("scan")
                    || p.contains("поиск") || p.contains("скан");
        }

        private static boolean hasMeaningfulText(String value, String source, String appName) {
            String text = clean(value);
            if (TextUtils.isEmpty(text)) return false;
            String src = clean(source);
            String app = clean(appName);
            return !text.equalsIgnoreCase(src == null ? "" : src)
                    && !text.equalsIgnoreCase(app == null ? "" : app);
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
