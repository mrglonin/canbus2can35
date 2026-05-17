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

final class TeyesRadioBridge {
    private static final String ACTION = "com.spd.radio.service";
    private static final String DESCRIPTOR = "com.spd.radio.IRadioAidlInterface";
    private static final String CALLBACK_DESCRIPTOR = "com.spd.radio.IRadioAidlCallback";
    private static final String CALLBACK_NAME = "kia.app";
    private static final String PACKAGE = "com.spd.radio";
    private static final String SERVICE = "com.spd.radio.service.RadioService";
    private static final int TRANSACTION_ENTER_SOURCE = 1;
    private static final int TRANSACTION_GET_FREQ_INFO = 5;
    private static final int TRANSACTION_GET_RDS_PS = 11;
    private static final int TRANSACTION_GET_RDS_RT = 12;
    private static final int TRANSACTION_GET_RADIO_STATUS = 13;
    private static final int TRANSACTION_REGISTER_CALLBACK = 15;
    private static final int TRANSACTION_UNREGISTER_CALLBACK = 20;
    private static final long ACTIVE_TTL_MS = 4_000L;
    private static final long CALLBACK_FORCE_MS = 2_500L;
    private static final long RDS_SETTLE_MS = 1_400L;
    private static final long RADIO_TEXT_HOLD_MS = 2_500L;
    private static final long FAST_POLL_AFTER_FREQ_MS = 1_500L;
    private static final long RADIO_FAST_POLL_MS = 100L;
    private static final long RADIO_ACTIVE_POLL_MS = 200L;
    private static final long DEFAULT_POLL_MS = 1_000L;

