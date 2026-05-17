package kia.app;

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
    static final String ACTION_TEYES_NAV_INFO = "com.yf.navinfo";
    static final String ACTION_TEYES_MAP_ASSISTANT = "com.teyes.MapAssistantService";
    static final String ACTION_MOBILE_NAVIGATION = "android.action.MOBILE_NAVIGATION";

    private static final String ACTION_MANEUVER = "kia.app.ACTION_MANEUVER_DATA";
    private static final String ACTION_ETA = "kia.app.ACTION_ETA_DATA";
    private static final String ACTION_NAVI_ON = "kia.app.ACTION_NAVI_ON_DATA";
    private static final String ACTION_SPEED = "kia.app.ACTION_SPEED_DATA";
    private static final String ACTION_EXCEEDED = "kia.app.ACTION_EXCEEDED_DATA";
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
    private static final Pattern SPEED_LIMIT_TEXT_PATTERN = Pattern.compile("(?:^|\\D)(\\d{1,3})\\s*(?:км/ч|km/h|kph)", Pattern.CASE_INSENSITIVE);
    private static final long FINISH_HOLD_MS = 5000L;
    private static final long TEYES_ROUTE_GRACE_MS = 12000L;
    private static final long NAV_REPLAY_MS = 1000L;

    private static String street;
    private static String speedLimit;
    private static String currentImageId;
    private static boolean exceeded;
    private static long lastStreetAt;
    private static long finishHoldUntil;
    private static long lastTeyesRouteAt;
    private static boolean teyesRouteActive;
    private static Handler handler;
    private static Runnable finishClearRunnable;
    private static Runnable navReplayRunnable;
    private static Context navReplayContext;
    private static boolean navActive;
    private static byte[] lastNavFrame;
    private static byte[] lastManeuverFrame;
    private static byte[] lastEtaFrame;
    private static byte[] lastSpeedFrame;
    private static byte[] lastTextFrame;
    private static boolean navSourceSent;
    private static String routeState = "off";
    private static String lastTeyesIntentKey;
    private static long lastTeyesIntentAt;
    private static String lastManeuverValue = "-";
    private static String lastDistanceValue = "-";
    private static String lastEtaValue = "-";
    private static String lastSpeedValue = "-";
    private static String lastStreetValue = "-";
    private static String lastRouteClearLog = "";
    private static long lastRouteClearLogAt;
    private static long lastWaitingRouteLogAt;

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

    static String adapterStateText() {
        String[][] rows = adapterRows();
        StringBuilder out = new StringBuilder();
        for (String[] row : rows) {
            if (out.length() > 0) out.append('\n');
            out.append(row[0]).append(": ").append(row[1]);
        }
        return out.toString();
    }

    static String[][] adapterRows() {
        String state;
        if (navActive) {
            state = "active";
        } else if (isFinishHoldActive()) {
            state = "finish hold";
        } else {
            state = routeState;
        }
        return new String[][]{
                {"Маршрут", safe(state)},
                {"Source", navSourceSent ? "0x7A nav sent" : "idle"},
                {"0x48", frameLine(lastNavFrame)},
                {"0x45", safe(lastManeuverValue) + " / " + safe(lastDistanceValue)
                        + " / " + frameLine(lastManeuverFrame)},
                {"0x47", safe(lastEtaValue) + " / " + frameLine(lastEtaFrame)},
                {"0x4A", safe(lastStreetValue) + " / " + frameLine(lastTextFrame)},
                {"0x44", safe(lastSpeedValue) + " / " + frameLine(lastSpeedFrame)},
                {"Компас", oneLine(CompassBridge.statusText())}
        };
    }

    static boolean canSendCompass() {
        return !navActive && !isFinishHoldActive();
    }

    static void handleTeyesNavInfo(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String event = safeAction(intent.getAction()) + " " + extras(intent);
        long now = System.currentTimeMillis();
        synchronized (NavProtocol.class) {
            if (TextUtils.equals(event, lastTeyesIntentKey) && now - lastTeyesIntentAt < 250L) {
                return;
            }
            lastTeyesIntentKey = event;
            lastTeyesIntentAt = now;
        }
        String state = extraText(intent, "state");
        String app = firstText(extraText(intent, "app"), extraText(intent, "package"), extraText(intent, "pkg"));
        NavDebugState.teyes(context, event);
        if (teyesStateIsExplicitOff(state)) {
            clearRouteNow(context, "Навигация TEYES: выключена, компас активен", true);
            return;
        }
        if (teyesStateIsPreviewOrFailure(state)) {
            if (teyesRouteActive) {
                finishTeyesRouteIfStale(context);
                if (teyesRouteActive) {
                    routeState = "active TEYES";
                    AppLog.setNav(context, "Навигация TEYES: " + previewLabel(state)
                            + ", активный маршрут не затираю");
                }
            } else {
                routeState = previewLabel(state);
                AppLog.setNav(context, "Навигация TEYES: " + previewLabel(state)
                        + ", маршрут не включён");
            }
            return;
        }
        if (!teyesStateAllowsRoute(state)) {
            routeState = "ignored " + safe(state);
            AppLog.setNav(context, "Навигация TEYES: не активное ведение " + safe(state));
            finishTeyesRouteIfStale(context);
            return;
        }

        String direction = extraText(intent, "direction");
        int directionLr = intent.getIntExtra("direction_lr", 0);
        String[] distance = teyesDistance(intent);
        String position = firstText(extraText(intent, "position"), extraText(intent, "describe"));
        String total = extraText(intent, "total_distance");
        boolean hasRoute = isFinishDirection(direction)
                || distance != null
                || !TextUtils.isEmpty(total)
                || hasMeaningfulDirection(direction);
        if (!hasRoute) {
            if (isOpenState(state) && isKnownNavigationApp(app)) {
                beginWaitingRoute(context, app);
                return;
            }
            routeState = "waiting route";
            finishTeyesRouteIfStale(context);
            if (!teyesRouteActive) {
                AppLog.setNav(context, "Навигация TEYES: ожидание маршрута");
            }
            return;
        }

        lastTeyesRouteAt = System.currentTimeMillis();
        teyesRouteActive = true;
        ensureNavigationOn(context, "active TEYES");

        String imageId = teyesDirectionToImageId(direction, directionLr);
        if (!TextUtils.isEmpty(imageId)) {
            Intent maneuver = new Intent(ACTION_MANEUVER);
            maneuver.putExtra("imageId", imageId);
            if (distance != null) {
                maneuver.putExtra("distance", distance[0]);
                maneuver.putExtra("unit", distance[1]);
            }
            if (!TextUtils.isEmpty(position)) maneuver.putExtra("street", position);
            handleManeuver(context, maneuver);
        } else if (!TextUtils.isEmpty(position)) {
            street = position;
            showStreet(context);
        } else if (TextUtils.equals(lastStreetValue, "Жду маршрут")) {
            showStatusText(context, "");
        }

        if (!TextUtils.isEmpty(total)) {
            Intent eta = new Intent(ACTION_ETA);
            eta.putExtra("edistance", total);
            handleEta(context, eta);
        }
        String limit = teyesSpeedLimit(intent, event);
        if (!TextUtils.isEmpty(limit)) {
            Intent speed = new Intent(ACTION_SPEED);
            speed.putExtra("speed_limit", limit);
            handleSpeed(context, speed);
        }

        AppLog.setNav(context, "Навигация TEYES: " + safe(app)
                + " / " + safe(direction)
                + " / " + safe(position));
    }

    private static void handleManeuver(Context context, Intent intent) {
        boolean hasImageId = intent.hasExtra("imageId");
        byte[] frame = hasImageId || lastManeuverFrame == null ? MANEUVER.clone() : lastManeuverFrame.clone();
        String imageId = intent.getStringExtra("imageId");
        boolean finish = "context_ra_finish".equals(imageId);
        if (hasImageId && !finish) {
            cancelFinishHold();
        }
        if (hasImageId && imageId != null) {
            currentImageId = imageId;
            lastManeuverValue = imageId;
            applyManeuverIcon(context, frame, imageId);
        }

        if (intent.hasExtra("distance")) {
            lastDistanceValue = safe(intent.getStringExtra("distance")) + " " + safe(intent.getStringExtra("unit"));
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
        } else if (hasImageId) {
            lastDistanceValue = "-";
        }

        send(context, frame);
        if (finish) {
            routeState = "finish hold";
            teyesRouteActive = false;
            showStatusText(context, "Вы приехали");
            holdFinish(context);
        }
        String nextStreet = intent.getStringExtra("street");
        if (intent.hasExtra("street")) {
            long now = System.currentTimeMillis();
            street = nextStreet;
            lastStreetValue = TextUtils.isEmpty(street) ? "-" : street;
            showStreet(context);
            lastStreetAt = now;
        }
        if (finish) {
            AppLog.setNav(context, "Навигация: маршрут завершён, вы приехали");
        } else {
            AppLog.setNav(context, "Навигация: маневр " + safe(imageId) + " " + safe(street));
        }
    }

    static void setTbtMode(Context context, boolean enabled) {
        AppPrefs.setNavTbt(context, enabled);
        if (!TextUtils.isEmpty(currentImageId) && (navActive || teyesRouteActive || isFinishHoldActive())) {
            resendCurrentManeuverForMode(context);
        }
        AppLog.setNav(context, "Навигация TBT: " + (enabled ? "включено" : "выключено"));
    }

    private static void resendCurrentManeuverForMode(Context context) {
        byte[] frame = MANEUVER.clone();
        if (lastManeuverFrame != null && lastManeuverFrame.length >= frame.length) {
            frame[9] = lastManeuverFrame[9];
            frame[10] = lastManeuverFrame[10];
            frame[11] = lastManeuverFrame[11];
            frame[12] = lastManeuverFrame[12];
        }
        applyManeuverIcon(context, frame, currentImageId);
        send(context, frame);
    }

    static void setTextMode(Context context, int mode) {
        AppPrefs.setNavTextMode(context, mode);
        showStreet(context);
        AppLog.setNav(context, "Навигация: " + navTextModeLabel(context));
    }

    private static void handleSpeed(Context context, Intent intent) {
        if (!intent.hasExtra("speed_limit")) return;
        speedLimit = intent.getStringExtra("speed_limit");
        lastSpeedValue = TextUtils.isEmpty(speedLimit) ? "-" : speedLimit.trim() + " km/h";
        showSpeed(context);
        String speed = speedLimit == null ? "" : speedLimit.trim();
        if (speed.isEmpty()) return;
        try {
            int value = Integer.parseInt(speed);
            byte[] frame = SPEED.clone();
            frame[5] = 1;
            frame[6] = (byte) (value & 0xff);
            send(context, frame);
            showStreet(context);
            AppLog.setNav(context, "Навигация: лимит " + value + " km/h");
        } catch (NumberFormatException e) {
            AppLog.setNav(context, "Навигация: лимит не число " + speed);
        }
    }

    private static void handleEta(Context context, Intent intent) {
        String value = intent.getStringExtra("edistance");
        if (value == null) return;
        lastEtaValue = value;
        Matcher matcher = ETA_PATTERN.matcher(value);
        if (!matcher.find()) {
            AppLog.setNav(context, "Навигация: ETA не распознано " + value);
            return;
        }
        byte[] frame = ETA.clone();
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
        send(context, frame);
        AppLog.setNav(context, "Навигация: осталось " + value);
    }

    private static void handleNaviOn(Context context, Intent intent) {
        if (!intent.hasExtra("navi_on")) return;
        boolean on = intent.getBooleanExtra("navi_on", false);
        if (on) {
            ensureNavigationOn(context, "active");
        } else {
            clearRouteNow(context, "Навигация: выключена, компас активен", true);
        }
    }

    private static void handleExceeded(Context context, Intent intent) {
        if (!intent.hasExtra("exceeded")) return;
        exceeded = intent.getBooleanExtra("exceeded", false);
        showStreet(context);
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

    private static String teyesDirectionToImageId(String direction, int directionLr) {
        String p = direction == null ? "" : direction.toLowerCase(Locale.US);
        boolean left = directionLr == 1 || p.contains("left") || p.contains("лев");
        boolean right = directionLr == 2 || p.contains("right") || p.contains("прав");
        if (p.contains("finish") || p.contains("arriv") || p.contains("destination") || p.contains("финиш")) {
            return "context_ra_finish";
        }
        if (p.contains("ferry")) return "context_ra_boardferry";
        if (p.contains("round") || p.contains("circular") || p.contains("circle") || p.contains("круг")) {
            return (p.contains("exit") || p.contains("out")) ? "context_ra_out_circular_movement" : "context_ra_in_circular_movement";
        }
        if (p.contains("uturn") || p.contains("u_turn") || p.contains("turn_back") || p.contains("развор")) {
            return left ? "context_ra_turn_back_left" : "context_ra_turn_back_right";
        }
        if (left || right) {
            String side = left ? "left" : "right";
            if (p.contains("exit")) return "context_ra_exit_" + side;
            if (p.contains("take")) return "context_ra_take_" + side;
            if (p.contains("hard") || p.contains("sharp")) return "context_ra_hard_turn_" + side;
            return "context_ra_turn_" + side;
        }
        if (p.contains("forward") || p.contains("straight") || p.contains("continue") || p.contains("прям")) {
            return "context_ra_forward";
        }
        return null;
    }

    private static String[] teyesDistance(Intent intent) {
        float meters = intent.getFloatExtra("distance_val", -1f);
        if (meters > 0f) {
            if (meters >= 1000f) {
                return new String[]{String.format(Locale.US, "%.1f", meters / 1000f), "км"};
            }
            return new String[]{String.valueOf(Math.round(meters)), "м"};
        }
        String value = firstText(extraText(intent, "distance_val_str"), extraText(intent, "distance"));
        if (TextUtils.isEmpty(value)) return null;
        String unit = firstText(extraText(intent, "distance_unit"), "м");
        return new String[]{value, unit};
    }

    private static String teyesSpeedLimit(Intent intent, String event) {
        String value = firstText(
                extraText(intent, "speed_limit"),
                extraText(intent, "speedLimit"),
                extraText(intent, "limit"),
                extraText(intent, "max_speed"),
                extraText(intent, "maxSpeed"),
                extraText(intent, "road_limit"),
                extraText(intent, "roadLimit"),
                extraText(intent, "camera_speed"),
                extraText(intent, "cameraSpeed"),
                extraText(intent, "speed_limit_val"),
                extraText(intent, "speed_limit_value"),
                extraText(intent, "limit_speed"),
                extraText(intent, "limitSpeed"),
                extraText(intent, "navi_speed_limit"),
                extraText(intent, "road_speed_limit"),
                extraText(intent, "speedLimitText")
        );
        String direct = speedLimitNumber(value);
        if (!TextUtils.isEmpty(direct)) return direct;
        Bundle extras = intent == null ? null : intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                if (!looksSpeedLimitKey(key)) continue;
                String parsed = speedLimitNumber(extraText(intent, key));
                if (!TextUtils.isEmpty(parsed)) return parsed;
            }
        }
        return speedLimitFromText(event);
    }

    private static boolean looksSpeedLimitKey(String key) {
        if (TextUtils.isEmpty(key)) return false;
        String k = key.toLowerCase(Locale.US);
        return k.contains("speed") || k.contains("limit") || k.contains("max")
                || k.contains("road") || k.contains("camera")
                || k.contains("огранич") || k.contains("скорост");
    }

    private static String speedLimitNumber(String value) {
        if (TextUtils.isEmpty(value)) return null;
        Matcher matcher = Pattern.compile("(\\d{1,3})").matcher(value);
        while (matcher.find()) {
            int parsed;
            try {
                parsed = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (parsed > 0 && parsed <= 200) return String.valueOf(parsed);
        }
        return null;
    }

    private static String speedLimitFromText(String value) {
        if (TextUtils.isEmpty(value)) return null;
        Matcher matcher = SPEED_LIMIT_TEXT_PATTERN.matcher(value);
        while (matcher.find()) {
            String parsed = speedLimitNumber(matcher.group(1));
            if (!TextUtils.isEmpty(parsed)) return parsed;
        }
        return null;
    }

    private static boolean teyesStateAllowsRoute(String state) {
        if (TextUtils.isEmpty(state)) return true;
        String s = state.toLowerCase(Locale.US).trim();
        return !teyesStateIsExplicitOff(s) && !teyesStateIsPreviewOrFailure(s);
    }

    private static boolean teyesStateIsExplicitOff(String state) {
        if (TextUtils.isEmpty(state)) return false;
        String s = state.toLowerCase(Locale.US).trim();
        return s.equals("0") || s.equals("false") || s.equals("off")
                || s.equals("close") || s.equals("closed")
                || s.equals("stop") || s.equals("stopped")
                || s.equals("idle") || s.equals("none")
                || s.contains("navigator mode=off")
                || s.contains("выключ") || s.contains("закры")
                || s.contains("останов");
    }

    private static boolean teyesStateIsPreviewOrFailure(String state) {
        if (TextUtils.isEmpty(state)) return false;
        String s = state.toLowerCase(Locale.US).trim();
        return s.contains("preview") || s.contains("search")
                || s.contains("failed") || s.contains("fail")
                || s.contains("error") || s.contains("ошиб");
    }

    private static String previewLabel(String state) {
        String s = state == null ? "" : state.toLowerCase(Locale.US);
        if (s.contains("failed") || s.contains("fail") || s.contains("error") || s.contains("ошиб")) {
            return "ошибка маршрута";
        }
        if (s.contains("search")) return "поиск маршрута";
        return "предпросмотр маршрута";
    }

    private static boolean isOpenState(String state) {
        if (TextUtils.isEmpty(state)) return true;
        String s = state.toLowerCase(Locale.US).trim();
        return s.equals("open") || s.equals("opened") || s.equals("working") || s.equals("active")
                || s.equals("on") || s.equals("true") || s.equals("1") || s.contains("guidance");
    }

    private static boolean isKnownNavigationApp(String app) {
        if (TextUtils.isEmpty(app)) return false;
        String a = app.toLowerCase(Locale.US);
        return a.contains("yandexnavi") || a.contains("yandex.maps") || a.contains("dublgis")
                || a.contains("2gis") || a.contains("google.android.apps.maps")
                || a.contains("waze") || a.contains("nav");
    }

    private static boolean hasMeaningfulDirection(String direction) {
        if (TextUtils.isEmpty(direction)) return false;
        String d = direction.toLowerCase(Locale.US).trim();
        return !(d.equals("0") || d.equals("none") || d.equals("unknown")
                || d.equals("null") || d.contains("loading") || d.contains("ожидан"));
    }

    private static boolean isFinishDirection(String direction) {
        if (TextUtils.isEmpty(direction)) return false;
        String d = direction.toLowerCase(Locale.US);
        return d.contains("finish") || d.contains("arriv") || d.contains("destination") || d.contains("финиш");
    }

    private static void beginWaitingRoute(Context context, String app) {
        long now = System.currentTimeMillis();
        lastTeyesRouteAt = now;
        if (teyesRouteActive) {
            finishTeyesRouteIfStale(context);
        } else {
            routeState = "waiting route";
        }
        if (now - lastWaitingRouteLogAt > 5000L) {
            lastWaitingRouteLogAt = now;
            AppLog.setNav(context, "Навигация TEYES: " + safe(app) + " открыта, жду маршрут");
        }
    }

    private static void ensureNavigationOn(Context context, String state) {
        cancelFinishHold();
        routeState = TextUtils.isEmpty(state) ? "active" : state;
        if (!navSourceSent) {
            CanbusControl.sendNavigationSourceQuiet(context);
            navSourceSent = true;
        }
        if (!navActive) {
            byte[] frame = NAV_ON.clone();
            frame[5] = 1;
            send(context, frame);
            AppLog.setNav(context, "Навигация: включена");
        }
        startNavReplay(context);
    }

    private static void showStreet(Context context) {
        if (isFinishHoldActive()) return;
        String value = navTextValue(context);
        lastStreetValue = TextUtils.isEmpty(value) ? "-" : value;
        send(context, textFrame(trim(value, 16)));
    }

    private static void showSpeed(Context context) {
        if (speedLimit == null) lastSpeedValue = "-";
    }

    private static void showStatusText(Context context, String value) {
        lastStreetValue = TextUtils.isEmpty(value) ? "-" : value;
        send(context, textFrame(trim(value, 16)));
    }

    private static String navTextValue(Context context) {
        String limit = speedLimitText();
        int mode = AppPrefs.navTextMode(context);
        if (mode == 1 && !TextUtils.isEmpty(limit)) return limit;
        if (mode == 2 && exceeded && !TextUtils.isEmpty(limit)) return limit;
        return street;
    }

    private static String speedLimitText() {
        String speed = speedLimit == null ? "" : speedLimit.trim();
        return speed.isEmpty() ? null : speed + " km/h";
    }

    private static String navTextModeLabel(Context context) {
        int mode = AppPrefs.navTextMode(context);
        if (mode == 1) return "0x4A показывает ограничение скорости";
        if (mode == 2) return "0x4A показывает лимит только при превышении";
        return "0x4A показывает улицу, speed limit отдельно через 0x44";
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
            clearRouteNow(app, "Навигация: маршрут завершён, компас активен", true);
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

    private static void clearRouteNow(Context context, String message, boolean sendOffFrame) {
        boolean changed = navActive || teyesRouteActive || isFinishHoldActive() || !"off".equals(routeState);
        stopNavReplay();
        cancelFinishHold();
        teyesRouteActive = false;
        lastTeyesRouteAt = 0L;
        navSourceSent = false;
        routeState = "off";
        street = null;
        speedLimit = null;
        currentImageId = null;
        exceeded = false;
        lastStreetAt = 0L;
        lastManeuverValue = "-";
        lastDistanceValue = "-";
        lastEtaValue = "-";
        lastSpeedValue = "-";
        lastStreetValue = "-";
        lastManeuverFrame = null;
        lastEtaFrame = null;
        lastSpeedFrame = null;
        lastTextFrame = null;
        byte[] frame = NAV_ON.clone();
        frame[5] = 0;
        complete(frame);
        if (sendOffFrame && navActive) {
            send(context, frame);
        } else {
            navActive = false;
            lastNavFrame = frame.clone();
        }
        long now = System.currentTimeMillis();
        if (changed || !TextUtils.equals(lastRouteClearLog, message) || now - lastRouteClearLogAt > 5000L) {
            lastRouteClearLog = message;
            lastRouteClearLogAt = now;
            AppLog.setNav(context, message);
        }
    }

    private static void startNavReplay(Context context) {
        if (context == null) return;
        navReplayContext = context.getApplicationContext();
        Handler h = handler();
        if (navReplayRunnable == null) {
            navReplayRunnable = () -> {
                Context app = navReplayContext;
                if (app == null || (!navActive && !isFinishHoldActive())) {
                    stopNavReplay();
                    return;
                }
                replayNavFrames(app);
                Handler next = handler();
                if (navReplayRunnable != null) next.postDelayed(navReplayRunnable, NAV_REPLAY_MS);
            };
        }
        h.removeCallbacks(navReplayRunnable);
        h.postDelayed(navReplayRunnable, NAV_REPLAY_MS);
    }

    private static void stopNavReplay() {
        if (handler != null && navReplayRunnable != null) {
            handler.removeCallbacks(navReplayRunnable);
        }
        navReplayContext = null;
    }

    private static void replayNavFrames(Context context) {
        if (navActive) CanbusControl.sendNavigationSourceQuiet(context);
        if (lastNavFrame != null && navActive) send(context, lastNavFrame.clone());
        if (lastManeuverFrame != null) send(context, lastManeuverFrame.clone());
        if (lastEtaFrame != null) send(context, lastEtaFrame.clone());
        if (lastTextFrame != null) send(context, lastTextFrame.clone());
        if (lastSpeedFrame != null) send(context, lastSpeedFrame.clone());
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
        rememberFrame(context, frame);
        NavDebugState.frame(context, CanbusControl.hex(frame));
        QaState.frame(context, frame);
        AppService.sendFrame(context, frame);
    }

    private static void rememberFrame(Context context, byte[] frame) {
        if (context == null || frame == null || frame.length < 6) return;
        int cmd = frame[4] & 0xff;
        byte[] copy = frame.clone();
        if (cmd == 0x48) {
            navActive = (frame[5] & 0xff) != 0;
            lastNavFrame = copy;
        } else if (cmd == 0x45) {
            lastManeuverFrame = copy;
        } else if (cmd == 0x47) {
            lastEtaFrame = copy;
        } else if (cmd == 0x44) {
            lastSpeedFrame = copy;
        } else if (cmd == 0x4A) {
            lastTextFrame = copy;
        }
    }

    private static void finishTeyesRouteIfStale(Context context) {
        long now = System.currentTimeMillis();
        if (teyesRouteActive && now - lastTeyesRouteAt > TEYES_ROUTE_GRACE_MS) {
            clearRouteNow(context, "Навигация TEYES: маршрут пропал, компас активен", true);
        } else if (teyesRouteActive) {
            routeState = "waiting route update";
            AppLog.setNav(context, "Навигация TEYES: жду обновление маршрута");
        }
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

    private static String firstText(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) return value.trim();
        }
        return null;
    }

    private static String extraText(Intent intent, String key) {
        if (intent == null || key == null || !intent.hasExtra(key)) return null;
        try {
            Object value = intent.getExtras() == null ? null : intent.getExtras().get(key);
            return value == null ? null : String.valueOf(value).trim();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safe(String value) {
        return TextUtils.isEmpty(value) ? "-" : value.toLowerCase(Locale.US);
    }

    private static String safeAction(String value) {
        return TextUtils.isEmpty(value) ? ACTION_TEYES_NAV_INFO : value;
    }

    private static String frameLine(byte[] frame) {
        return frame == null ? "-" : CanbusControl.hex(frame);
    }

    private static String oneLine(String value) {
        if (TextUtils.isEmpty(value)) return "-";
        return value.replace('\n', ' ').replace('\r', ' ').trim();
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
