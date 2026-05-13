package kia.app;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

final class PermissionHelper {
    static final int REQ_RUNTIME = 3001;

    private PermissionHelper() {
    }

    static String[] runtimePermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[]{
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
        if (Build.VERSION.SDK_INT >= 23) {
            return new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
        return new String[0];
    }

    static boolean shouldRequestRuntime(Context context, String permission) {
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