    private static Context appContext;
    private static IBinder remote;
    private static boolean bound;
    private static boolean binding;
    private static long lastBindAttemptAt;
    private static long lastBindLogAt;
    private static String lastLogKey = "";
    private static boolean currentActive;
    private static String currentTitle = "";
    private static long currentAt;
    private static long lastCheckedAt;
    private static long lastCallbackAt;
    private static long lastForcedCallbackAt;
    private static String lastCallbackReason = "";
    private static int lastFreq;
    private static long lastFreqChangedAt;
    private static long lastRdsPsCallbackAt;
    private static String lastRdsPsCallbackValue = "";
    private static boolean radioTextVisible = true;
    private static int displayedTextFreq;
    private static String displayedTextTitle = "";
    private static long displayedTextAt;
    private static String lastStationTitle = "";
    private static boolean currentSearching;

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
                        markCallback("band:" + data.readString());
                        break;
                    case 2:
                        markCallback("status");
                        break;
                    case 3:
                        markCallback("freq:" + data.readInt());
                        break;
                    case 5:
                        String ps = data.readString();
                        markCallback("ps:" + ps);
                        markRdsPsCallback(ps);
                        break;
                    case 6:
                        markCallback("rt:" + data.readString());
                        break;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
                if (reply != null) reply.writeNoException();
                Context app = appContext;
                if (app != null) scanNow(app);
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
            AppLog.line(appContext, "TEYES radio: сервис подключён");
            registerCallback(service);
            scanNow(appContext);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remote = null;
            bound = false;
            binding = false;
            currentActive = false;
            currentSearching = false;
            AppLog.line(appContext, "TEYES radio: сервис отключён");
        }
    };

    private TeyesRadioBridge() {
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
        currentActive = false;
        currentSearching = false;
        lastFreq = 0;
        lastFreqChangedAt = 0L;
        lastRdsPsCallbackAt = 0L;
        lastRdsPsCallbackValue = "";
        radioTextVisible = true;
        displayedTextFreq = 0;
        displayedTextTitle = "";
        displayedTextAt = 0L;
        lastStationTitle = "";
        appContext = null;
    }

    static synchronized boolean scanNow(Context context) {
        if (context == null) return false;
        appContext = context.getApplicationContext();
        if (MediaMonitor.manualOverrideActive()) return false;
        if (remote == null) {
            ensureBound(appContext);
            return false;
        }
        long now = System.currentTimeMillis();
        if (TeyesMediaCenterBridge.hasFreshNonLocalMedia(appContext)) {
            currentActive = false;
            currentSearching = false;
            currentAt = now;
            return false;
        }
        Snapshot snapshot = readSnapshot(remote, now);
        lastCheckedAt = now;
        if (snapshot == null || !snapshot.active) {
            currentActive = false;
            currentSearching = false;
            currentAt = now;
            return false;
        }
        boolean wasActive = hasFreshActiveRadio();
        currentActive = true;
        currentSearching = snapshot.searching;
        currentTitle = snapshot.title;
        currentAt = now;
        lastFreq = snapshot.freq;
        boolean forceCan = !wasActive || snapshot.frequencyChanged || shouldForceCallback(now);
        boolean sent = MediaMonitor.reportRadio(appContext, snapshot.source, PACKAGE,
                snapshot.title, -1, 230, forceCan, snapshot.updateText, snapshot.clearText);
        if (forceCan) {
            lastForcedCallbackAt = Math.max(lastForcedCallbackAt, lastCallbackAt);
        }
        if (sent) {
            if (snapshot.updateText) {
                radioTextVisible = true;
                displayedTextFreq = snapshot.freq;
                displayedTextTitle = snapshot.title;
                displayedTextAt = now;
                lastStationTitle = snapshot.title;
            }
            if (snapshot.clearText && !snapshot.updateText) {
                radioTextVisible = false;
                displayedTextFreq = 0;
                displayedTextTitle = "";
                displayedTextAt = 0L;
            }
        }
        String key = snapshot.source + "|" + snapshot.title + "|" + snapshot.freq + "|" + snapshot.statusText;
        if (!TextUtils.equals(key, lastLogKey)) {
            lastLogKey = key;
            AppLog.line(appContext, "TEYES radio: источник=" + snapshot.source
                    + " freq=" + snapshot.freqText
                    + " ps=" + dash(snapshot.station)
                    + " text=" + snapshot.textMode()
                    + " status=" + snapshot.statusText);
        }
        return sent;
    }

    static synchronized boolean hasFreshActiveRadio() {
        return currentActive && System.currentTimeMillis() - currentAt <= ACTIVE_TTL_MS;
    }

    static synchronized long recommendedPollDelayMs() {
        long now = System.currentTimeMillis();
        if (!hasFreshActiveRadio()) return DEFAULT_POLL_MS;
        if (currentSearching || now - lastFreqChangedAt <= FAST_POLL_AFTER_FREQ_MS) {
            return RADIO_FAST_POLL_MS;
        }
        return RADIO_ACTIVE_POLL_MS;
    }

    static synchronized String currentTitle() {
        return currentTitle;
    }

    static synchronized boolean hasChecked() {
        return lastCheckedAt > 0;
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
                AppLog.line(context, "TEYES radio: сервис недоступен");
            }
        } catch (Exception e) {
            binding = false;
            if (now - lastBindLogAt > 30_000L) {
                lastBindLogAt = now;
                AppLog.line(context, "TEYES radio: bind " + e.getClass().getSimpleName());
            }
        }
    }

    private static void markCallback(String reason) {
        synchronized (TeyesRadioBridge.class) {
            lastCallbackAt = System.currentTimeMillis();
            lastCallbackReason = clean(reason);
        }
    }

    private static void markRdsPsCallback(String value) {
        synchronized (TeyesRadioBridge.class) {
            lastRdsPsCallbackAt = System.currentTimeMillis();
            lastRdsPsCallbackValue = clean(value);
        }
    }

    private static synchronized boolean shouldForceCallback(long now) {
        return lastCallbackAt > lastForcedCallbackAt && now - lastCallbackAt <= CALLBACK_FORCE_MS;
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
                AppLog.line(appContext, "TEYES radio: callback подключён");
            }
        } catch (Exception e) {
            AppLog.line(appContext, "TEYES radio: callback " + e.getClass().getSimpleName());
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

    private static Snapshot readSnapshot(IBinder binder, long now) {
        FreqInfo freq = readFreqInfo(binder);
        RadioStatus status = readStatus(binder);
        if (freq == null || status == null) return null;
        String band = clean(freq.band);
        String source = isAmBand(band) ? "AM 24" : "FM радио";
        String freqText = formatFrequency(band, freq.freq);
        boolean frequencyChanged = lastFreq > 0 && freq.freq > 0 && freq.freq != lastFreq;
        if (frequencyChanged) {
            lastFreqChangedAt = now;
            lastRdsPsCallbackAt = 0L;
            lastRdsPsCallbackValue = "";
        }
        boolean serviceSearching = status.isSearching();
        boolean settling = lastFreqChangedAt > 0 && now - lastFreqChangedAt < RDS_SETTLE_MS;
        boolean freshPsCallback = !TextUtils.isEmpty(lastRdsPsCallbackValue)
                && lastRdsPsCallbackAt >= lastFreqChangedAt;
        String centerStation = serviceSearching ? null : TeyesMediaCenterBridge.currentRadioStation(appContext);
        boolean centerLooksStale = !TextUtils.isEmpty(centerStation)
                && settling
                && !TextUtils.isEmpty(lastStationTitle)
                && TextUtils.equals(centerStation, lastStationTitle);
        String station = null;
        if (!serviceSearching && !centerLooksStale && !TextUtils.isEmpty(centerStation)) {
            station = centerStation;
        } else if (!serviceSearching && freshPsCallback) {
            station = lastRdsPsCallbackValue;
        } else if (!serviceSearching && !settling) {
            station = firstNonEmpty(freq.ps, readString(binder, TRANSACTION_GET_RDS_PS));
        }
        boolean hasStation = !TextUtils.isEmpty(station);
        boolean currentFreqHasText = radioTextVisible
                && displayedTextFreq == freq.freq
                && !TextUtils.isEmpty(displayedTextTitle);
        boolean sameStationShown = currentFreqHasText && TextUtils.equals(displayedTextTitle, clean(station));
        boolean updateText = hasStation && !sameStationShown;
        boolean textHold = radioTextVisible
                && displayedTextAt > 0
                && now - displayedTextAt < RADIO_TEXT_HOLD_MS;
        boolean clearText = !updateText && radioTextVisible
                && !hasStation
                && (serviceSearching || (frequencyChanged && !currentFreqHasText)
                || (!currentFreqHasText && !textHold));
        Snapshot out = new Snapshot();
        out.active = status.playState > 0 && freq.freq > 0;
        out.source = sourceWithFrequency(source, freqText);
        out.freq = freq.freq;
        out.freqText = freqText;
        out.station = station;
        out.title = hasStation ? clean(station) : "";
        out.frequencyChanged = frequencyChanged;
        out.searching = serviceSearching;
        out.updateText = updateText;
        out.clearText = clearText;
        out.statusText = status.statusText();
        return out;
    }

    private static FreqInfo readFreqInfo(IBinder binder) {
        Parcel reply = transact(binder, TRANSACTION_GET_FREQ_INFO);
        if (reply == null) return null;
        try {
            if (reply.readInt() == 0) return null;
            FreqInfo out = new FreqInfo();
            out.band = reply.readString();
            out.freq = reply.readInt();
            reply.readInt(); // min
            reply.readInt(); // max
            reply.readInt(); // step
            reply.readInt(); // pi
            out.signal = reply.readInt();
            out.ps = reply.readString();
            return out;
        } catch (Exception ignored) {
            return null;
        } finally {
            reply.recycle();
        }
    }

    private static RadioStatus readStatus(IBinder binder) {
        Parcel reply = transact(binder, TRANSACTION_GET_RADIO_STATUS);
        if (reply == null) return null;
        try {
            if (reply.readInt() == 0) return null;
            RadioStatus out = new RadioStatus();
            out.signal = reply.readInt();
            out.stereo = reply.readInt() != 0;
            reply.readInt(); // TP
            reply.readInt(); // TA
            out.seeking = reply.readInt() != 0;
            out.previewScanning = reply.readInt() != 0;
            out.autoSearching = reply.readInt() != 0;
            out.local = reply.readInt();
            reply.readInt(); // PTY
            out.playState = reply.readInt();
            return out;
        } catch (Exception ignored) {
            return null;
        } finally {
            reply.recycle();
        }
    }

    private static String readString(IBinder binder, int transaction) {
        Parcel reply = transact(binder, transaction);
        if (reply == null) return null;
        try {
            return clean(reply.readString());
        } catch (Exception ignored) {
            return null;
        } finally {
            reply.recycle();
        }
    }

    private static Parcel transact(IBinder binder, int transaction) {
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

    private static String sourceWithFrequency(String source, String freqText) {
        if (TextUtils.isEmpty(freqText)) return source;
        String cleanSource = firstNonEmpty(source, "FM радио");
        String sourceLower = cleanSource.toLowerCase(Locale.US);
        if (sourceLower.contains(freqText.toLowerCase(Locale.US))) return cleanSource;
        return cleanSource + " " + freqText;
    }

    private static String formatFrequency(String band, int freq) {
        if (freq <= 0) return null;
        if (isAmBand(band) || freq < 10_000) {
            return freq + " kHz";
        }
        int mhz10 = Math.round(freq / 100f);
        return (mhz10 / 10) + "." + (mhz10 % 10) + " MHz";
    }

    private static boolean isAmBand(String band) {
        String text = clean(band);
        return text != null && text.toLowerCase(Locale.US).startsWith("am");
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String value : values) {
            String clean = clean(value);
            if (!TextUtils.isEmpty(clean)) return clean;
        }
        return null;
    }

    private static String clean(String value) {
        if (value == null) return null;
        String out = value.replace('\n', ' ').replace('\r', ' ').trim();
        while (out.contains("  ")) out = out.replace("  ", " ");
        String compact = out.replace(" ", "");
        if (compact.matches("[-_.]+")) return null;
        return out.length() == 0 ? null : out;
    }

    private static String dash(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    private static final class FreqInfo {
        String band;
        int freq;
        int signal;
        String ps;
    }

    private static final class RadioStatus {
        int signal;
        int local;
        int playState;
        boolean stereo;
        boolean seeking;
        boolean previewScanning;
        boolean autoSearching;

        boolean isSearching() {
            return seeking || previewScanning || autoSearching;
        }

        String statusText() {
            return "play=" + playState
                    + ",signal=" + signal
                    + ",stereo=" + stereo
                    + ",seek=" + isSearching()
                    + ",local=" + local;
        }
    }

    private static final class Snapshot {
        boolean active;
        String source;
        String title;
        String station;
        String freqText;
        String statusText;
        int freq;
        boolean frequencyChanged;
        boolean updateText;
        boolean clearText;
        boolean searching;

        String textMode() {
            if (updateText) return "station";
            if (clearText) return "clear";
            if (!TextUtils.isEmpty(station)) return "hold";
            return "off";
        }
    }
}
