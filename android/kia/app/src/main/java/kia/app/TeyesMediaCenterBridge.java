package kia.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.text.TextUtils;

import java.util.Locale;

final class TeyesMediaCenterBridge {
    private static final String ACTION = "com.spd.teyes.media.center";
    private static final String DESCRIPTOR = "com.spd.teyes.IMediaCenterService";
    private static final String CALLBACK_DESCRIPTOR = "com.spd.teyes.IMediaCenterCallback";
    private static final String CALLBACK_NAME = "kia.app";
    private static final String PACKAGE = "com.spd.home";
    private static final String SERVICE = "com.spd.teyes.mediacenter.TeyeMediaCenterService";
    private static final int TRANSACTION_REGISTER_CALLBACK = 1;
    private static final int TRANSACTION_UNREGISTER_CALLBACK = 2;
    private static final int TRANSACTION_GET_CURRENT_SOURCE_ID = 3;
    private static final int TRANSACTION_GET_CURRENT_MEDIA_INFO = 4;
    private static final int TRANSACTION_GET_CURRENT_SOURCE_TYPE = 14;
    private static final int SOURCE_RADIO = 9;
    private static final int SOURCE_NETWORK_RADIO = 50;
    private static final long SNAPSHOT_TTL_MS = 4_000L;

    private static Context appContext;
    private static IBinder remote;
    private static boolean bound;
    private static boolean binding;
    private static long lastBindAttemptAt;
    private static long lastBindLogAt;
    private static MediaCenterInfo currentInfo;
    private static int currentSourceId = -1;
    private static String currentSourceType = "";
    private static long currentAt;
    private static String lastLogKey = "";

