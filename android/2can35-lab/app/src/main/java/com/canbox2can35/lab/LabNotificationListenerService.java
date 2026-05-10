package com.canbox2can35.lab;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class LabNotificationListenerService extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        NotificationStore.update(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        NotificationStore.remove(sbn);
    }
}
