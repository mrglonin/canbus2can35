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
    private static Handler handler;
    private static Runnable poll;
    private static String lastLine = "";
    private static long lastAt;
    private static long lastCandidateAt;

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
        MediaSession.Token token;
    }

    private MediaMonitor() {
    }

    static void start(Context context) {
        Context app = context.getApplicationContext();
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
    }

    static void scanNow(Context context) {
        boolean found = MediaNotificationListener.refresh(context);
        if (!found) scanSessions(context);
        scanCandidates(context);
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
        if (emit(context, candidate.label + " [увед]", candidate.pkg, artist, candidate.title, -1)) {
            AppLog.line(context, "Уведомление: пакет=" + candidate.pkg
                    + " score=" + candidate.score
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
        boolean mediaPkg = looksMedia(pkg) || looksMedia(candidate.label);
        boolean scanAllMatch = AppPrefs.mediaScanAll(context) && looksTrackLike(candidate);
        if (candidate.token != null) {
            candidate.score = 1000 + (mediaCategory ? 200 : 0) + (mediaPkg ? 100 : 0);
            return candidate;
        }
        if (TextUtils.isEmpty(candidate.title)) return null;
        if (!mediaCategory && !mediaPkg && !scanAllMatch) return null;
        candidate.score = (mediaCategory ? 700 : 0)
                + (mediaPkg ? 450 : 0)
                + (scanAllMatch ? 200 : 0)
                + (!TextUtils.isEmpty(candidate.artist) ? 80 : 0)
                + (!TextUtils.isEmpty(candidate.sub) ? 20 : 0);
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
        if (emit(context, label(context, pkg), pkg, artist, title, duration)) {
            AppLog.line(context, "Сессия: пакет=" + pkg + " состояние=" + state(c.getPlaybackState()) + " трек=" + title + " автор=" + artist);
        }
        return true;
    }

    private static boolean emit(Context context, String source, String pkg, String artist, String title, long durationMs) {
        String line = "Мультимедиа: " + dash(source) + " / " + dash(artist) + " / " + dash(title) + " / " + duration(durationMs);
        long now = System.currentTimeMillis();
        if (!TextUtils.equals(line, lastLine) || now - lastAt > 3000) {
            lastLine = line;
            lastAt = now;
            AppLog.setMedia(context, line);
            CanbusControl.sendMediaTrack(context, !TextUtils.isEmpty(title) ? title : source);
            AppLog.line(context, "Мультимедиа: источник=" + source + " пакет=" + pkg + " трек=" + title + " автор=" + artist);
            return true;
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

    private static int state(PlaybackState s) {
        return s == null ? -1 : s.getState();
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
                || "ru.yandex.yandexnavi".equals(pkg)) {
            return true;
        }
        String p = pkg.toLowerCase(Locale.US);
        return p.contains("tpms") || p.contains("obd") || p.contains("canbus") || p.contains("navinfo");
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

    private static String dash(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    private static String duration(long ms) {
        if (ms < 0) return "--:--";
        long sec = ms / 1000;
        return (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
    }
}
