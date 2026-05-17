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
        } else if ("log_overlay_on".equals(scenario)) {
            AppPrefs.setLogOverlay(context, true);
            AppLog.line(context, "QA: журнал overlay включён");
            AppService.refreshOverlays(context);
        } else if ("log_overlay_off".equals(scenario)) {
            AppPrefs.setLogOverlay(context, false);
            AppLog.line(context, "QA: журнал overlay выключен");
            AppService.refreshOverlays(context);
        } else {
            AppLog.line(context, "QA: неизвестный сценарий " + scenario);
        }
    }

    private static void media(Context context, String scenario) {
        MediaMonitor.suppressAutoScan(3500L);
        if ("media_yandex".equals(scenario)) {
            MediaMonitor.reportExternal(context, "Яндекс Музыка", "ru.yandex.music",
                    "Vara Gianna", "Blood // Water", 225380, 160);
        } else if ("media_bt_selected_paused".equals(scenario)) {
            MediaMonitor.reportSourceHint(context, "Bluetooth", "com.teyes.music.widget", 180, 3000);
            QaState.event(context, "Bluetooth selected hint without playback");
        } else if ("media_bt_playing".equals(scenario)) {
            MediaMonitor.reportExternal(context, "Bluetooth", "com.android.bluetooth",
                    "Vara Gianna", "Blood // Water", 225380, 150, true);
        } else if ("media_usb".equals(scenario)) {
            MediaMonitor.reportExternal(context, "USB", "com.spd.media",
                    "OneRepublic", "Counting Stars", 232598, 153);
        } else if ("media_fm".equals(scenario)) {
            MediaMonitor.reportRadio(context, "FM радио 101.7 MHz", "com.spd.radio",
                    "Авторадио Казахстан", -1, 230, true, true, true);
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
        } else if ("nav_teyes_open".equals(scenario)) {
            Intent open = new Intent("com.yf.navinfo");
            open.putExtra("state", "open");
            open.putExtra("app", "ru.yandex.yandexnavi");
            NavProtocol.handleTeyesNavInfo(context, open);
        } else if ("nav_teyes_active".equals(scenario)) {
            Intent teyes = new Intent("com.yf.navinfo");
            teyes.putExtra("state", "open");
            teyes.putExtra("app", "ru.yandex.yandexnavi");
            teyes.putExtra("distance_val", 120f);
            teyes.putExtra("distance_val_str", "120");
            teyes.putExtra("distance_unit", "м");
            teyes.putExtra("total_distance", "4.2 км");
            teyes.putExtra("describe", "через 7 мин");
            teyes.putExtra("position", "Дружбы");
            teyes.putExtra("direction", "turn right");
            teyes.putExtra("direction_lr", 2);
            NavProtocol.handleTeyesNavInfo(context, teyes);
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
            BlindSpotState.fromCan(context, 0x58B, hex("0000000000000000"));
        } else if ("rcta_left".equals(scenario)) {
            BlindSpotState.fromCan(context, 0x58B, hex("0001000000000000"));
        } else if ("rcta_right".equals(scenario)) {
            BlindSpotState.fromCan(context, 0x58B, hex("0002000000000000"));
        } else if ("rcta_both".equals(scenario)) {
            BlindSpotState.fromCan(context, 0x58B, hex("0003000000000000"));
        } else if ("rcta_unknown".equals(scenario)) {
            BlindSpotState.fromCan(context, 0x58B, hex("0004000000000000"));
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
        } else if ("tpms_can_593".equals(scenario)) {
            VehicleCanParser.apply(context, new CanSideband.Frame(1, 0, 0x593, 6, hex("00111A1B1A1B")));
        }
    }

    private static void vehicle(Context context) {
        CanbusControl.handleIncomingFrame(context, snapshotFrame(72, 2100, 86, 14.4f, 22, hex("0000C00000000001")));
        apply(context, 1, 0x132, hex("9001011400000001"));
        apply(context, 0, 0x316, hex("0000800A00003200"));
        apply(context, 0, 0x329, hex("00AF000000000000"));
        apply(context, 0, 0x044, hex("0000009A00000000"));
        apply(context, 0, 0x131, hex("1000FF001000FF00"));
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

    private static byte[] snapshotFrame(int speed, int rpm, int coolant, float voltage, int outside, byte[] rcta) {
        byte[] frame = new byte[51];
        frame[0] = (byte) 0xBB;
        frame[1] = (byte) 0xA1;
        frame[2] = 0x41;
        frame[3] = (byte) frame.length;
        frame[4] = 0x77;
        int base = 5;
        frame[base] = 1;
        int known = 0x44F;
        frame[base + 1] = (byte) (known & 0xff);
        frame[base + 2] = (byte) ((known >> 8) & 0xff);
        frame[base + 3] = (byte) ((known >> 16) & 0xff);
        put32(frame, base + 4, 1);
        put16(frame, base + 8, speed);
        put16(frame, base + 10, rpm);
        put16(frame, base + 12, coolant);
        put16(frame, base + 14, Math.round(voltage * 1000f));
        frame[base + 18] = 2;
        put16(frame, base + 20, outside);
        frame[base + 30] = 1;
        frame[base + 31] = 0;
        frame[base + 32] = 8;
        put32(frame, base + 33, 0x58B);
        if (rcta != null) System.arraycopy(rcta, 0, frame, base + 37, Math.min(8, rcta.length));
        int sum = 0;
        for (int i = 0; i < frame.length - 1; i++) sum += frame[i] & 0xff;
        frame[frame.length - 1] = (byte) sum;
        return frame;
    }

    private static void put16(byte[] frame, int offset, int value) {
        frame[offset] = (byte) (value & 0xff);
        frame[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    private static void put32(byte[] frame, int offset, int value) {
        frame[offset] = (byte) (value & 0xff);
        frame[offset + 1] = (byte) ((value >> 8) & 0xff);
        frame[offset + 2] = (byte) ((value >> 16) & 0xff);
        frame[offset + 3] = (byte) ((value >> 24) & 0xff);
    }
}
