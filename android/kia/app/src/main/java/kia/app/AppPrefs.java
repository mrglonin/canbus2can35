package kia.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

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
    private static final String KEY_MEDIA_TEXT_FORMAT = "media_text_format";
    private static final String KEY_BACKGROUND_ANIMATION = "background_animation";
    private static final String KEY_SAS_RATIO = "sas_ratio";
    private static final String KEY_OBD_ENABLED = "obd_enabled";
    private static final String KEY_AMP_ENABLED = "amp_enabled";
    private static final String KEY_TPMS_ENABLED = "tpms_enabled";
    private static final String KEY_OBD_EMULATION = "obd_emulation";
    private static final String KEY_TPMS_LOW_BAR_X10 = "tpms_low_bar_x10";
    private static final String KEY_TPMS_HIGH_BAR_X10 = "tpms_high_bar_x10";
    private static final String KEY_TPMS_AUTO_OPEN = "tpms_auto_open";
    private static final String KEY_TPMS_ALERT_OVERLAY = "tpms_alert_overlay";
    private static final String KEY_TPMS_ALERT_SOUND = "tpms_alert_sound";
    private static final String KEY_TPMS_ALERT_SUPPRESSED_UNTIL = "tpms_alert_suppressed_until";
    private static final String KEY_HOME_WIDGET_SPEED = "home_widget_speed";
    private static final String KEY_HOME_WIDGET_RPM = "home_widget_rpm";
    private static final String KEY_HOME_WIDGET_NAV = "home_widget_nav";
    private static final String KEY_HOME_WIDGET_MUSIC = "home_widget_music";
    private static final String KEY_HOME_WIDGET_ENGINE_TEMP = "home_widget_engine_temp";
    private static final String KEY_HOME_WIDGET_CABIN_TEMP = "home_widget_cabin_temp";
    private static final String KEY_HOME_WIDGET_CLIMATE_TEMP = "home_widget_climate_temp";
    private static final String KEY_HOME_WIDGET_VOLTAGE = "home_widget_voltage";
    private static final String KEY_HOME_WIDGET_TRIP = "home_widget_trip";
    private static final String KEY_HOME_WIDGET_ORDER = "home_widget_order";
    private static final String KEY_DEBUG_CAN = "debug_can";
    private static final String KEY_DEBUG_UART = "debug_uart";
    private static final String KEY_UART_OVERLAY = "uart_overlay";
    private static final String KEY_LOG_OVERLAY = "log_overlay";
    private static final String KEY_CAN_LOG_MODE = "can_log_mode";
    private static final String KEY_BLIND_SPOT_ENABLED = "blind_spot_enabled";
    private static final String KEY_BLIND_SPOT_OVERLAY = "blind_spot_overlay";
    private static final String KEY_UPDATE_CHECK_ON_LAUNCH = "update_check_on_launch";
    private static final String KEY_DEFAULT_PROFILE_VERSION = "default_profile_version";

    private static final int DEFAULT_PROFILE_VERSION = 4;

    static final String HOME_WIDGET_SPEED = "speed";
    static final String HOME_WIDGET_RPM = "rpm";
    static final String HOME_WIDGET_NAV = "nav";
    static final String HOME_WIDGET_MUSIC = "music";
    static final String HOME_WIDGET_ENGINE_TEMP = "engine";
    static final String HOME_WIDGET_OUTSIDE_TEMP = "outside";
    static final String HOME_WIDGET_CLIMATE_TEMP = "climate";
    static final String HOME_WIDGET_VOLTAGE = "voltage";
    static final String HOME_WIDGET_TRIP = "trip";

    private static final String[] DEFAULT_HOME_WIDGET_ORDER = new String[]{
            HOME_WIDGET_SPEED,
            HOME_WIDGET_RPM,
            HOME_WIDGET_NAV,
            HOME_WIDGET_MUSIC,
            HOME_WIDGET_ENGINE_TEMP,
            HOME_WIDGET_OUTSIDE_TEMP,
            HOME_WIDGET_CLIMATE_TEMP,
            HOME_WIDGET_VOLTAGE,
            HOME_WIDGET_TRIP
    };

    private AppPrefs() {
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    static void applyDefaultProfileIfNeeded(Context context) {
        SharedPreferences prefs = prefs(context);
        if (prefs.getInt(KEY_DEFAULT_PROFILE_VERSION, 0) >= DEFAULT_PROFILE_VERSION) return;
        prefs.edit()
                .putBoolean(KEY_AUTO_START, true)
                .putBoolean(KEY_BACKGROUND_AUTO_START, true)
                .putBoolean(KEY_AUTO_HIDE, false)
                .putInt(KEY_AUTO_HIDE_DELAY, 1)
                .putInt(KEY_NAV_TEXT_MODE, 0)
                .putBoolean(KEY_NAV_TBT, false)
                .remove(KEY_NAV_OVERLAY)
                .putBoolean(KEY_NAV_COMPASS, true)
                .putInt(KEY_SPEED_UNIT, 0)
                .putInt(KEY_TEMP_UNIT, 0)
                .putBoolean(KEY_ENGINE_TEMP_ENABLED, true)
                .putBoolean(KEY_MEDIA_ACCESS_PROMPT, true)
                .remove(KEY_MEDIA_DEBUG)
                .remove(KEY_MEDIA_SCAN_ALL)
                .remove(KEY_MEDIA_OVERLAY)
                .putInt(KEY_MEDIA_TEXT_FORMAT, 2)
                .putBoolean(KEY_BACKGROUND_ANIMATION, false)
                .putBoolean(KEY_OBD_ENABLED, true)
                .putBoolean(KEY_AMP_ENABLED, true)
                .putBoolean(KEY_TPMS_ENABLED, true)
                .putBoolean(KEY_OBD_EMULATION, false)
                .putBoolean(KEY_TPMS_AUTO_OPEN, false)
                .putBoolean(KEY_TPMS_ALERT_OVERLAY, true)
                .putBoolean(KEY_TPMS_ALERT_SOUND, true)
                .putLong(KEY_TPMS_ALERT_SUPPRESSED_UNTIL, 0L)
                .putBoolean(KEY_DEBUG, false)
                .putBoolean(KEY_DEBUG_CAN, false)
                .putBoolean(KEY_DEBUG_UART, false)
                .putBoolean(KEY_UART_OVERLAY, false)
                .putBoolean(KEY_LOG_OVERLAY, false)
                .putInt(KEY_CAN_LOG_MODE, 2)
                .putBoolean(KEY_BLIND_SPOT_ENABLED, true)
                .putBoolean(KEY_BLIND_SPOT_OVERLAY, true)
                .putBoolean(KEY_UPDATE_CHECK_ON_LAUNCH, true)
                .putInt(KEY_DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_VERSION)
                .apply();
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
        return clamp(prefs(context).getInt(KEY_NAV_TEXT_MODE, 0), 0, 2);
    }

    static void setNavTextMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_NAV_TEXT_MODE, clamp(mode, 0, 2)).apply();
    }

    static boolean navTbt(Context context) {
        return prefs(context).getBoolean(KEY_NAV_TBT, false);
    }

    static void setNavTbt(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_NAV_TBT, value).apply();
    }

    static boolean navOverlay(Context context) {
        return prefs(context).getBoolean(KEY_NAV_OVERLAY, false);
    }

    static void setNavOverlay(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_NAV_OVERLAY, value).apply();
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
        return prefs(context).getBoolean(KEY_MEDIA_DEBUG, false);
    }

    static void setMediaDebug(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_MEDIA_DEBUG, value).apply();
    }

    static boolean mediaScanAll(Context context) {
        return prefs(context).getBoolean(KEY_MEDIA_SCAN_ALL, false);
    }

    static void setMediaScanAll(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_MEDIA_SCAN_ALL, value).apply();
    }

    static boolean mediaOverlay(Context context) {
        return prefs(context).getBoolean(KEY_MEDIA_OVERLAY, false);
    }

    static void setMediaOverlay(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_MEDIA_OVERLAY, value).apply();
    }

    static int mediaTextFormat(Context context) {
        return clamp(prefs(context).getInt(KEY_MEDIA_TEXT_FORMAT, 2), 0, 4);
    }

    static void setMediaTextFormat(Context context, int value) {
        prefs(context).edit().putInt(KEY_MEDIA_TEXT_FORMAT, clamp(value, 0, 4)).apply();
    }

    static String mediaTextFormatLabel(Context context) {
        switch (mediaTextFormat(context)) {
            case 0:
                return "Трек";
            case 1:
                return "Автор - трек";
            case 2:
                return "Автор - трек - время";
            case 3:
                return "Источник - автор - трек - время";
            case 4:
                return "Источник - трек";
            default:
                return "Автор - трек - время";
        }
    }

    static boolean backgroundAnimation(Context context) {
        return prefs(context).getBoolean(KEY_BACKGROUND_ANIMATION, false);
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

    static boolean ampEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AMP_ENABLED, true);
    }

    static void setAmpEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_AMP_ENABLED, value).apply();
    }

    static boolean tpmsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_TPMS_ENABLED, true);
    }

    static void setTpmsEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_TPMS_ENABLED, value).apply();
    }

    static boolean homeWidgetSpeed(Context context) {
        return prefs(context).getBoolean(KEY_HOME_WIDGET_SPEED, true);
    }

    static void setHomeWidgetSpeed(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_HOME_WIDGET_SPEED, value).apply();
    }

    static boolean homeWidgetRpm(Context context) {
        return prefs(context).getBoolean(KEY_HOME_WIDGET_RPM, true);
    }

    static void setHomeWidgetRpm(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_HOME_WIDGET_RPM, value).apply();
    }

    static boolean homeWidgetNav(Context context) {
        return prefs(context).getBoolean(KEY_HOME_WIDGET_NAV, true);
    }

    static void setHomeWidgetNav(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_HOME_WIDGET_NAV, value).apply();
    }

    static boolean homeWidgetMusic(Context context) {
        return prefs(context).getBoolean(KEY_HOME_WIDGET_MUSIC, true);
    }

    static void setHomeWidgetMusic(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_HOME_WIDGET_MUSIC, value).apply();
    }

    static boolean homeWidgetEngineTemp(Context context) {
        return prefs(context).getBoolean(KEY_HOME_WIDGET_ENGINE_TEMP, true);
    }

    static void setHomeWidgetEngineTemp(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_HOME_WIDGET_ENGINE_TEMP, value).apply();
    }

    static boolean homeWidgetCabinTemp(Context context) {
        return prefs(context).getBoolean(KEY_HOME_WIDGET_CABIN_TEMP, true);
    }

    static void setHomeWidgetCabinTemp(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_HOME_WIDGET_CABIN_TEMP, value).apply();
    }

    static boolean homeWidgetClimateTemp(Context context) {
        return prefs(context).getBoolean(KEY_HOME_WIDGET_CLIMATE_TEMP, true);
    }

    static void setHomeWidgetClimateTemp(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_HOME_WIDGET_CLIMATE_TEMP, value).apply();
    }

    static boolean homeWidgetVoltage(Context context) {
        return prefs(context).getBoolean(KEY_HOME_WIDGET_VOLTAGE, true);
    }

    static void setHomeWidgetVoltage(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_HOME_WIDGET_VOLTAGE, value).apply();
    }

    static boolean homeWidgetTrip(Context context) {
        return prefs(context).getBoolean(KEY_HOME_WIDGET_TRIP, true);
    }

    static void setHomeWidgetTrip(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_HOME_WIDGET_TRIP, value).apply();
    }

    static String[] homeWidgetOrder(Context context) {
        ArrayList<String> out = new ArrayList<>();
        String raw = prefs(context).getString(KEY_HOME_WIDGET_ORDER, "");
        if (raw != null && raw.length() > 0) {
            String[] parts = raw.split(",");
            for (String part : parts) {
                String id = part == null ? "" : part.trim();
                if (validHomeWidgetId(id) && !contains(out, id)) out.add(id);
            }
        }
        for (String id : DEFAULT_HOME_WIDGET_ORDER) {
            if (!contains(out, id)) out.add(id);
        }
        return out.toArray(new String[0]);
    }

    static void moveHomeWidget(Context context, String id, int delta) {
        if (!validHomeWidgetId(id) || delta == 0) return;
        String[] order = homeWidgetOrder(context);
        int index = -1;
        for (int i = 0; i < order.length; i++) {
            if (id.equals(order[i])) {
                index = i;
                break;
            }
        }
        if (index < 0) return;
        int next = clamp(index + delta, 0, order.length - 1);
        if (next == index) return;
        String swap = order[next];
        order[next] = order[index];
        order[index] = swap;
        saveHomeWidgetOrder(context, order);
    }

    static int homeWidgetOrderSignature(Context context) {
        int signature = 17;
        for (String id : homeWidgetOrder(context)) {
            signature = signature * 31 + id.hashCode();
        }
        return signature;
    }

    static String homeWidgetLabel(String id) {
        if (HOME_WIDGET_SPEED.equals(id)) return "Скорость";
        if (HOME_WIDGET_RPM.equals(id)) return "Обороты";
        if (HOME_WIDGET_NAV.equals(id)) return "Навигация";
        if (HOME_WIDGET_MUSIC.equals(id)) return "Музыка";
        if (HOME_WIDGET_ENGINE_TEMP.equals(id)) return "Температура двигателя";
        if (HOME_WIDGET_OUTSIDE_TEMP.equals(id)) return "Улица: наружная температура";
        if (HOME_WIDGET_CLIMATE_TEMP.equals(id)) return "Климат: заданная температура";
        if (HOME_WIDGET_VOLTAGE.equals(id)) return "Вольтаж";
        if (HOME_WIDGET_TRIP.equals(id)) return "Поездка: дистанция и время";
        return id;
    }

    static boolean homeWidgetEnabled(Context context, String id) {
        if (HOME_WIDGET_SPEED.equals(id)) return homeWidgetSpeed(context);
        if (HOME_WIDGET_RPM.equals(id)) return homeWidgetRpm(context);
        if (HOME_WIDGET_NAV.equals(id)) return homeWidgetNav(context);
        if (HOME_WIDGET_MUSIC.equals(id)) return homeWidgetMusic(context);
        if (HOME_WIDGET_ENGINE_TEMP.equals(id)) return homeWidgetEngineTemp(context);
        if (HOME_WIDGET_OUTSIDE_TEMP.equals(id)) return homeWidgetCabinTemp(context);
        if (HOME_WIDGET_CLIMATE_TEMP.equals(id)) return homeWidgetClimateTemp(context);
        if (HOME_WIDGET_VOLTAGE.equals(id)) return homeWidgetVoltage(context);
        if (HOME_WIDGET_TRIP.equals(id)) return homeWidgetTrip(context);
        return false;
    }

    static void setHomeWidgetEnabled(Context context, String id, boolean value) {
        if (HOME_WIDGET_SPEED.equals(id)) setHomeWidgetSpeed(context, value);
        else if (HOME_WIDGET_RPM.equals(id)) setHomeWidgetRpm(context, value);
        else if (HOME_WIDGET_NAV.equals(id)) setHomeWidgetNav(context, value);
        else if (HOME_WIDGET_MUSIC.equals(id)) setHomeWidgetMusic(context, value);
        else if (HOME_WIDGET_ENGINE_TEMP.equals(id)) setHomeWidgetEngineTemp(context, value);
        else if (HOME_WIDGET_OUTSIDE_TEMP.equals(id)) setHomeWidgetCabinTemp(context, value);
        else if (HOME_WIDGET_CLIMATE_TEMP.equals(id)) setHomeWidgetClimateTemp(context, value);
        else if (HOME_WIDGET_VOLTAGE.equals(id)) setHomeWidgetVoltage(context, value);
        else if (HOME_WIDGET_TRIP.equals(id)) setHomeWidgetTrip(context, value);
    }

    private static void saveHomeWidgetOrder(Context context, String[] order) {
        StringBuilder out = new StringBuilder();
        if (order != null) {
            for (String id : order) {
                if (!validHomeWidgetId(id)) continue;
                if (out.length() > 0) out.append(',');
                out.append(id);
            }
        }
        prefs(context).edit().putString(KEY_HOME_WIDGET_ORDER, out.toString()).apply();
    }

    private static boolean validHomeWidgetId(String id) {
        if (id == null) return false;
        for (String known : DEFAULT_HOME_WIDGET_ORDER) {
            if (known.equals(id)) return true;
        }
        return false;
    }

    private static boolean contains(ArrayList<String> values, String id) {
        for (String value : values) {
            if (value.equals(id)) return true;
        }
        return false;
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

    static boolean tpmsAlertOverlay(Context context) {
        SharedPreferences prefs = prefs(context);
        return prefs.getBoolean(KEY_TPMS_ALERT_OVERLAY, prefs.getBoolean(KEY_TPMS_AUTO_OPEN, true));
    }

    static void setTpmsAlertOverlay(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_TPMS_ALERT_OVERLAY, value).apply();
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

    static boolean logOverlay(Context context) {
        return prefs(context).getBoolean(KEY_LOG_OVERLAY, false);
    }

    static void setLogOverlay(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_LOG_OVERLAY, value).apply();
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
