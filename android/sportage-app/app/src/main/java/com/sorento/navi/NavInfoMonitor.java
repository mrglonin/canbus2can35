package com.sorento.navi;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.util.Locale;

final class NavInfoMonitor {
    private NavInfoMonitor() {
    }

    static boolean reportNotification(Context context, StatusBarNotification sbn) {
        if (context == null || sbn == null || sbn.getNotification() == null) return false;
        String pkg = sbn.getPackageName();
        String label = label(context, pkg);
        if (!isTeyesNavSource(pkg, label)) return false;
        Notification n = sbn.getNotification();
        Bundle e = n.extras;
        String title = text(e, "android.title");
        String line = text(e, "android.text");
        String sub = text(e, "android.subText");
        String big = text(e, "android.bigText");
        String ticker = n.tickerText == null ? "" : n.tickerText.toString();
        String value = label + " (" + pkg + ")"
                + " title=" + dash(title)
                + " text=" + dash(line)
                + " sub=" + dash(sub)
                + " big=" + dash(big)
                + " ticker=" + dash(ticker);
        NavDebugState.teyes(context, value);
        return true;
    }

    private static boolean isTeyesNavSource(String pkg, String label) {
        String probe = ((pkg == null ? "" : pkg) + " " + (label == null ? "" : label)).toLowerCase(Locale.US);
        return probe.contains("teyesnavinfo")
                || probe.contains("navinfo")
                || (probe.contains("teyes") && (probe.contains("navi") || probe.contains("nav")));
    }

    private static String text(Bundle b, String key) {
        if (b == null) return null;
        CharSequence cs = b.getCharSequence(key);
        return cs == null ? null : cs.toString();
    }

    private static String label(Context context, String pkg) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(info);
            if (!TextUtils.isEmpty(label)) return label.toString();
        } catch (Exception ignored) {
        }
        return pkg;
    }

    private static String dash(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }
}
