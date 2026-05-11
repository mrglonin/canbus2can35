package com.sorento.navi;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class AppLog {
    static final String ACTION_STATE = "com.sorento.navi.CLEAN_STATE";
    static final String EXTRA_LOG = "log";
    static final String EXTRA_USB = "usb";
    static final String EXTRA_MEDIA = "media";
    static final String EXTRA_NAV = "nav";

    private static final StringBuilder LOG = new StringBuilder();
    private static String usb = "USB: поиск";
    private static String media = "Мультимедиа: - / - / - / --:--";
    private static String nav = "Навигация: ожидание данных";
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private AppLog() {
    }

    static synchronized void line(Context context, String value) {
        if (TextUtils.isEmpty(value)) return;
        String row = "[" + TIME.format(new Date()) + "] " + value;
        Log.i("KiaCanbus", row);
        LOG.append(row).append('\n');
        int extra = LOG.length() - 7000;
        if (extra > 0) LOG.delete(0, extra);
        broadcast(context);
    }

    static synchronized String text() {
        return LOG.toString();
    }

    static synchronized String usb() {
        return usb;
    }

    static synchronized String media() {
        return media;
    }

    static synchronized String nav() {
        return nav;
    }

    static synchronized void setUsb(Context context, String value) {
        usb = value;
        if (TextUtils.isEmpty(value)) {
            broadcast(context);
        } else {
            line(context, value);
        }
    }

    static synchronized void setMedia(Context context, String value) {
        media = value;
        broadcast(context);
    }

    static synchronized void setNav(Context context, String value) {
        nav = value;
        line(context, value);
    }

    static synchronized void broadcast(Context context) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_LOG, LOG.toString());
        intent.putExtra(EXTRA_USB, usb);
        intent.putExtra(EXTRA_MEDIA, media);
        intent.putExtra(EXTRA_NAV, nav);
        context.sendBroadcast(intent);
    }
}
