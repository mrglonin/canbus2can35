package kia.app;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NavInfoMonitor {
    private static final Pattern DISTANCE = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(км|km|м|m)\\b", Pattern.CASE_INSENSITIVE);

    private NavInfoMonitor() {
    }

    static boolean reportNotification(Context context, StatusBarNotification sbn) {
        if (context == null || sbn == null || sbn.getNotification() == null) return false;
        String pkg = sbn.getPackageName();
        String label = label(context, pkg);
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
        if (isTeyesNavSource(pkg, label)) {
            NavDebugState.teyes(context, value);
            return true;
        }
        if (!isNavigationApp(pkg, label)) return false;
        return reportNavigationNotification(context, pkg, label, title, line, sub, big, ticker, value);
    }

    private static boolean isTeyesNavSource(String pkg, String label) {
        String probe = ((pkg == null ? "" : pkg) + " " + (label == null ? "" : label)).toLowerCase(Locale.US);
        return probe.contains("teyesnavinfo")
                || probe.contains("navinfo")
                || (probe.contains("teyes") && (probe.contains("navi") || probe.contains("nav")));
    }

    private static boolean isNavigationApp(String pkg, String label) {
        String probe = ((pkg == null ? "" : pkg) + " " + (label == null ? "" : label)).toLowerCase(Locale.US);
        return probe.contains("yandexnavi")
                || probe.contains("yandex.maps")
                || probe.contains("dublgis")
                || probe.contains("2gis")
                || probe.contains("google.android.apps.maps")
                || probe.contains("waze");
    }

    private static boolean reportNavigationNotification(Context context, String pkg, String label,
                                                        String title, String line, String sub, String big,
                                                        String ticker, String debug) {
        String combined = join(" ", title, line, sub, big, ticker);
        if (TextUtils.isEmpty(combined)) return false;
        Intent nav = new Intent(NavProtocol.ACTION_TEYES_NAV_INFO);
        nav.putExtra("app", pkg);
        String lower = combined.toLowerCase(Locale.US);
        if (looksFailedOrPreview(lower)) {
            nav.putExtra("state", lower.contains("preview") || lower.contains("search") ? "route preview" : "route failed");
            NavProtocol.handleTeyesNavInfo(context, nav);
            NavDebugState.teyes(context, debug);
            return true;
        }
        Matcher distance = DISTANCE.matcher(combined);
        boolean hasDistance = distance.find();
        boolean hasDirection = looksDirection(lower);
        if (!hasDistance && !hasDirection) {
            NavDebugState.teyes(context, debug);
            return false;
        }
        nav.putExtra("state", "open");
        nav.putExtra("direction", firstText(title, line, label));
        nav.putExtra("position", firstText(sub, extractStreet(line), extractStreet(big)));
        nav.putExtra("describe", firstText(big, line));
        if (hasDistance) {
            nav.putExtra("distance_val_str", distance.group(1));
            nav.putExtra("distance_unit", distance.group(2));
        }
        nav.putExtra("direction_lr", directionSide(lower));
        NavProtocol.handleTeyesNavInfo(context, nav);
        NavDebugState.teyes(context, debug);
        return true;
    }

    private static boolean looksFailedOrPreview(String value) {
        return value.contains("preview") || value.contains("search") || value.contains("failed")
                || value.contains("error") || value.contains("ошиб") || value.contains("поиск")
                || value.contains("маршрут не") || value.contains("route failed");
    }

    private static boolean looksDirection(String value) {
        return value.contains("turn") || value.contains("right") || value.contains("left")
                || value.contains("exit") || value.contains("round") || value.contains("straight")
                || value.contains("повер") || value.contains("направ") || value.contains("налев")
                || value.contains("съезд") || value.contains("круг") || value.contains("прям");
    }

    private static int directionSide(String value) {
        if (value.contains("right") || value.contains("направ")) return 2;
        if (value.contains("left") || value.contains("налев")) return 1;
        return 0;
    }

    private static String extractStreet(String value) {
        if (TextUtils.isEmpty(value)) return null;
        String clean = value.replace('\n', ' ').replace('\r', ' ').trim();
        int idx = clean.indexOf(" на ");
        if (idx >= 0 && idx + 4 < clean.length()) return clean.substring(idx + 4).trim();
        idx = clean.indexOf(" onto ");
        if (idx >= 0 && idx + 6 < clean.length()) return clean.substring(idx + 6).trim();
        return null;
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

    private static String firstText(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) return value.trim();
        }
        return null;
    }

    private static String join(String separator, String... values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (TextUtils.isEmpty(value)) continue;
                String clean = value.trim();
                if (clean.length() == 0 || out.indexOf(clean) >= 0) continue;
                if (out.length() > 0) out.append(separator);
                out.append(clean);
            }
        }
        return out.toString();
    }
}
