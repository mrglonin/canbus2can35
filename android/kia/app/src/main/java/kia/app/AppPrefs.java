package kia.app;

import android.content.Context;
import android.content.SharedPreferences;

final class AppPrefs {
    private static final String NAME = "Kia";
    private static final String KEY_DEBUG = "debug";
    private static final String KEY_AUTO_START = "auto_start";
    private static final String KEY_BACKGROUND_AUTO_START = "background_auto_start";
    private static final String KEY_AUTO_HIDE = "auto_hide";
    private static final String KEY_AUTO_HIDE_DELAY = "auto_hide_delay";
    private static final String KEY_NAV_TEXT_MODE = "nav_text_mode";
    private static final String KEY_NAV_TBT = "nav_tbt";
    private static final String KEY_NAV_OVERLAY = "nav_overlay";
    private static final String KEY_NAV_COMPASS = "nav_compass";
    private static final String KEY_SPEED_UNIT = "speed_unit";
    private static final String KEY_TEMP_UNIT = "temp_unit";
    private static final String KEY_ENGINE_TEMP_ENABLED = "engine_temp_enabled";
    private static final String KEY_MEDIA_ACCESS_PROMPT = "media_access_prompt";
    private static final String KEY_MEDIA_DEBUG = "media_debug";
    private static final String KEY_MEDIA_SCAN_ALL = "media_scan_all";
    private static final String KEY_MEDIA_OVERLAY = "media_overlay";
    private static final String KEY_BACKGROUND_ANIMATION = "background_animation";
    private static final String KEY_SAS_RATIO = "sas_ratio";
    private static final String KEY_OBD_ENABLED = "obd_enabled";
    private static final String KEY_TPMS_ENABLED = "tpms_enabled";
    private static final String KEY_OBD_EMULATION = "obd_emulation";
    private static final String KEY_TPMS_LOW_BAR_X10 = "tpms_low_bar_x10";
    private static final String KEY_TPMS_HIGH_BAR_X10 = "tpms_high_bar_x10";
    private static final String KEY_TPMS_AUTO_OPEN = "tpms_auto_open";
    private static final String KEY_TPMS_ALERT_SOUND = "tpms_alert_sound";
    private static final String KEY_TPMS_ALERT_SUPPRESSED_UNTIL = "tpms_alert_suppressed_until";
    private static final String KEY_DEBUG_CAN = "debug_can";
    private static final String KEY_DEBUG_UART = "debug_uart";
    private static final String KEY_UART_OVERLAY = "uart_overlay";
    private static final String KEY_CAN_LOG_MODE = "can_log_mode";
    private static final String KEY_BLIND_SPOT_ENABLED = "blind_spot_enabled";
    private static final String KEY_BLIND_SPOT_OVERLAY = "blind_spot_overlay";
    private static final String KEY_UPDATE_CHECK_ON_LAUNCH = "update_check_on_launch";

    private AppPrefs() {
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    static boolean debug(Context context) {
        return prefs(context).getBoolean(KEY_DEBUG, false);
    }

    static void setDebug(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_DEBUG, value).apply();
    }

