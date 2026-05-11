package com.sorento.navi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

final class TpmsAlertManager {
    private static final String CHANNEL = "tpms_alerts_v2";
    private static final int NOTIFICATION_ID = 73;
    private static final long USER_DISMISS_SUPPRESS_MS = 5L * 60L * 1000L;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static long lastOpenAt;
    private static long lastSoundAt;

    private TpmsAlertManager() {
    }

    static void onTpmsUpdate(Context context, TpmsState.Snapshot snapshot) {
        if (context == null || snapshot == null || !AppPrefs.tpmsEnabled(context)) return;
        TpmsState.Tire tire = firstAlert(snapshot);
        if (tire == null) return;

        long now = System.currentTimeMillis();
        if (AppPrefs.tpmsAlertSuppressed(context, now)) return;

        String message = warningMessage(snapshot, tire);
        showNotification(context, message);

        if (AppPrefs.tpmsAutoOpen(context) && now - lastOpenAt > 8000L) {
            lastOpenAt = now;
            Intent intent = new Intent(context, TpmsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("tpms_alert", true);
            try {
                context.startActivity(intent);
                AppLog.line(context, "TPMS: открыт экран предупреждения");
            } catch (Exception e) {
                AppLog.line(context, "TPMS: экран предупреждения не открыт " + e.getClass().getSimpleName());
            }
        }

        if (AppPrefs.tpmsAlertSound(context) && now - lastSoundAt > 2500L) {
            lastSoundAt = now;
            playTone(context);
        }
    }

    static boolean isSuppressed(Context context) {
        return context != null && AppPrefs.tpmsAlertSuppressed(context, System.currentTimeMillis());
    }

    static void suppressAfterUserClose(Context context) {
        if (context == null) return;
        long now = System.currentTimeMillis();
        AppPrefs.suppressTpmsAlertUntil(context, now + USER_DISMISS_SUPPRESS_MS);
        lastOpenAt = now;
        lastSoundAt = now;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIFICATION_ID);
        AppLog.line(context, "TPMS: предупреждение закрыто, повтор через 5 минут");
    }

    private static TpmsState.Tire firstAlert(TpmsState.Snapshot snapshot) {
        if (snapshot.tires == null) return null;
        for (TpmsState.Tire tire : snapshot.tires) {
            if (tire != null && tire.alert()) return tire;
        }
        return null;
    }

    private static String warningMessage(TpmsState.Snapshot snapshot, TpmsState.Tire tire) {
        int index = -1;
        if (snapshot.tires != null) {
            for (int i = 0; i < snapshot.tires.length; i++) {
                if (snapshot.tires[i] == tire) {
                    index = i;
                    break;
                }
            }
        }
        String[] labels = {"Переднее левое", "Переднее правое", "Заднее левое", "Заднее правое"};
        String label = index >= 0 && index < labels.length ? labels[index] : tire.label;
        return label + ": " + tire.warningText().toLowerCase(java.util.Locale.ROOT);
    }

    private static void showNotification(Context context, String message) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "TPMS warnings", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Предупреждения давления в шинах");
            nm.createNotificationChannel(channel);
        }
        PendingIntent open = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, TpmsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL)
                : new Notification.Builder(context);
        Notification notification = builder
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("TPMS предупреждение")
                .setContentText(message)
                .setContentIntent(open)
                .setPriority(Notification.PRIORITY_LOW)
                .setAutoCancel(false)
                .setOngoing(false)
                .build();
        try {
            nm.notify(NOTIFICATION_ID, notification);
        } catch (SecurityException e) {
            AppLog.line(context, "TPMS: уведомление заблокировано разрешением Android");
        }
    }

    private static void playTone(Context context) {
        playToneOnStream(AudioManager.STREAM_ALARM, 100, 0L);
        playToneOnStream(AudioManager.STREAM_MUSIC, 95, 140L);
    }

    private static void playToneOnStream(int stream, int volume, long delayMs) {
        HANDLER.postDelayed(() -> {
            try {
                ToneGenerator tone = new ToneGenerator(stream, volume);
                tone.startTone(ToneGenerator.TONE_SUP_ERROR, 360);
                HANDLER.postDelayed(() -> tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 260), 420);
                HANDLER.postDelayed(() -> tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 260), 760);
                HANDLER.postDelayed(tone::release, 1150);
            } catch (Exception ignored) {
            }
        }, delayMs);
    }
}
