package com.sorento.navi;

import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class MediaNotificationListener extends NotificationListenerService {
    private static MediaNotificationListener current;

    static boolean refresh(Context context) {
        MediaNotificationListener listener = current;
        if (listener == null) return false;
        try {
            StatusBarNotification[] notifications = listener.getActiveNotifications();
            if (notifications == null) return false;
            StatusBarNotification best = null;
            int bestScore = 0;
            long bestAt = Long.MIN_VALUE;
            for (StatusBarNotification notification : notifications) {
                NavInfoMonitor.reportNotification(listener, notification);
                int score = MediaMonitor.notificationScore(listener, notification);
                if (score <= 0) continue;
                long postTime = notification.getPostTime();
                if (score > bestScore || (score == bestScore && postTime >= bestAt)) {
                    bestScore = score;
                    bestAt = postTime;
                    best = notification;
                }
            }
            return best != null && MediaMonitor.reportNotification(listener, best);
        } catch (Exception e) {
            AppLog.line(context, "Уведомления: ошибка обновления " + e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        current = this;
        AppLog.line(this, "Уведомления: доступ подключён");
        MediaMonitor.start(this);
        refresh(this);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        current = null;
        AppLog.line(this, "Уведомления: доступ отключён");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        NavInfoMonitor.reportNotification(this, sbn);
        MediaMonitor.reportNotification(this, sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        MediaMonitor.scanNow(this);
    }
}
