package kia.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Locale;

public class QaScenarioReceiver extends BroadcastReceiver {
    static final String ACTION = "kia.app.QA_SCENARIO";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String scenario = text(intent, "scenario");
        if (scenario.length() == 0) scenario = text(intent, "name");
        scenario = scenario.toLowerCase(Locale.US);
        AppService.start(context);
        QaState.event(context, "scenario=" + scenario);

        if (scenario.startsWith("media_")) {
            media(context, scenario);
        } else if (scenario.startsWith("nav_")) {
            nav(context, scenario);
        } else if (scenario.startsWith("rcta_")) {
            rcta(context, scenario);
        } else if (scenario.startsWith("tpms_")) {
            tpms(context, scenario);
        } else if (scenario.startsWith("vehicle")) {
            vehicle(context);
        } else if (scenario.startsWith("compass")) {
            int step = intent.getIntExtra("step", 9);
            CanbusControl.sendCompassStep(context, step);
            QaState.event(context, "compass step=" + step);
        } else {
            AppLog.line(context, "QA: неизвестный сценарий " + scenario);
        }
    }

    private static void media(Context context, String scenario) {
        if ("media_yandex".equals(scenario)) {
            MediaMonitor.reportExternal(context, "Яндекс Музыка", "ru.yandex.music",
                    "Vara Gianna", "Blood // Water", 225380, 160);
        } else if ("media_bt_selected_paused".equals(scenario)) {
            MediaMonitor.reportSourceHint(context, "Bluetooth", "com.teyes.music.widget", 50, 3000);
            QaState.event(context, "Bluetooth selected hint without playback");
        } else if ("media_bt_playing".equals(scenario)) {
            MediaMonitor.reportExternal(context, "Bluetooth", "com.android.bluetooth",
                    "Vara Gianna", "Blood // Water", 225380, 150);
        } else if ("media_usb".equals(scenario)) {
            MediaMonitor.reportExternal(context, "USB", "com.spd.media",
                    "OneRepublic", "Counting Stars", 232598, 153);
        } else if ("media_fm".equals(scenario)) {
            MediaMonitor.reportExternal(context, "FM радио", "com.spd.radio",
                    "", "Авторадио Казахстан", -1, 130);
        } else if ("media_am".equals(scenario)) {
            MediaMonitor.reportExternal(context, "AM 24", "com.spd.radio",
                    "", "AM 24", -1, 130);
        }
    }

    private static void nav(Context context, String scenario) {
        if ("nav_active".equals(scenario)) {
            Intent on = new Intent("kia.app.ACTION_NAVI_ON_DATA");
            on.putExtra("navi_on", true);
            NavProtocol.handle(context, on);

            Intent maneuver = new Intent("kia.app.ACTION_MANEUVER_DATA");
            maneuver.putExtra("imageId", "context_ra_turn_right");
            maneuver.putExtra("distance", "120");
            maneuver.putExtra("unit", "м");
            maneuver.putExtra("street", "Дружбы");
            NavProtocol.handle(context, maneuver);

            Intent eta = new Intent("kia.app.ACTION_ETA_DATA");
            eta.putExtra("edistance", "4.2 км");
            NavProtocol.handle(context, eta);

            Intent speed = new Intent("kia.app.ACTION_SPEED_DATA");
            speed.putExtra("speed_limit", "60");
            NavProtocol.handle(context, speed);
        } else if ("nav_preview".equals(scenario)) {
            Intent preview = new Intent("com.yf.navinfo");
            preview.putExtra("state", "route preview");
            preview.putExtra("app", "2GIS");
            NavProtocol.handleTeyesNavInfo(context, preview);
        } else if ("nav_failed".equals(scenario)) {
            Intent failed = new Intent("com.yf.navinfo");
            failed.putExtra("state", "route failed network error navigator mode=Off");
            failed.putExtra("app", "2GIS");
            NavProtocol.handleTeyesNavInfo(context, failed);
        } else if ("nav_finish".equals(scenario)) {
            Intent finish = new Intent("kia.app.ACTION_MANEUVER_DATA");
            finish.putExtra("imageId", "context_ra_finish");
            finish.putExtra("distance", "0");
            finish.putExtra("unit", "м");
            finish.putExtra("street", "");
            NavProtocol.handle(context, finish);
        } else if ("nav_off".equals(scenario)) {
            Intent off = new Intent("kia.app.ACTION_NAVI_ON_DATA");
            off.putExtra("navi_on", false);
            NavProtocol.handle(context, off);
        }
    }

    private static void rcta(Context context, String scenario) {
        AppPrefs.setBlindSpotEnabled(context, true);
        AppPrefs.setBlindSpotOverlay(context, true);
        if ("rcta_off".equals(scenario) || "rcta_idle".equals(scenario)) {
            BlindSpotState.fromCan(context, 0x4F4, hex("0000C00000000001"));
        } else if ("rcta_left".equals(scenario)) {
            BlindSpotState.fromCan(context, 0x4F4, hex("0001C00000003001"));
        } else if ("rcta_right".equals(scenario)) {
            BlindSpotState.fromCan(context, 0x4F4, hex("0001C00018000C61"));
        } else if ("rcta_both".equals(scenario)) {
            BlindSpotState.fromCan(context, 0x4F4, hex("0001C00118083069"));
        } else if ("rcta_unknown".equals(scenario)) {
            BlindSpotState.fromCan(context, 0x4F4, hex("0001C00000000002"));
        }
        AppService.refreshOverlays(context);
    }

    private static void tpms(Context context, String scenario) {
        AppPrefs.setTpmsEnabled(context, true);
        AppPrefs.setTpmsAlertOverlay(context, true);
        AppPrefs.setTpmsAlertSound(context, true);
        AppPrefs.suppressTpmsAlertUntil(context, 0L);

        if ("tpms_close".equals(scenario)) {
            TpmsAlertManager.suppressAfterUserClose(context);
            return;
        }
        if ("tpms_clear".equals(scenario) || "tpms_normal".equals(scenario)) {
            TpmsState.clear(context);
            return;
        }

        if ("tpms_low".equals(scenario)) {
            TpmsState.clear(context);
            TpmsState.status(context, "TPMS: QA низкое давление", true);
            TpmsState.tire(context, 0, 1.1f, 27, 4, false);
        } else if ("tpms_high".equals(scenario)) {
            TpmsState.status(context, "TPMS: QA высокое давление", true);
            TpmsState.tire(context, 3, 3.6f, 29, 2, false);
        }
    }

    private static void vehicle(Context context) {
        apply(context, 1, 0x132, hex("9001011400000001"));
        apply(context, 0, 0x316, hex("0000800A00003200"));
        apply(context, 0, 0x329, hex("00AF000000000000"));
        apply(context, 0, 0x044, hex("0000009A00000000"));
        apply(context, 0, 0x541, hex("0000000000000000"));
        apply(context, 0, 0x553, hex("0000000000000000"));
    }

    private static void apply(Context context, int bus, int id, byte[] data) {
        VehicleCanParser.apply(context, new CanSideband.Frame(bus, 0, id, data.length, data));
    }

    private static String text(Intent intent, String key) {
        String out = intent.getStringExtra(key);
        return out == null ? "" : out.trim();
    }

    private static byte[] hex(String value) {
        String clean = value.replace(" ", "");
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
