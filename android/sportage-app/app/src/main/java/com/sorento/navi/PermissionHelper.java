package com.sorento.navi;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.concurrent.TimeUnit;

final class PermissionHelper {
    static final int REQ_RUNTIME = 3001;

    private PermissionHelper() {
    }

    static String[] runtimePermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[]{
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_AUDIO
            };
        }
        return new String[0];
    }

    static boolean shouldRequestRuntime(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= 33
                && Manifest.permission.READ_MEDIA_AUDIO.equals(permission)
                && context != null
                && !AppPrefs.mediaAccessPrompt(context)) {
            return false;
        }
        return true;
    }

    static boolean hasNotificationAccess(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners"
        );
        if (enabled == null) return false;
        String pkg = context.getPackageName();
        String[] parts = enabled.split(":");
        for (String part : parts) {
            ComponentName cn = ComponentName.unflattenFromString(part);
            if (cn != null && TextUtils.equals(pkg, cn.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    static void openNotificationListener(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.putExtra("android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME",
                    new ComponentName(activity, MediaNotificationListener.class).flattenToString());
            activity.startActivity(intent);
        } catch (Exception e) {
            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    static boolean canDrawOverlays(Context context) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context);
    }

    static void openOverlaySettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        } catch (Exception e) {
            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    static void openDeveloperSettings(Activity activity) {
        try {
            activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception e) {
            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    static boolean activateDeveloperOptionsAndAdb(Context context) {
        if (context == null) return false;
        if (enableAdbThroughSettings(context)) {
            AppLog.line(context, "ADB: включён через системные настройки");
            return true;
        }
        if (enableAdbThroughRoot(context)) {
            AppLog.line(context, "ADB: включён через root");
            return true;
        }
        AppLog.line(context, "ADB: нет прав для автоматического включения, открыт экран разработчика");
        return false;
    }

    private static boolean enableAdbThroughSettings(Context context) {
        try {
            Settings.Global.putInt(context.getContentResolver(), "development_settings_enabled", 1);
            Settings.Global.putInt(context.getContentResolver(), "adb_enabled", 1);
            return Settings.Global.getInt(context.getContentResolver(), "adb_enabled", 0) == 1;
        } catch (SecurityException e) {
            return false;
        } catch (Exception e) {
            AppLog.line(context, "ADB: ошибка системной настройки " + e.getClass().getSimpleName());
            return false;
        }
    }

    private static boolean enableAdbThroughRoot(Context context) {
        Process process = null;
        try {
            String command = "settings put global development_settings_enabled 1; "
                    + "settings put global adb_enabled 1; "
                    + "setprop ctl.restart adbd";
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            boolean done = process.waitFor(2, TimeUnit.SECONDS);
            if (!done) {
                process.destroy();
                return false;
            }
            if (process.exitValue() != 0) return false;
            return Settings.Global.getInt(context.getContentResolver(), "adb_enabled", 0) == 1;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }

    static void openBatterySettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        } catch (Exception e) {
            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}