    private static final IBinder CALLBACK = new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (code == INTERFACE_TRANSACTION) {
                if (reply != null) reply.writeString(CALLBACK_DESCRIPTOR);
                return true;
            }
            try {
                data.enforceInterface(CALLBACK_DESCRIPTOR);
                switch (code) {
                    case 1:
                        currentSourceId = data.readInt();
                        break;
                    case 2:
                        MediaCenterInfo info = readTypedInfo(data);
                        if (info != null) updateInfo(info);
                        break;
                    case 3:
                        data.readInt();
                        data.readInt();
                        break;
                    case 4:
                        if (currentInfo != null) currentInfo.playState = data.readInt();
                        break;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
                if (reply != null) reply.writeNoException();
                Context app = appContext;
                if (app != null) TeyesRadioBridge.scanNow(app);
                return true;
            } catch (Exception ignored) {
                return true;
            }
        }
    };

    private static final ServiceConnection CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            remote = service;
            bound = true;
            binding = false;
            AppLog.line(appContext, "TEYES center: сервис подключён");
            registerCallback(service);
            refresh(appContext);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remote = null;
            bound = false;
            binding = false;
            currentInfo = null;
            currentSourceId = -1;
            currentSourceType = "";
            currentAt = 0L;
            AppLog.line(appContext, "TEYES center: сервис отключён");
        }
    };

    private TeyesMediaCenterBridge() {
    }

    static synchronized void start(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        ensureBound(appContext);
    }

    static synchronized void stop(Context context) {
        Context app = context == null ? appContext : context.getApplicationContext();
        if (app != null && (bound || binding)) {
            try {
                unregisterCallback(remote);
                app.unbindService(CONNECTION);
            } catch (Exception ignored) {
            }
        }
        remote = null;
        bound = false;
        binding = false;
        currentInfo = null;
        currentSourceId = -1;
        currentSourceType = "";
        currentAt = 0L;
        appContext = null;
    }

    static synchronized String currentRadioStation(Context context) {
        refresh(context);
        if (!isFreshLocalRadio()) return null;
        MediaCenterInfo info = currentInfo;
        if (info == null) return null;
        return firstStationText(info.title, info.artist, info.mediaCode);
    }

    static synchronized boolean reportCurrentMedia(Context context) {
        refresh(context);
        if (!isFreshMedia() || isFreshLocalRadio()) return false;
        MediaCenterInfo info = currentInfo;
        if (info == null) return false;
        String source = sourceLabel(info);
        String title = firstMediaText(info.title, info.mediaCode);
        String artist = artistText(info, source);
        if (TextUtils.isEmpty(source) || TextUtils.isEmpty(title) && TextUtils.isEmpty(artist)) return false;
        return MediaMonitor.reportExternal(context, source, packageName(info, source),
                artist, title, durationMs(info), priority(source), false);
    }

    static synchronized boolean hasFreshNonLocalMedia(Context context) {
        refresh(context);
        return isFreshMedia() && !isFreshLocalRadio();
    }

    private static void refresh(Context context) {
        if (context != null) appContext = context.getApplicationContext();
        if (remote == null) {
            ensureBound(appContext);
            return;
        }
        MediaCenterInfo info = readCurrentMediaInfo(remote);
        if (info != null) updateInfo(info);
        int sourceId = readInt(remote, TRANSACTION_GET_CURRENT_SOURCE_ID, -1);
        if (sourceId >= 0) currentSourceId = sourceId;
        String sourceType = readString(remote, TRANSACTION_GET_CURRENT_SOURCE_TYPE);
        if (!TextUtils.isEmpty(sourceType)) currentSourceType = sourceType;
        logState();
    }

    private static void updateInfo(MediaCenterInfo info) {
        currentInfo = info;
        currentSourceId = info.sourceId;
        currentAt = System.currentTimeMillis();
        logState();
    }

    private static boolean isFreshMedia() {
        long age = System.currentTimeMillis() - currentAt;
        if (age > SNAPSHOT_TTL_MS) return false;
        return currentInfo != null;
    }

    private static boolean isFreshLocalRadio() {
        if (!isFreshMedia()) return false;
        MediaCenterInfo info = currentInfo;
        return info != null && info.sourceId == SOURCE_RADIO;
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
                AppLog.line(context, "TEYES center: сервис недоступен");
            }
        } catch (Exception e) {
            binding = false;
            if (now - lastBindLogAt > 30_000L) {
                lastBindLogAt = now;
                AppLog.line(context, "TEYES center: bind " + e.getClass().getSimpleName());
            }
        }
    }

    private static void registerCallback(IBinder binder) {
        if (binder == null) return;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeString(CALLBACK_NAME);
            data.writeStrongBinder(CALLBACK);
            if (binder.transact(TRANSACTION_REGISTER_CALLBACK, data, reply, 0)) {
                reply.readException();
                AppLog.line(appContext, "TEYES center: callback подключён");
            }
        } catch (Exception e) {
            AppLog.line(appContext, "TEYES center: callback " + e.getClass().getSimpleName());
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private static void unregisterCallback(IBinder binder) {
        if (binder == null) return;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeString(CALLBACK_NAME);
            if (binder.transact(TRANSACTION_UNREGISTER_CALLBACK, data, reply, 0)) {
                reply.readException();
            }
        } catch (Exception ignored) {
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private static MediaCenterInfo readCurrentMediaInfo(IBinder binder) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            if (!binder.transact(TRANSACTION_GET_CURRENT_MEDIA_INFO, data, reply, 0)) return null;
            reply.readException();
            return readTypedInfo(reply);
        } catch (Exception ignored) {
            return null;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private static MediaCenterInfo readTypedInfo(Parcel parcel) {
        if (parcel == null || parcel.readInt() == 0) return null;
        MediaCenterInfo out = new MediaCenterInfo();
        parcel.readHashMap(TeyesMediaCenterBridge.class.getClassLoader());
        parcel.readHashMap(TeyesMediaCenterBridge.class.getClassLoader());
        out.sourceId = parcel.readInt();
        out.title = clean(parcel.readString());
        out.artist = clean(parcel.readString());
        out.album = clean(parcel.readString());
        out.playState = parcel.readInt();
        out.playCurrent = parcel.readInt();
        out.playTotal = parcel.readInt();
        out.musicPackage = clean(parcel.readString());
        out.mediaCode = clean(parcel.readString());
        return out;
    }

    private static int readInt(IBinder binder, int transaction, int fallback) {
        Parcel reply = transactNoArgs(binder, transaction);
        if (reply == null) return fallback;
        try {
            return reply.readInt();
        } catch (Exception ignored) {
            return fallback;
        } finally {
            reply.recycle();
        }
    }

    private static String readString(IBinder binder, int transaction) {
        Parcel reply = transactNoArgs(binder, transaction);
        if (reply == null) return null;
        try {
            return clean(reply.readString());
        } catch (Exception ignored) {
            return null;
        } finally {
            reply.recycle();
        }
    }

    private static Parcel transactNoArgs(IBinder binder, int transaction) {
        if (binder == null) return null;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            if (!binder.transact(transaction, data, reply, 0)) {
                reply.recycle();
                return null;
            }
            reply.readException();
            return reply;
        } catch (Exception ignored) {
            reply.recycle();
            return null;
        } finally {
            data.recycle();
        }
    }

    private static void logState() {
        MediaCenterInfo info = currentInfo;
        if (info == null) return;
        String key = info.sourceId + "|" + currentSourceId + "|" + currentSourceType + "|"
                + info.title + "|" + info.artist + "|" + info.album + "|" + info.musicPackage + "|" + info.mediaCode;
        if (TextUtils.equals(key, lastLogKey)) return;
        lastLogKey = key;
        AppLog.line(appContext, "TEYES center: source=" + info.sourceId
                + "/" + currentSourceId
                + " type=" + dash(currentSourceType)
                + " title=" + dash(info.title)
                + " artist=" + dash(info.artist)
                + " album=" + dash(info.album)
                + " pkg=" + dash(info.musicPackage)
                + " code=" + dash(info.mediaCode)
                + " state=" + info.playState
                + " pos=" + info.playCurrent
                + "/" + info.playTotal);
    }

    private static String firstStationText(String... values) {
        if (values == null) return null;
        for (String value : values) {
            String text = clean(value);
            if (!TextUtils.isEmpty(text) && !isNonStationText(text)) return text;
        }
        return null;
    }

    private static boolean isNonStationText(String value) {
        String text = clean(value);
        if (TextUtils.isEmpty(text)) return false;
        String lower = text.toLowerCase(Locale.US);
        if (lower.startsWith("/") || lower.startsWith("file:") || lower.endsWith(".png")
                || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return true;
        }
        if (lower.equals("fm") || lower.equals("am") || lower.equals("radio") || lower.equals("радио")) return true;
        return lower.matches("^(fm|am)?\\s*\\d{2,4}([.,]\\d{1,2})?\\s*(mhz|khz)?$");
    }

    private static String sourceLabel(MediaCenterInfo info) {
        String probe = (clean(currentSourceType) + " " + clean(info.musicPackage) + " "
                + clean(info.mediaCode) + " " + clean(info.album)).toLowerCase(Locale.US);
        if (info.sourceId == SOURCE_NETWORK_RADIO || probe.contains("net_fm")
                || probe.contains("station")) return "S-radio";
        if (isBluetoothText(probe)) return "Bluetooth";
        if (probe.contains("yandex") || probe.contains("яндекс")) return "Яндекс Музыка";
        if (probe.contains("spotify")) return "Spotify";
        if (probe.contains("youtube")) return "YouTube Music";
        if (probe.contains("usb") || probe.contains("local_music") || probe.contains("storage")
                || probe.contains("spd.media")) return "USB";
        if (probe.contains("radio")) return "Радио";
        if (!TextUtils.isEmpty(clean(currentSourceType))) return currentSourceType;
        return "TEYES Media";
    }

    private static String packageName(MediaCenterInfo info, String source) {
        String type = clean(currentSourceType);
        if (!TextUtils.isEmpty(type) && type.contains(".")) return type;
        String s = clean(source);
        String lower = s == null ? "" : s.toLowerCase(Locale.US);
        if (isBluetoothText(lower)) return "com.android.bluetooth";
        if (lower.contains("yandex") || lower.contains("яндекс")) return "ru.yandex.music";
        if (lower.contains("s-radio") || lower.contains("radio")) return "com.spd.radio";
        return "com.spd.media";
    }

    private static String firstMediaText(String... values) {
        if (values == null) return null;
        for (String value : values) {
            String text = clean(value);
            if (!TextUtils.isEmpty(text) && !isPathText(text)) return text;
        }
        return null;
    }

    private static String artistText(MediaCenterInfo info, String source) {
        String s = clean(source);
        String lower = s == null ? "" : s.toLowerCase(Locale.US);
        if (lower.contains("s-radio")) return "";
        String artist = clean(info.artist);
        if (TextUtils.isEmpty(artist) || isPathText(artist)) return "";
        return artist;
    }

    private static boolean isPathText(String value) {
        String text = clean(value);
        if (TextUtils.isEmpty(text)) return false;
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("/") || lower.startsWith("file:") || lower.endsWith(".png")
                || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private static long durationMs(MediaCenterInfo info) {
        if (info == null || info.playTotal <= 0) return -1L;
        return info.playTotal < 10_000 ? info.playTotal * 1000L : info.playTotal;
    }

    private static int priority(String source) {
        String text = clean(source);
        String lower = text == null ? "" : text.toLowerCase(Locale.US);
        if (isBluetoothText(lower)) return 152;
        if (lower.contains("yandex") || lower.contains("spotify") || lower.contains("youtube")) return 148;
        if (lower.contains("usb")) return 145;
        if (lower.contains("s-radio")) return 145;
        return 130;
    }

    private static String clean(String value) {
        if (value == null) return null;
        String out = value.replace('\n', ' ').replace('\r', ' ').trim();
        while (out.contains("  ")) out = out.replace("  ", " ");
        String compact = out.replace(" ", "");
        if (compact.matches("[-_.]+")) return null;
        return out.length() == 0 ? null : out;
    }

    private static boolean isBluetoothText(String value) {
        String text = clean(value);
        if (TextUtils.isEmpty(text)) return false;
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("bluetooth")
                || lower.equals("bt")
                || lower.startsWith("bt ")
                || lower.contains("btmusic")
                || lower.contains("bt_music")
                || lower.contains("bt-music")
                || lower.contains("bt audio")
                || lower.contains("bt_audio")
                || lower.contains("bt-audio")
                || lower.contains("bt музыка")
                || lower.contains("a2dp")
                || lower.contains("avrcp");
    }

    private static String dash(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    private static final class MediaCenterInfo {
        int sourceId = -1;
        int playState = -1;
        int playCurrent;
        int playTotal;
        String title;
        String artist;
        String album;
        String musicPackage;
        String mediaCode;
    }
}
