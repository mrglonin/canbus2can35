package kia.app;

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
    private static long lastSoundAt;

    private TpmsAlertManager() {
    }

    static void onTpmsUpdate(Context context, TpmsState.Snapshot snapshot) {
        if (context == null || snapshot == null || !AppPrefs.tpmsEnabled(context)) return;
        TpmsState.Tire tire = firstAlert(snapshot);
        if (tire == null) {
            clearAlert(context);
            return;
        }

        long now = System.currentTimeMillis();
        if (AppPrefs.tpmsAlertSuppressed(context, now)) return;

        String message = warningMessage(snapshot, tire);
        showNotification(context, message);
        if (AppPrefs.tpmsAutoOpen(context)) {
            AppService.start(context);
            AppService.refreshOverlays(context);
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
        lastSoundAt = now;
        cancelNotification(context);
        AppService.refreshOverlays(context);
        AppLog.line(context, "TPMS: предупреждение закрыто, повтор через 5 минут");
    }

    static String alertMessage(TpmsState.Snapshot snapshot) {
        TpmsState.Tire tire = firstAlert(snapshot);
        return tire == null ? "" : warningMessage(snapshot, tire);
    }

    private static void clearAlert(Context context) {
        cancelNotification(context);
        AppService.refreshOverlays(context);
    }

    private static void cancelNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIFICATION_ID);
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
        playToneOnStream(AudioManager.STREAM_ALARM, ToneGenerator.TONE_SUP_ERROR, 100, 360, 0L);
        playToneOnStream(AudioManager.STREAM_MUSIC, ToneGenerator.TONE_PROP_BEEP2, 95, 220, 430L);
        playToneOnStream(AudioManager.STREAM_MUSIC, ToneGenerator.TONE_PROP_BEEP2, 95, 220, 760L);
    }

    private static void playToneOnStream(int stream, int toneId, int volume, int durationMs, long delayMs) {
        HANDLER.postDelayed(() -> {
            try {
                ToneGenerator tone = new ToneGenerator(stream, volume);
                tone.startTone(toneId, durationMs);
                HANDLER.postDelayed(tone::release, durationMs + 100L);
            } catch (Exception ignored) {
            }
        }, delayMs);
    }
}
