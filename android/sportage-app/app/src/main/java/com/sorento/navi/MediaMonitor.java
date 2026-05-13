package com.sorento.navi;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MediaMonitor {
    private static final long SOURCE_LOCK_MS = 8000L;
    private static final int SOURCE_SWITCH_MARGIN = 25;
    private static Handler handler;
    private static Runnable poll;
    private static String lastLine = "";
    private static long lastAt;
    private static long lastCandidateAt;
    private static String activeSource = "";
    private static String activePkg = "";
    private static String activeSourceKey = "";
    private static int activePriority;
    private static long activeSourceAt;
    private static String hintSource = "";
    private static String hintPkg = "";
    private static int hintPriority;
    private static long hintUntil;

    private static final class NotificationCandidate {
        String pkg;
        String title;
        String artist;
        String sub;
        String big;
        String label;
        String category;
        long postTime;
        int score;
        int playbackRank;
        MediaSession.Token token;
    }

    private MediaMonitor() {
    }

    static void start(Context context) {
        Context app = context.getApplicationContext();
        TeyesMusicWidgetBridge.start(app);
        TeyesMediaBridge.start(app);
        if (handler == null) handler = new Handler(Looper.getMainLooper());
        if (poll == null) {
            poll = () -> {
                scanNow(app);
                if (handler != null && poll != null) handler.postDelayed(poll, 1000);
            };
        }
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    static void stop() {
        if (handler != null && poll != null) handler.removeCallbacks(poll);
        poll = null;
        TeyesMusicWidgetBridge.stop();
    }

    static void scanNow(Context context) {
        TeyesMusicWidgetBridge.scanNow(context);
        TeyesMediaBridge.scanNow(context);
        MediaNotificationListener.refresh(context);
        scanSessions(context);
        scanCandidates(context);
    }

    static boolean reportExternal(Context context, String source, String pkg, String artist, String title, long durationMs) {
        return reportExternal(context, source, pkg, artist, title, durationMs, 95);
    }

    static boolean reportExternal(Context context, String source, String pkg, String artist, String title, long durationMs, int priority) {
        if (context == null) return false;
        if (TextUtils.isEmpty(source) && TextUtils.isEmpty(title) && TextUtils.isEmpty(artist)) return false;
        return emit(context, source, pkg, artist, title, durationMs, priority);
    }

    static void reportSourceHint(Context context, String source, String pkg, int priority, long ttlMs) {
        if (context == null || TextUtils.isEmpty(source)) return;
        long now = System.currentTimeMillis();
        String sourceKey = sourceKey(source, pkg);
        boolean changed = !sameSource(source, pkg, activeSource, activePkg);
        hintSource = source;
        hintPkg = pkg;
        hintPriority = priority;
        hintUntil = now + Math.max(1000L, ttlMs);
        activeSource = source;
        activePkg = pkg;
        activeSourceKey = sourceKey;
        activePriority = changed ? priority : Math.max(activePriority, priority);
        activeSourceAt = now;
        if (changed || TextUtils.isEmpty(lastLine)) {
            String line = "Мультимедиа: " + dash(source) + " / - / - / --:--";
            if (!TextUtils.equals(line, lastLine) && now - lastAt > 750L) {
                lastLine = line;
                lastAt = now;
                AppLog.setMedia(context, line);
                CanbusControl.sendMediaMetadata(context, source, "", source);
                if (AppPrefs.debug(context)) {
                    AppLog.line(context, "Мультимедиа: TEYES widget hint=" + source + " pkg=" + pkg);
                }
            }
        }
    }

    static void resetDebugScan() {
        lastCandidateAt = 0;
    }

    static boolean reportNotification(Context context, StatusBarNotification sbn) {
        NotificationCandidate candidate = notificationCandidate(context, sbn);
        if (candidate == null) return false;
        if (candidate.token != null) {
            try {
                if (reportController(context, new MediaController(context, candidate.token), candidate)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        String artist = firstText(candidate.artist, candidate.sub, candidate.big);
        int priority = candidate.token != null
                ? priorityForPlayback(candidate.playbackRank)
                : (allowedMediaPackage(candidate.pkg) ? 90 : (looksTrackLike(candidate) ? 82 : 65));
        if (emit(context, sourceLabel(context, candidate.pkg) + " [увед]", candidate.pkg, artist, candidate.title, -1, priority)) {
            AppLog.line(context, "Уведомление: пакет=" + candidate.pkg
                    + " score=" + candidate.score
                    + " playback=" + candidate.playbackRank
                    + " category=" + candidate.category
                    + " трек=" + candidate.title
                    + " текст=" + artist);
        }
        return true;
    }

    static boolean isMediaNotification(Context context, StatusBarNotification sbn) {
        return notificationCandidate(context, sbn) != null;
    }

    static int notificationScore(Context context, StatusBarNotification sbn) {
        NotificationCandidate candidate = notificationCandidate(context, sbn);
        return candidate == null ? 0 : candidate.score;
    }

    private static NotificationCandidate notificationCandidate(Context context, StatusBarNotification sbn) {
        if (context == null || sbn == null) return null;
        String pkg = sbn.getPackageName();
        if (ignorePackage(context, pkg)) return null;
        Notification n = sbn.getNotification();
        if (n == null || n.extras == null) return null;
        Bundle e = n.extras;
        NotificationCandidate candidate = new NotificationCandidate();
        candidate.pkg = pkg;
        candidate.label = label(context, pkg);
        candidate.category = n.category;
        candidate.postTime = sbn.getPostTime();
        Object token = e.getParcelable("android.mediaSession");
        if (token instanceof MediaSession.Token) {
            candidate.token = (MediaSession.Token) token;
            candidate.playbackRank = playbackRank(context, candidate.token);
        }
        candidate.title = firstText(
                text(e, "android.title"),
                text(e, "android.title.big"),
                text(e, "android.conversationTitle")
        );
        candidate.artist = firstText(text(e, "android.text"), firstLine(e, "android.textLines"));
        candidate.sub = firstText(text(e, "android.subText"), text(e, "android.infoText"), text(e, "android.summaryText"));
        candidate.big = text(e, "android.bigText");
        if (TextUtils.isEmpty(candidate.title) && n.tickerText != null) {
            candidate.title = n.tickerText.toString();
        }
        normalizeCandidate(candidate);
        if (isNoise(candidate.label, candidate.title, candidate.artist, candidate.sub, candidate.big)) return null;
        boolean mediaCategory = Notification.CATEGORY_TRANSPORT.equals(n.category)
                || Notification.CATEGORY_SERVICE.equals(n.category) && candidate.token != null;
        boolean mediaPkg = allowedMediaPackage(pkg) || looksMedia(candidate.label);
        boolean scanAllMatch = looksTrackLike(candidate);
        if (candidate.token != null) {
            candidate.score = 650
                    + (candidate.playbackRank * 450)
                    + (mediaCategory ? 200 : 0)
                    + (mediaPkg ? 140 : 0)
                    + freshness(candidate.postTime);
            return candidate;
        }
        if (TextUtils.isEmpty(candidate.title)) return null;
        if (!mediaCategory && !mediaPkg && !scanAllMatch) return null;
        candidate.score = (mediaCategory ? 850 : 0)
                + (mediaPkg ? 600 : 0)
                + (scanAllMatch ? 420 : 0)
                + (!TextUtils.isEmpty(candidate.artist) ? 80 : 0)
                + (!TextUtils.isEmpty(candidate.sub) ? 20 : 0)
                + freshness(candidate.postTime);
        return candidate;
    }

    private static boolean scanSessions(Context context) {
        try {
            MediaSessionManager manager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (manager == null) return false;
            List<MediaController> sessions = manager.getActiveSessions(new ComponentName(context, MediaNotificationListener.class));
            if (sessions == null || sessions.isEmpty()) return false;
            MediaController best = null;
            int bestRank = 0;
            for (MediaController c : sessions) {
                if (c == null || ignorePackage(context, c.getPackageName())) continue;
                int rank = rank(c.getPlaybackState()) * 100 + (looksMedia(c.getPackageName()) ? 20 : 0);
                if (rank > bestRank) {
                    bestRank = rank;
                    best = c;
                }
            }
            if (best != null) {
                reportController(context, best, null);
                return true;
            }
        } catch (SecurityException e) {
            AppLog.line(context, "Мультимедиа: нет доступа к уведомлениям");
        } catch (Exception e) {
            AppLog.line(context, "Мультимедиа: ошибка сессии " + e.getClass().getSimpleName());
        }
        return false;
    }

    private static boolean reportController(Context context, MediaController c, NotificationCandidate fallback) {
        String pkg = c.getPackageName();
        String title = null;
        String artist = null;
        long duration = -1;
        MediaMetadata md = c.getMetadata();
        if (md != null) {
            title = md.getString(MediaMetadata.METADATA_KEY_TITLE);
            artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST);
            duration = md.getLong(MediaMetadata.METADATA_KEY_DURATION);
        }
        if (TextUtils.isEmpty(title) && fallback != null) title = fallback.title;
        if (TextUtils.isEmpty(artist) && fallback != null) artist = firstText(fallback.artist, fallback.sub, fallback.big);
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(artist)) return false;
        int priority = priorityForPlayback(rank(c.getPlaybackState()));
        if (emit(context, sourceLabel(context, pkg), pkg, artist, title, duration, priority)) {
            AppLog.line(context, "Сессия: пакет=" + pkg + " состояние=" + state(c.getPlaybackState()) + " трек=" + title + " автор=" + artist);
        }
        return true;
    }

    private static boolean emit(Context context, String source, String pkg, String artist, String title, long durationMs, int priority) {
        long now = System.currentTimeMillis();
        boolean hasHint = now < hintUntil && !TextUtils.isEmpty(hintSource);
        if (hasHint && !sameSource(source, pkg, hintSource, hintPkg)) {
            if (isGenericTeyesSource(source, pkg)) {
                source = hintSource;
                pkg = hintPkg;
                priority = Math.max(priority, hintPriority);
            } else if (priority < hintPriority + SOURCE_SWITCH_MARGIN) {
                if (AppPrefs.debug(context)) {
                    AppLog.line(context, "Мультимедиа: TEYES widget удержал " + hintSource
                            + ", пропущен " + source + " pkg=" + pkg + " priority=" + priority + "/" + hintPriority);
                }
                return false;
            }
        }
        String line = "Мультимедиа: " + dash(source) + " / " + dash(artist) + " / " + dash(title) + " / " + duration(durationMs);
        String sourceKey = sourceKey(source, pkg);
        boolean sameSource = sameSource(source, pkg, activeSource, activePkg);
        boolean locked = !TextUtils.isEmpty(activeSourceKey) && now - activeSourceAt < SOURCE_LOCK_MS;
        if (!sameSource && locked && priority < activePriority + SOURCE_SWITCH_MARGIN) {
            if (AppPrefs.debug(context)) {
                AppLog.line(context, "Мультимедиа: источник удержан " + activeSourceKey
                        + ", пропущен " + sourceKey + " priority=" + priority + "/" + activePriority);
            }
            return false;
        }
        if (!TextUtils.equals(line, lastLine) || now - lastAt > 3000) {
            activeSource = source;
            activePkg = pkg;
            activeSourceKey = sourceKey;
            activePriority = priority;
            activeSourceAt = now;
            lastLine = line;
            lastAt = now;
            AppLog.setMedia(context, line);
            CanbusControl.sendMediaMetadata(context, source, artist, !TextUtils.isEmpty(title) ? title : source);
            AppLog.line(context, "Мультимедиа: источник=" + source + " пакет=" + pkg + " трек=" + title + " автор=" + artist);
            return true;
        }
        if (sameSource) {
            activeSource = source;
            activePkg = pkg;
            activeSourceKey = sourceKey;
            activeSourceAt = now;
            activePriority = Math.max(activePriority, priority);
        }
        return false;
    }

    private static void scanCandidates(Context context) {
        if (!AppPrefs.mediaDebug(context)) return;
        long now = System.currentTimeMillis();
        if (now - lastCandidateAt < 30000) return;
        lastCandidateAt = now;
        try {
            List<android.content.pm.ResolveInfo> apps = context.getPackageManager().queryIntentActivities(
                    new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER),
                    0
            );
            int count = 0;
            for (android.content.pm.ResolveInfo ri : apps) {
                if (ri == null || ri.activityInfo == null || ri.activityInfo.applicationInfo == null) continue;
                String pkg = ri.activityInfo.applicationInfo.packageName;
                CharSequence label = ri.loadLabel(context.getPackageManager());
                String name = label == null ? "" : label.toString();
                String probe = (pkg + " " + name).toLowerCase(Locale.US);
                if (AppPrefs.mediaScanAll(context)
                        || probe.contains("radio") || probe.contains("fm") || probe.contains("teyes") || probe.contains("syu")) {
                    AppLog.line(context, "Источник найден: " + name + " (" + pkg + ")");
                    if (++count >= 12) break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static int rank(PlaybackState s) {
        if (s == null) return 0;
        int state = s.getState();
        if (state == PlaybackState.STATE_PLAYING) return 3;
        if (state == PlaybackState.STATE_BUFFERING || state == PlaybackState.STATE_FAST_FORWARDING || state == PlaybackState.STATE_REWINDING) return 2;
        if (state == PlaybackState.STATE_PAUSED || state == PlaybackState.STATE_STOPPED) return 1;
        return 0;
    }

    private static int playbackRank(Context context, MediaSession.Token token) {
        if (token == null) return 0;
        try {
            return rank(new MediaController(context, token).getPlaybackState());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int freshness(long postTime) {
        if (postTime <= 0) return 0;
        long age = Math.max(0L, System.currentTimeMillis() - postTime);
        if (age < 10_000L) return 180;
        if (age < 60_000L) return 120;
        if (age < 5 * 60_000L) return 50;
        return 0;
    }

    private static int state(PlaybackState s) {
        return s == null ? -1 : s.getState();
    }

    private static int priorityForPlayback(int playbackRank) {
        if (playbackRank >= 3) return 145;
        if (playbackRank == 2) return 120;
        if (playbackRank == 1) return 70;
        return 35;
    }

    private static String text(Bundle b, String key) {
        CharSequence cs = b.getCharSequence(key);
        return cs == null ? null : cs.toString();
    }

    private static String firstLine(Bundle b, String key) {
        CharSequence[] lines = b.getCharSequenceArray(key);
        if (lines == null || lines.length == 0) return null;
        for (CharSequence line : lines) {
            if (!TextUtils.isEmpty(line)) return line.toString();
        }
        return null;
    }

    private static String firstText(String... values) {
        if (values == null) return null;
        for (String value : values) {
            String clean = clean(value);
            if (!TextUtils.isEmpty(clean)) return clean;
        }
        return null;
    }

    private static void normalizeCandidate(NotificationCandidate c) {
        if (c == null) return;
        c.title = clean(c.title);
        c.artist = clean(c.artist);
        c.sub = clean(c.sub);
        c.big = clean(c.big);
        if (same(c.title, c.label)) c.title = firstText(c.artist, c.sub, c.big);
        if (!TextUtils.isEmpty(c.title) && TextUtils.isEmpty(c.artist)) {
            String[] split = splitArtistTitle(c.title);
            if (split != null) {
                c.artist = split[0];
                c.title = split[1];
            }
        }
    }

    private static String[] splitArtistTitle(String value) {
        if (TextUtils.isEmpty(value)) return null;
        String[] seps = new String[]{" - ", " – ", " — "};
        for (String sep : seps) {
            int pos = value.indexOf(sep);
            if (pos <= 0 || pos >= value.length() - sep.length()) continue;
            return new String[]{value.substring(0, pos).trim(), value.substring(pos + sep.length()).trim()};
        }
        return null;
    }

    private static boolean ignorePackage(Context context, String pkg) {
        if (TextUtils.isEmpty(pkg)
                || TextUtils.equals(pkg, context.getPackageName())
                || "com.kia.navi".equals(pkg)
                || "com.sorento.navi".equals(pkg)
                || "android".equals(pkg)
                || "com.android.systemui".equals(pkg)
                || "com.google.android.gms".equals(pkg)
                || "com.google.android.as".equals(pkg)
                || "com.android.vending".equals(pkg)
                || "com.android.settings".equals(pkg)
                || "com.android.launcher3".equals(pkg)
                || "ru.yandex.yandexnavi".equals(pkg)) {
            return true;
        }
        String p = pkg.toLowerCase(Locale.US);
        return p.contains("tpms") || p.contains("obd") || p.contains("canbus") || p.contains("navinfo")
                || p.contains("account") || p.contains("setupwizard");
    }

    private static boolean allowedMediaPackage(String pkg) {
        if (pkg == null) return false;
        String p = pkg.toLowerCase(Locale.US);
        return "ru.yandex.music".equals(p)
                || "com.yandex.music".equals(p)
                || "com.spotify.music".equals(p)
                || "com.google.android.apps.youtube.music".equals(p)
                || "com.vkontakte.android".equals(p)
                || "ru.mts.music".equals(p)
                || "com.apple.android.music".equals(p)
                || "com.yf.teyesspotify".equals(p)
                || "com.spd.media".equals(p)
                || "com.spd.radio".equals(p)
                || "com.spd.spdmedia".equals(p)
                || "com.spd.bluetooth".equals(p)
                || "com.alink.bluetooth".equals(p)
                || "com.alink.carplay".equals(p)
                || "com.alink.androidauto".equals(p)
                || "com.teyes.music.widget".equals(p);
    }

    private static boolean looksMedia(String pkg) {
        if (pkg == null) return false;
        String p = pkg.toLowerCase(Locale.US);
        return p.contains("music") || p.contains("audio") || p.contains("radio") || p.contains("media")
                || p.contains("player") || p.contains("bluetooth") || p.contains("syu") || p.contains("fm")
                || p.contains("yandex") || p.contains("zvuk") || p.contains("spotify") || p.contains("youtube");
    }

    private static boolean looksTrackLike(NotificationCandidate c) {
        if (c == null || TextUtils.isEmpty(c.title)) return false;
        if (isNoise(c.label, c.title, c.artist, c.sub, c.big)) return false;
        if (looksMedia(c.pkg) || looksMedia(c.label)) return true;
        if (!TextUtils.isEmpty(c.artist) && !same(c.title, c.artist)) return true;
        String text = (c.title + " " + dash(c.artist) + " " + dash(c.sub)).toLowerCase(Locale.US);
        return text.contains(" - ") || text.contains(" — ") || text.contains(" – ")
                || text.contains("track") || text.contains("song") || text.contains("artist")
                || text.contains("трек") || text.contains("песня") || text.contains("исполнитель");
    }

    private static boolean isNoise(String... values) {
        ArrayList<String> parts = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                String clean = clean(value);
                if (!TextUtils.isEmpty(clean)) parts.add(clean.toLowerCase(Locale.US));
            }
        }
        if (parts.isEmpty()) return false;
        String all = TextUtils.join(" ", parts);
        return all.contains("download") || all.contains("downloading") || all.contains("downloaded")
                || all.contains("скачан") || all.contains("скачано") || all.contains("загруз")
                || all.contains("установ") || all.contains("обновлен") || all.contains("обновлён")
                || all.contains("permission") || all.contains("разрешен") || all.contains("разрешён")
                || all.contains("sign in") || all.contains("account") || all.contains("аккаунт")
                || all.contains("вход в аккаунт") || all.contains("google play services")
                || all.contains("сервисы google play")
                || all.contains("нет уведом") || all.contains("notification listener")
                || all.contains("android system") || all.contains("system intelligence");
    }

    private static boolean same(String left, String right) {
        return !TextUtils.isEmpty(left) && !TextUtils.isEmpty(right)
                && left.trim().equalsIgnoreCase(right.trim());
    }

    private static String clean(String value) {
        if (value == null) return null;
        String out = value.replace('\n', ' ').replace('\r', ' ').trim();
        while (out.contains("  ")) out = out.replace("  ", " ");
        return out.length() == 0 ? null : out;
    }

    private static String label(Context context, String pkg) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(info);
            if (!TextUtils.isEmpty(label)) return label.toString();
        } catch (Exception ignored) {
        }
        return pkg;
    }

    private static String sourceLabel(Context context, String pkg) {
        String label = label(context, pkg);
        String probe = ((pkg == null ? "" : pkg) + " " + (label == null ? "" : label)).toLowerCase(Locale.US);
        if (probe.contains("yandex") || probe.contains("яндекс")) return "Яндекс Музыка";
        if (probe.contains("spotify")) return "Spotify";
        if (probe.contains("youtube")) return "YouTube Music";
        if (probe.contains("carplay")) return "CarPlay";
        if (probe.contains("androidauto") || probe.contains("android auto")) return "Android Auto";
        if (probe.contains("bluetooth") || probe.contains("btmusic")) return "Bluetooth";
        if (probe.contains("radio") || probe.contains("fm")) return "Радио";
        if (probe.contains("teyes")) return label == null ? "TEYES" : label.replace("Teyes", "TEYES");
        return label;
    }

    private static String sourceKey(String source, String pkg) {
        return dash(pkg).toLowerCase(Locale.US) + "|" + dash(source).toLowerCase(Locale.US);
    }

    private static boolean sameSource(String source, String pkg, String otherSource, String otherPkg) {
        return TextUtils.equals(sourceKey(source, pkg), sourceKey(otherSource, otherPkg))
                || (!TextUtils.isEmpty(source) && !TextUtils.isEmpty(otherSource)
                && source.trim().equalsIgnoreCase(otherSource.trim()));
    }

    private static boolean isGenericTeyesSource(String source, String pkg) {
        String p = dash(pkg).toLowerCase(Locale.US);
        String s = dash(source).toLowerCase(Locale.US);
        return "com.spd.media".equals(p)
                || "com.spd.spdmedia".equals(p)
                || "com.teyes.music.widget".equals(p)
                || s.equals("teyes")
                || s.equals("teyes media")
                || s.equals("teyes video")
                || s.equals("-");
    }

    private static String dash(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    private static String duration(long ms) {
        if (ms < 0) return "--:--";
        long sec = ms / 1000;
        return (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
    }
}
