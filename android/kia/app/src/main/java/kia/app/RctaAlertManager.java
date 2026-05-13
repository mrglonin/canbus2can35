package kia.app;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

final class RctaAlertManager {
    private static final long ACTIVE_HOLD_MS = 2400L;
    private static final long SOUND_PERIOD_MS = 720L;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static Context soundContext;
    private static boolean soundActive;
    private static long activeUntil;
    private static int soundGeneration;

    private static final Runnable SOUND_TICK = new Runnable() {
        @Override
        public void run() {
            if (!soundActive) return;
            Context context = soundContext;
            if (context == null || System.currentTimeMillis() > activeUntil) {
                stop(context, false);
                return;
            }
            int generation = soundGeneration;
            play(AudioManager.STREAM_ALARM, ToneGenerator.TONE_SUP_ERROR, 100, 180, 0L, generation);
            play(AudioManager.STREAM_MUSIC, ToneGenerator.TONE_PROP_BEEP2, 95, 140, 210L, generation);
            HANDLER.postDelayed(this, SOUND_PERIOD_MS);
        }
    };

    private RctaAlertManager() {
    }

    static void onWarning(Context context, boolean left, boolean right) {
        if (context == null) return;
        if (!left && !right) {
            clear(context);
            return;
        }
        long now = System.currentTimeMillis();
        activeUntil = now + ACTIVE_HOLD_MS;
        if (soundActive) return;
        soundContext = context.getApplicationContext();
        soundActive = true;
        soundGeneration++;
        HANDLER.removeCallbacks(SOUND_TICK);
        HANDLER.post(SOUND_TICK);
        AppLog.line(context, "RCTA: звук включён");
    }

    static void clear(Context context) {
        activeUntil = 0L;
        stop(context, true);
    }

    private static void stop(Context context, boolean log) {
        if (!soundActive) return;
        soundActive = false;
        soundGeneration++;
        HANDLER.removeCallbacks(SOUND_TICK);
        if (log && context != null) AppLog.line(context, "RCTA: звук выключен");
    }

    private static void play(int stream, int toneId, int volume, int durationMs, long delayMs, int generation) {
        HANDLER.postDelayed(() -> {
            if (!soundActive || generation != soundGeneration) return;
            try {
                ToneGenerator tone = new ToneGenerator(stream, volume);
                tone.startTone(toneId, durationMs);
                HANDLER.postDelayed(tone::release, durationMs + 90L);
            } catch (Exception ignored) {
            }
        }, delayMs);
    }
}
