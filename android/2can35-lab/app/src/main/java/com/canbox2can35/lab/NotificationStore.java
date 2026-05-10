package com.canbox2can35.lab;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NotificationStore {
    private static final Map<String, JSONObject> items = new LinkedHashMap<>();

    private NotificationStore() {
    }

    public static synchronized void update(StatusBarNotification sbn) {
        try {
            Notification n = sbn.getNotification();
            Bundle extras = n.extras;
            JSONObject item = new JSONObject();
            item.put("package", sbn.getPackageName());
            item.put("key", sbn.getKey());
            item.put("title", text(extras.getCharSequence(Notification.EXTRA_TITLE)));
            item.put("text", text(extras.getCharSequence(Notification.EXTRA_TEXT)));
            item.put("sub_text", text(extras.getCharSequence(Notification.EXTRA_SUB_TEXT)));
            item.put("big_text", text(extras.getCharSequence(Notification.EXTRA_BIG_TEXT)));
            item.put("category", n.category == null ? "" : n.category);
            item.put("when", n.when);
            items.put(sbn.getKey(), item);
            while (items.size() > 80) {
                String first = items.keySet().iterator().next();
                items.remove(first);
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized void remove(StatusBarNotification sbn) {
        items.remove(sbn.getKey());
    }

    public static synchronized JSONArray snapshot() {
        JSONArray out = new JSONArray();
        for (JSONObject item : items.values()) out.put(item);
        return out;
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
