package kia.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.text.TextUtils;

import java.util.Locale;

final class TeyesMediaBridge {
    private static final String ACTION = "android.spd.IMediaService";
    private static final String DESCRIPTOR = "com.spd.media.aidl.IMediaService";
    private static final String PACKAGE = "com.spd.media";
    private static final String SERVICE = "com.spd.media.service.MediaService";
    private static final int TRANSACTION_GET_NOW_PLAYING = 6;
    private static final int TRANSACTION_GET_NOW_PLAYING_BY_TYPE = 7;
    private static final long POLL_MS = 1000L;
    private static final int[] MEDIA_TYPES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static int nextMediaTypeIndex;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static Context appContext;
    private static IBinder remote;
    private static boolean bound;
    private static boolean binding;
    private static long lastBindAttemptAt;
    private static long lastBindLogAt;

    private static final Runnable POLL = new Runnable() {
        @Override
        public void run() {
            scanNow(appContext);
            if (appContext != null) HANDLER.postDelayed(this, POLL_MS);
        }
    };

    private static final ServiceConnection CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            remote = service;
            bound = true;
            binding = false;
            AppLog.line(appContext, "TEYES media: сервис подключён");
            scanNow(appContext);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remote = null;
            bound = false;
            binding = false;
            AppLog.line(appContext, "TEYES media: сервис отключён");
        }
    };

    private TeyesMediaBridge() {
    }

    static synchronized void start(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        ensureBound(appContext);
        HANDLER.removeCallbacks(POLL);
        HANDLER.post(POLL);
    }

    static synchronized void stop(Context context) {
        HANDLER.removeCallbacks(POLL);
        Context app = context == null ? appContext : context.getApplicationContext();
        if (app != null && (bound || binding)) {
            try {
                app.unbindService(CONNECTION);
            } catch (Exception ignored) {
            }
        }
        remote = null;
        bound = false;
        binding = false;
        appContext = null;
    }

    static synchronized boolean scanNow(Context context) {
        if (context == null) return false;
        appContext = context.getApplicationContext();
        if (remote == null) {
            ensureBound(appContext);
            return false;
        }
        NowPlaying nowPlaying = readBestNowPlaying(remote);
        if (nowPlaying == null || !nowPlaying.hasUsefulData()) return false;
        String source = sourceLabel(nowPlaying);
        String title = clean(nowPlaying.songTitle);
        String artist = clean(nowPlaying.songArtist);
        if (TextUtils.isEmpty(title)) title = fileName(nowPlaying.fullPath);
        return MediaMonitor.reportExternal(appContext, source, PACKAGE, artist, title, nowPlaying.durationMs, nowPlaying.priority());
    }

    private static void ensureBound(Context context) {
        if (context == null || bound || binding) return;
        long now = System.currentTimeMillis();
        if (now - lastBindAttemptAt < 10_000L) return;
        lastBindAttemptAt = now;
        Intent intent = new Intent(ACTION);
        intent.setComponent(new ComponentName(PACKAGE, SERVICE));
        try {
            binding = context.bindService(intent, CONNECTION, Context.BIND_AUTO_CREATE);
            if (!binding && now - lastBindLogAt > 30_000L) {
                lastBindLogAt = now;
                AppLog.line(context, "TEYES media: сервис недоступен");
            }
        } catch (Exception e) {
            binding = false;
            if (now - lastBindLogAt > 30_000L) {
                lastBindLogAt = now;
                AppLog.line(context, "TEYES media: bind " + e.getClass().getSimpleName());
            }
        }
    }

    private static NowPlaying readBestNowPlaying(IBinder binder) {
        NowPlaying best = null;
        best = choose(best, readNowPlaying(binder, -1));
        for (int i = 0; i < 3; i++) {
            int type = MEDIA_TYPES[nextMediaTypeIndex % MEDIA_TYPES.length];
            nextMediaTypeIndex = (nextMediaTypeIndex + 1) % MEDIA_TYPES.length;
            best = choose(best, readNowPlaying(binder, type));
        }
        return best;
    }

    private static NowPlaying choose(NowPlaying best, NowPlaying candidate) {
        if (candidate == null || !candidate.hasUsefulData()) return best;
        if (best == null || candidate.priority() > best.priority()) return candidate;
        return best;
    }

    private static NowPlaying readNowPlaying(IBinder binder, int mediaTypeRequest) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            int transaction = mediaTypeRequest < 0 ? TRANSACTION_GET_NOW_PLAYING : TRANSACTION_GET_NOW_PLAYING_BY_TYPE;
            if (mediaTypeRequest >= 0) data.writeInt(mediaTypeRequest);
            if (!binder.transact(transaction, data, reply, 0)) return null;
            reply.readException();
            if (reply.readInt() == 0) return null;
            NowPlaying out = NowPlaying.read(reply);
            out.requestedType = mediaTypeRequest;
            return out;
        } catch (Exception ignored) {
            return null;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private static String sourceLabel(NowPlaying nowPlaying) {
        if (nowPlaying == null) return "TEYES";
        String probe = (clean(nowPlaying.fullPath) + " " + clean(nowPlaying.songTitle) + " " + clean(nowPlaying.songArtist))
                .toLowerCase(Locale.US);
        if (probe.contains("bluetooth") || probe.contains("btmusic")) return "Bluetooth";
        if (probe.contains("carplay")) return "CarPlay";
        if (probe.contains("androidauto") || probe.contains("android auto")) return "Android Auto";
        if (probe.contains("radio") || probe.contains("fm://") || probe.contains("frequency")) return "Радио";
        if ((nowPlaying.deviceMask & 0x01) != 0) return "USB";
        if ((nowPlaying.deviceMask & 0x02) != 0) return "USB";
        if ((nowPlaying.deviceMask & 0x10) != 0) return "USB";
        if (nowPlaying.requestedType == 4) return "Bluetooth";
        if (nowPlaying.requestedType == 5) return "CarPlay";
        if (nowPlaying.requestedType == 6) return "Android Auto";
        if (nowPlaying.requestedType == 7 || nowPlaying.requestedType == 9) return "Радио";
        if (nowPlaying.requestedType == 1 || nowPlaying.requestedType == 2) return "USB";
        if (nowPlaying.mediaType == 2) return "TEYES Media";
        if (nowPlaying.mediaType == 3) return "TEYES Video";
        return "TEYES";
    }

    private static String clean(String value) {
        if (value == null) return null;
        String out = value.replace('\n', ' ').replace('\r', ' ').trim();
        while (out.contains("  ")) out = out.replace("  ", " ");
        return out.length() == 0 ? null : out;
    }

    private static final class NowPlaying {
        int playStatus;
        int playtimeMs;
        int durationMs;
        int mediaType;
        int deviceMask;
        int requestedType;
        String songTitle;
        String songArtist;
        String fullPath;

        static NowPlaying read(Parcel in) {
            NowPlaying out = new NowPlaying();
            in.readInt(); // fileType
            in.readInt(); // list_id
            in.readInt(); // listType
            in.readInt(); // playIndex
            in.readInt(); // playCount
            out.playStatus = in.readInt();
            in.readInt(); // playSpeed
            in.readInt(); // repeatMode
            in.readInt(); // shuffleMode
            out.playtimeMs = in.readInt();
            out.durationMs = in.readInt();
            in.readLong(); // file_id
            in.readLong(); // artist_id
            in.readLong(); // album_id
            in.readInt(); // parent_id
            in.readInt(); // storage_id
            in.readInt(); // ability
            in.readInt(); // lyricsOffsetMs
            in.readInt(); // select_parent
            in.readLong(); // select_artist
            in.readLong(); // select_album
            out.songTitle = in.readString();
            out.songArtist = in.readString();
            in.readString(); // songAlbum
            in.readString(); // albumArt
            out.fullPath = in.readString();
            in.readInt(); // audioSessionID
            out.mediaType = in.readInt();
            out.deviceMask = in.readInt();
            return out;
        }

        boolean hasUsefulData() {
            if (playStatus <= 0) return false;
            if (!TextUtils.isEmpty(clean(songTitle)) || !TextUtils.isEmpty(clean(songArtist))) return true;
            String path = clean(fullPath);
            return !TextUtils.isEmpty(path) && !path.toLowerCase(Locale.US).endsWith("/");
        }

        int priority() {
            int sourceBonus = requestedType >= 0 ? 8 : 0;
            if (playStatus == 3) return 145 + sourceBonus;
            if (playStatus >= 2 && playStatus != 4) return 120;
            if (playStatus == 4) return 70;
            return 55;
        }
    }

    private static String fileName(String value) {
        String clean = clean(value);
        if (TextUtils.isEmpty(clean)) return null;
        int slash = Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\'));
        return slash >= 0 && slash + 1 < clean.length() ? clean.substring(slash + 1) : clean;
    }
}
