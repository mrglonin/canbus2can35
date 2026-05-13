package kia.app;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

final class RctaAlertManager {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static long lastSoundAt;

    private RctaAlertManager() {
    }

    static void onWarning(Context context, boolean left, boolean right) {
        if (context == null || (!left && !right)) return;
        long now = System.currentTimeMillis();
        if (now - lastSoundAt < 850L) return;
        lastSoundAt = now;
        play(AudioManager.STREAM_ALARM, ToneGenerator.TONE_SUP_ERROR, 100, 180, 0L);
        play(AudioManager.STREAM_MUSIC, ToneGenerator.TONE_PROP_BEEP2, 95, 140, 210L);
    }

    private static void play(int stream, int toneId, int volume, int durationMs, long delayMs) {
        HANDLER.postDelayed(() -> {
            try {
                ToneGenerator tone = new ToneGenerator(stream, volume);
                tone.startTone(toneId, durationMs);
                HANDLER.postDelayed(tone::release, durationMs + 90L);
            } catch (Exception ignored) {
            }
        }, delayMs);
    }
}