    static boolean autoStart(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_START, true);
    }

    static void setAutoStart(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_AUTO_START, value).apply();
    }

    static boolean backgroundAutoStart(Context context) {
        return prefs(context).getBoolean(KEY_BACKGROUND_AUTO_START, false);
    }

    static void setBackgroundAutoStart(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_BACKGROUND_AUTO_START, value).apply();
    }

    static boolean autoHide(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_HIDE, true);
    }

    static void setAutoHide(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_AUTO_HIDE, value).apply();
    }

    static int autoHideDelaySeconds(Context context) {
        return clamp(prefs(context).getInt(KEY_AUTO_HIDE_DELAY, 5), 1, 5);
    }

    static void setAutoHideDelaySeconds(Context context, int value) {
        prefs(context).edit().putInt(KEY_AUTO_HIDE_DELAY, clamp(value, 1, 5)).apply();
    }

    static int navTextMode(Context context) {
        return prefs(context).getInt(KEY_NAV_TEXT_MODE, 0);
    }

    static void setNavTextMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_NAV_TEXT_MODE, mode).apply();
    }

    static boolean navTbt(Context context) {
        return prefs(context).getBoolean(KEY_NAV_TBT, false);
    }

    static void setNavTbt(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_NAV_TBT, value).apply();
    }

    static boolean navOverlay(Context context) {
        return false;
    }

    static void setNavOverlay(Context context, boolean value) {
        prefs(context).edit().remove(KEY_NAV_OVERLAY).apply();
    }

    static boolean navCompass(Context context) {
        return prefs(context).getBoolean(KEY_NAV_COMPASS, true);
    }

    static void setNavCompass(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_NAV_COMPASS, value).apply();
    }

    static int speedUnit(Context context) {
        return prefs(context).getInt(KEY_SPEED_UNIT, 0);
    }

    static void setSpeedUnit(Context context, int value) {
        prefs(context).edit().putInt(KEY_SPEED_UNIT, value == 1 ? 1 : 0).apply();
    }

    static int displaySpeed(Context context, int kmh) {
        if (speedUnit(context) == 1) {
            return Math.round(kmh * 0.621371f);
        }
        return kmh;
    }

    static String speedUnitLabel(Context context) {
        return speedUnit(context) == 1 ? "mph" : "km/h";
    }

    static int tempUnit(Context context) {
        return prefs(context).getInt(KEY_TEMP_UNIT, 0);
    }

    static void setTempUnit(Context context, int value) {
        prefs(context).edit().putInt(KEY_TEMP_UNIT, value == 1 ? 1 : 0).apply();
    }

    static int displayTemp(Context context, int celsius) {
        if (tempUnit(context) == 1) {
            return Math.round(celsius * 9f / 5f + 32f);
        }
        return celsius;
    }

    static String tempText(Context context, int celsius) {
        return displayTemp(context, celsius) + (tempUnit(context) == 1 ? "°F" : "°C");
    }

    static boolean engineTempEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENGINE_TEMP_ENABLED, true);
    }

    static void setEngineTempEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_ENGINE_TEMP_ENABLED, value).apply();
    }

    static boolean mediaAccessPrompt(Context context) {
        return prefs(context).getBoolean(KEY_MEDIA_ACCESS_PROMPT, true);
    }

    static void setMediaAccessPrompt(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_MEDIA_ACCESS_PROMPT, value).apply();
    }

    static boolean mediaDebug(Context context) {
        return false;
    }

    static void setMediaDebug(Context context, boolean value) {
        prefs(context).edit().remove(KEY_MEDIA_DEBUG).apply();
    }

    static boolean mediaScanAll(Context context) {
        return false;
    }

    static void setMediaScanAll(Context context, boolean value) {
        prefs(context).edit().remove(KEY_MEDIA_SCAN_ALL).apply();
    }

    static boolean mediaOverlay(Context context) {
        return false;
    }

    static void setMediaOverlay(Context context, boolean value) {
        prefs(context).edit().remove(KEY_MEDIA_OVERLAY).apply();
    }

    static boolean backgroundAnimation(Context context) {
        return prefs(context).getBoolean(KEY_BACKGROUND_ANIMATION, true);
    }

    static void setBackgroundAnimation(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_BACKGROUND_ANIMATION, value).apply();
    }

    static int sasRatio(Context context) {
        return clamp(prefs(context).getInt(KEY_SAS_RATIO, 10), 10, 50);
    }

    static void setSasRatio(Context context, int value) {
        prefs(context).edit().putInt(KEY_SAS_RATIO, clamp(value, 10, 50)).apply();
    }

    static boolean obdEnabled(Context context) {
        return prefs(context).getBoolean(KEY_OBD_ENABLED, true);
    }

    static void setObdEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_OBD_ENABLED, value).apply();
    }

    static boolean tpmsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_TPMS_ENABLED, true);
    }

    static void setTpmsEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_TPMS_ENABLED, value).apply();
    }

    static boolean obdEmulation(Context context) {
        return prefs(context).getBoolean(KEY_OBD_EMULATION, false);
    }

    static void setObdEmulation(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_OBD_EMULATION, value).apply();
    }

    static float tpmsLowBar(Context context) {
        return prefs(context).getInt(KEY_TPMS_LOW_BAR_X10, 16) / 10f;
    }

    static void setTpmsLowBar(Context context, float value) {
        prefs(context).edit().putInt(KEY_TPMS_LOW_BAR_X10, clamp(Math.round(value * 10f), 8, 30)).apply();
    }

    static float tpmsHighBar(Context context) {
        return prefs(context).getInt(KEY_TPMS_HIGH_BAR_X10, 32) / 10f;
    }

    static void setTpmsHighBar(Context context, float value) {
        prefs(context).edit().putInt(KEY_TPMS_HIGH_BAR_X10, clamp(Math.round(value * 10f), 20, 50)).apply();
    }

    static boolean tpmsAutoOpen(Context context) {
        return prefs(context).getBoolean(KEY_TPMS_AUTO_OPEN, true);
    }

    static void setTpmsAutoOpen(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_TPMS_AUTO_OPEN, value).apply();
    }

    static boolean tpmsAlertSound(Context context) {
        return prefs(context).getBoolean(KEY_TPMS_ALERT_SOUND, true);
    }

    static void setTpmsAlertSound(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_TPMS_ALERT_SOUND, value).apply();
    }

    static long tpmsAlertSuppressedUntil(Context context) {
        return prefs(context).getLong(KEY_TPMS_ALERT_SUPPRESSED_UNTIL, 0L);
    }

    static boolean tpmsAlertSuppressed(Context context, long now) {
        return tpmsAlertSuppressedUntil(context) > now;
    }

    static void suppressTpmsAlertUntil(Context context, long untilMillis) {
        prefs(context).edit().putLong(KEY_TPMS_ALERT_SUPPRESSED_UNTIL, untilMillis).apply();
    }

    static boolean debugCan(Context context) {
        return prefs(context).getBoolean(KEY_DEBUG_CAN, false);
    }

    static void setDebugCan(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_DEBUG_CAN, value).apply();
    }

    static boolean debugUart(Context context) {
        return prefs(context).getBoolean(KEY_DEBUG_UART, false);
    }

    static void setDebugUart(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_DEBUG_UART, value).apply();
    }

    static boolean uartOverlay(Context context) {
        return prefs(context).getBoolean(KEY_UART_OVERLAY, false);
    }

    static void setUartOverlay(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_UART_OVERLAY, value).apply();
    }

    static int canLogMode(Context context) {
        return clamp(prefs(context).getInt(KEY_CAN_LOG_MODE, 2), 0, 2);
    }

    static void setCanLogMode(Context context, int value) {
        prefs(context).edit().putInt(KEY_CAN_LOG_MODE, clamp(value, 0, 2)).apply();
    }

    static boolean blindSpotEnabled(Context context) {
        return prefs(context).getBoolean(KEY_BLIND_SPOT_ENABLED, true);
    }

    static void setBlindSpotEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_BLIND_SPOT_ENABLED, value).apply();
    }

    static boolean blindSpotOverlay(Context context) {
        return prefs(context).getBoolean(KEY_BLIND_SPOT_OVERLAY, true);
    }

    static void setBlindSpotOverlay(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_BLIND_SPOT_OVERLAY, value).apply();
    }

    static boolean updateCheckOnLaunch(Context context) {
        return prefs(context).getBoolean(KEY_UPDATE_CHECK_ON_LAUNCH, true);
    }

    static void setUpdateCheckOnLaunch(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_UPDATE_CHECK_ON_LAUNCH, value).apply();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
