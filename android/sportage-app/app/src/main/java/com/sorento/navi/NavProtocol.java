package com.sorento.navi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NavProtocol {
    private static final String ACTION_MANEUVER = "com.sorento.navi.ACTION_MANEUVER_DATA";
    private static final String ACTION_ETA = "com.sorento.navi.ACTION_ETA_DATA";
    private static final String ACTION_NAVI_ON = "com.sorento.navi.ACTION_NAVI_ON_DATA";
    private static final String ACTION_SPEED = "com.sorento.navi.ACTION_SPEED_DATA";
    private static final String ACTION_EXCEEDED = "com.sorento.navi.ACTION_EXCEEDED_DATA";
    private static final String KIA_ACTION_MANEUVER = "com.kia.navi.ACTION_MANEUVER_DATA";
    private static final String KIA_ACTION_ETA = "com.kia.navi.ACTION_ETA_DATA";
    private static final String KIA_ACTION_NAVI_ON = "com.kia.navi.ACTION_NAVI_ON_DATA";
    private static final String KIA_ACTION_SPEED = "com.kia.navi.ACTION_SPEED_DATA";
    private static final String KIA_ACTION_EXCEEDED = "com.kia.navi.ACTION_EXCEEDED_DATA";

    private static final byte[] MANEUVER = {(byte) 0xBB, 0x41, (byte) 0xA1, 0x0E, 0x45, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] ETA = {(byte) 0xBB, 0x41, (byte) 0xA1, 0x0B, 0x47, 0, 0, 0, 0, 0, 0};
    private static final byte[] NAV_ON = {(byte) 0xBB, 0x41, (byte) 0xA1, 0x07, 0x48, 0, 0};
    private static final byte[] TEXT = {(byte) 0xBB, 0x41, (byte) 0xA1, 0, 0x4A, (byte) 0xF0};
    private static final byte[] SPEED = {(byte) 0xBB, 0x41, (byte) 0xA1, 0x08, 0x44, 0, 0, 0};
    private static final Pattern ETA_PATTERN = Pattern.compile("([\\d.,]+)\\s*(\\D+)");
    private static final byte[] maneuverFrame = MANEUVER.clone();
    private static final byte[] etaFrame = ETA.clone();
    private static final byte[] naviOnFrame = NAV_ON.clone();
    private static final byte[] speedFrame = SPEED.clone();
    private static final long FINISH_HOLD_MS = 5000L;

    private static String street;
    private static String speedLimit;
    private static String currentImageId;
    private static boolean exceeded;
    private static long lastStreetAt;
    private static long finishHoldUntil;
    private static Handler handler;
    private static Runnable finishClearRunnable;

    private NavProtocol() {
    }

    static void handle(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        NavDebugState.event(context, action + " " + extras(intent));
        try {
            if (ACTION_MANEUVER.equals(action) || KIA_ACTION_MANEUVER.equals(action)) {
                handleManeuver(context, intent);
            } else if (ACTION_SPEED.equals(action) || KIA_ACTION_SPEED.equals(action)) {
                handleSpeed(context, intent);
            } else if (ACTION_ETA.equals(action) || KIA_ACTION_ETA.equals(action)) {
                handleEta(context, intent);
            } else if (ACTION_NAVI_ON.equals(action) || KIA_ACTION_NAVI_ON.equals(action)) {
                handleNaviOn(context, intent);
            } else if (ACTION_EXCEEDED.equals(action) || KIA_ACTION_EXCEEDED.equals(action)) {
                handleExceeded(context, intent);
            }
        } catch (Exception e) {
            AppLog.setNav(context, "Навигация: ошибка " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    private static void handleManeuver(Context context, Intent intent) {
        byte[] frame = maneuverFrame;
        String imageId = intent.getStringExtra("imageId");
        boolean finish = "context_ra_finish".equals(imageId);
        if (intent.hasExtra("imageId") && !finish) {
            cancelFinishHold();
        }
        if (intent.hasExtra("imageId") && imageId != null) {
            currentImageId = imageId;
            applyManeuverIcon(context, frame, imageId);
        }

        if (intent.hasExtra("distance")) {
            float value = parseFloat(intent.getStringExtra("distance"));
            if (value != 0f) {
                int whole = (int) value;
                int tenth = Math.round((value - whole) * 10f);
                frame[9] = (byte) ((whole >> 8) & 0xff);
                frame[10] = (byte) (whole & 0xff);
                frame[12] = (byte) ((tenth & 0x0f) | (frame[12] & 0xf0));
            }
            String unit = intent.getStringExtra("unit");
            if ("м".equals(unit) || "m".equalsIgnoreCase(unit)) {
                frame[11] = (byte) (frame[11] & 0x0f);
            } else if ("км".equals(unit) || "km".equalsIgnoreCase(unit)) {
                frame[11] = (byte) ((frame[11] & 0x0f) | 0x10);
            }
            int lowDistance = frame[10] & 0xff;
            if ((frame[11] & 0xf0) == 0 && frame[9] == 0 && lowDistance < 100) {
                frame[12] = (byte) ((((lowDistance / 10) & 0x0f) << 4) | (frame[12] & 0x0f));
            } else {
                frame[12] = (byte) ((frame[12] & 0x0f) | 0xA0);
            }
        }

        send(context, frame.clone());
        if (finish) {
            holdFinish(context);
        }
        String nextStreet = intent.getStringExtra("street");
        if (intent.hasExtra("street")) {
            long now = System.currentTimeMillis();
            boolean changed = !TextUtils.equals(street, nextStreet);
            street = nextStreet;
            if (changed || now - lastStreetAt >= 3000) {
                showStreet(context);
                lastStreetAt = now;
            }
        }
        AppLog.setNav(context, "Навигация: маневр " + safe(imageId) + " " + safe(street));
    }

    static void setTbtMode(Context context, boolean enabled) {
        AppPrefs.setNavTbt(context, enabled);
        if (currentImageId != null) {
            applyManeuverIcon(context, maneuverFrame, currentImageId);
        }
        send(context, maneuverFrame.clone());
        AppLog.setNav(context, "Навигация TBT: " + (enabled ? "включена" : "выключена"));
    }

    static void setTextMode(Context context, int mode) {
        int safeMode = Math.max(0, Math.min(2, mode));
        AppPrefs.setNavTextMode(context, safeMode);
        if (safeMode == 1) {
            showSpeed(context);
        } else {
            showStreet(context);
        }
        AppLog.setNav(context, "Навигация: режим текста " + safeMode);
    }

    private static void handleSpeed(Context context, Intent intent) {
        if (!intent.hasExtra("speed_limit")) return;
        speedLimit = intent.getStringExtra("speed_limit");
        showSpeed(context);
        String speed = speedLimit == null ? "" : speedLimit.trim();
        if (speed.isEmpty()) return;
        try {
            int value = Integer.parseInt(speed);
            byte[] frame = speedFrame;
            frame[5] = 1;
            frame[6] = (byte) (value & 0xff);
            send(context, frame.clone());
            AppLog.setNav(context, "Навигация: лимит " + value + " km/h");
        } catch (NumberFormatException e) {
            AppLog.setNav(context, "Навигация: лимит не число " + speed);
        }
    }

    private static void handleEta(Context context, Intent intent) {
        String value = intent.getStringExtra("edistance");
        if (value == null) return;
        Matcher matcher = ETA_PATTERN.matcher(value);
        if (!matcher.find()) {
            AppLog.setNav(context, "Навигация: ETA не распознано " + value);
            return;
        }
        byte[] frame = etaFrame;
        float distance = parseFloat(matcher.group(1));
        if (distance != 0f) {
            int whole = (int) distance;
            int tenth = Math.round((distance - whole) * 10f);
            frame[6] = (byte) ((whole >> 8) & 0xff);
            frame[7] = (byte) (whole & 0xff);
            frame[8] = (byte) tenth;
        }
        String unit = matcher.group(2) == null ? "" : matcher.group(2).trim();
        if ("м".equals(unit) || "m".equalsIgnoreCase(unit)) {
            frame[9] = 0;
        } else if ("км".equals(unit) || "km".equalsIgnoreCase(unit)) {
            frame[9] = 1;
        }
        send(context, frame.clone());
        AppLog.setNav(context, "Навигация: осталось " + value);
    }

    private static void handleNaviOn(Context context, Intent intent) {
        if (!intent.hasExtra("navi_on")) return;
        byte[] frame = naviOnFrame;
        boolean on = intent.getBooleanExtra("navi_on", false);
        if (on) {
            cancelFinishHold();
        } else if (isFinishHoldActive()) {
            scheduleFinishClear(context, finishHoldUntil);
            AppLog.setNav(context, "Навигация: финиш удерживается 5 сек.");
            return;
        }
        frame[5] = on ? (byte) 1 : 0;
        send(context, frame.clone());
        AppLog.setNav(context, "Навигация: " + (on ? "включена" : "выключена"));
    }

    private static void handleExceeded(Context context, Intent intent) {
        if (!intent.hasExtra("exceeded")) return;
        exceeded = intent.getBooleanExtra("exceeded", false);
        if (AppPrefs.navTextMode(context) == 2) {
            if (exceeded) {
                showSpeed(context);
            } else {
                showStreet(context);
            }
        }
        AppLog.setNav(context, "Навигация: превышение " + exceeded);
    }

    private static void applyManeuverIcon(Context context, byte[] frame, String imageId) {
        if (AppPrefs.navTbt(context)) {
            frame[6] = 0;
            frame[7] = 0;
            frame[8] = 0;
            applyTbtIcon(frame, imageId);
        } else {
            frame[5] = 0x0D;
            frame[6] = 0;
            frame[7] = 0;
            applyClassicIcon(frame, imageId);
        }
    }

    private static void applyClassicIcon(byte[] f, String id) {
        switch (iconCase(id)) {
            case 0:
                f[8] = 0x02;
                break;
            case 1:
                f[8] = 0x2D;
                break;
            case 2:
            case 3:
                f[8] = 0;
                f[7] = 1;
                break;
            case 4:
                f[8] = 0x12;
                break;
            case 5:
                f[8] = 0;
                f[5] = 0x20;
                break;
            case 6:
                f[8] = 0x06;
                f[5] = 0x20;
                break;
            case 7:
                f[8] = 0x2D;
                f[7] = 1;
                f[6] = 0x40;
                break;
            case 8:
                f[8] = 0x1E;
                break;
            case 9:
                f[8] = 0x03;
                break;
            case 10:
                f[8] = 0x24;
                break;
            case 11:
                f[8] = 0x0C;
                break;
            case 12:
                f[8] = 0x03;
                f[7] = 0x03;
                break;
            case 13:
                f[8] = 0x0C;
                f[5] = 0x1F;
                break;
            case 14:
                f[8] = 0x24;
                f[5] = 0x1F;
                break;
            default:
                f[5] = 0x09;
                break;
        }
    }

    private static void applyTbtIcon(byte[] f, String id) {
        switch (iconCase(id)) {
            case 0:
                f[5] = 0x70;
                break;
            case 1:
                f[5] = 0x47;
                break;
            case 2:
                f[5] = (byte) 0xB0;
                break;
            case 3:
                f[5] = 0x41;
                break;
            case 4:
                f[5] = 0x44;
                break;
            case 5:
                f[5] = 0x60;
                break;
            case 6:
                f[5] = 0x61;
                break;
            case 7:
                f[5] = (byte) 0x95;
                break;
            case 8:
                f[5] = 0x45;
                break;
            case 9:
                f[5] = 0x42;
                break;
            case 10:
                f[5] = 0x46;
                break;
            case 11:
                f[5] = 0x43;
                break;
            case 12:
                f[5] = (byte) 0x93;
                break;
            case 13:
                f[5] = 0x49;
                break;
            case 14:
                f[5] = 0x48;
                break;
            default:
                f[5] = 0x09;
                break;
        }
    }

    private static int iconCase(String id) {
        if (id == null) return -1;
        switch (id) {
            case "context_ra_finish":
                return 0;
            case "context_ra_take_left":
                return 1;
            case "context_ra_boardferry":
                return 2;
            case "context_ra_forward":
                return 3;
            case "context_ra_hard_turn_right":
                return 4;
            case "context_ra_in_circular_movement":
                return 5;
            case "context_ra_out_circular_movement":
                return 6;
            case "context_ra_exit_left":
                return 7;
            case "context_ra_hard_turn_left":
                return 8;
            case "context_ra_take_right":
                return 9;
            case "context_ra_turn_left":
                return 10;
            case "context_ra_turn_right":
                return 11;
            case "context_ra_exit_right":
                return 12;
            case "context_ra_turn_back_right":
                return 13;
            case "context_ra_turn_back_left":
                return 14;
            default:
                return -1;
        }
    }

    private static void showStreet(Context context) {
        if (isFinishHoldActive()) return;
        int mode = AppPrefs.navTextMode(context);
        if (mode == 1 || (mode == 2 && exceeded)) return;
        send(context, textFrame(trim(street, 16)));
    }

    private static void showSpeed(Context context) {
        if (isFinishHoldActive()) return;
        int mode = AppPrefs.navTextMode(context);
        if (mode == 0 || (mode == 2 && !exceeded)) return;
        String text = speedLimit == null ? null : speedLimit.trim() + " km/h";
        send(context, textFrame(text));
    }

    private static void holdFinish(Context context) {
        long until = System.currentTimeMillis() + FINISH_HOLD_MS;
        finishHoldUntil = until;
        scheduleFinishClear(context, until);
    }

    private static void scheduleFinishClear(Context context, long until) {
        Context app = context.getApplicationContext();
        Handler h = handler();
        if (finishClearRunnable != null) {
            h.removeCallbacks(finishClearRunnable);
        }
        finishClearRunnable = () -> {
            if (finishHoldUntil != until) return;
            finishHoldUntil = 0L;
            byte[] frame = naviOnFrame;
            frame[5] = 0;
            send(app, frame.clone());
            AppLog.setNav(app, "Навигация: финиш очищен после 5 сек.");
        };
        long delay = Math.max(0L, until - System.currentTimeMillis());
        h.postDelayed(finishClearRunnable, delay);
    }

    private static void cancelFinishHold() {
        finishHoldUntil = 0L;
        if (handler != null && finishClearRunnable != null) {
            handler.removeCallbacks(finishClearRunnable);
        }
        finishClearRunnable = null;
    }

    private static boolean isFinishHoldActive() {
        return finishHoldUntil > System.currentTimeMillis();
    }

    private static Handler handler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }

    private static byte[] textFrame(String value) {
        if (TextUtils.isEmpty(value)) {
            byte[] frame = new byte[9];
            System.arraycopy(TEXT, 0, frame, 0, TEXT.length);
            frame[3] = 9;
            frame[6] = 0;
            frame[7] = 0;
            complete(frame);
            return frame;
        }
        byte[] text = value.getBytes(StandardCharsets.UTF_16LE);
        int length = Math.min(text.length, 28);
        byte[] frame = new byte[length + 7];
        System.arraycopy(TEXT, 0, frame, 0, TEXT.length);
        System.arraycopy(text, 0, frame, 6, length);
        frame[3] = (byte) frame.length;
        complete(frame);
        return frame;
    }

    private static void send(Context context, byte[] frame) {
        complete(frame);
        NavDebugState.frame(context, CanbusControl.hex(frame));
        AppService.sendFrame(context, frame);
    }

    private static void complete(byte[] frame) {
        int len = frame[3] & 0xff;
        int sum = 0;
        for (int i = 0; i < len - 1 && i < frame.length; i++) {
            sum += frame[i] & 0xff;
        }
        if (len > 0 && len <= frame.length) {
            frame[len - 1] = (byte) (sum & 0xff);
        }
    }

    private static float parseFloat(String value) {
        if (value == null) return 0f;
        String normalized = value.replace(',', '.').trim();
        Matcher matcher = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?").matcher(normalized);
        if (matcher.find()) normalized = matcher.group();
        try {
            return Float.parseFloat(normalized);
        } catch (Exception e) {
            return 0f;
        }
    }

    private static String trim(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static String safe(String value) {
        return TextUtils.isEmpty(value) ? "-" : value.toLowerCase(Locale.US);
    }

    private static String extras(Intent intent) {
        Bundle b = intent.getExtras();
        if (b == null || b.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (String key : b.keySet()) {
            if (out.length() > 0) out.append(' ');
            Object value = b.get(key);
            out.append(key).append('=').append(value);
            if (out.length() > 220) break;
        }
        return out.toString();
    }
}
