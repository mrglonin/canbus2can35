package kia.app;

import android.content.Context;
import android.content.Intent;

final class BlindSpotState {
    static final String ACTION_STATE = "com.kia.navi.BLIND_SPOT_STATE";

    private static final long ACTIVE_MS = 2200L;

    private static boolean reverse;
    private static boolean left;
    private static boolean right;
    private static boolean rearUnknown;
    private static long eventAt;
    private static long lastLogAt;
    private static String lastRaw = "";

    private BlindSpotState() {
    }

    static synchronized Snapshot snapshot() {
        return new Snapshot(reverse, activeLeft(), activeRight(), activeUnknown(), eventAt, lastRaw);
    }

    static synchronized void reverse(Context context, boolean value) {
        if (reverse == value) return;
        reverse = value;
        if (!reverse) {
            left = false;
            right = false;
            rearUnknown = false;
            eventAt = 0L;
        }
        broadcast(context);
        AppService.refreshOverlays(context);
    }

    static synchronized void fromCan(Context context, int canId, byte[] data) {
        if (context == null || data == null || data.length < 8) return;
        if (!AppPrefs.blindSpotEnabled(context)) return;
        if (canId != 0x58B && canId != 0x4F4) return;

        String raw = CanbusControl.hex(data);
        if (!raw.equals(lastRaw)) {
            lastRaw = raw;
            long now = System.currentTimeMillis();
            if (now - lastLogAt > 500L) {
                lastLogAt = now;
                AppLog.line(context, "CAN слепые зоны 0x" + Integer.toHexString(canId).toUpperCase() + ": " + raw);
            }
        }

        if (canId == 0x4F4) {
            apply4f4(context, data, raw);
            return;
        }

        if (!reverse) {
            update(context, false, false, false, raw);
            return;
        }
        boolean nextLeft = (data[1] & 0x01) != 0 || (data[2] & 0x01) != 0;
        boolean nextRight = (data[1] & 0x02) != 0 || (data[2] & 0x02) != 0;
        update(context, nextLeft, nextRight, false, raw);
    }

    private static void apply4f4(Context context, byte[] data, String raw) {
        String compact = compact(raw);
        if ("0000C00000000001".equals(compact) || "0001C00000000001".equals(compact)) {
            update(context, false, false, false, raw);
            return;
        }
        boolean active = data.length >= 2 && data[0] == 0x00 && data[1] == 0x01;
        if (!active) {
            update(context, false, false, false, raw);
            return;
        }
        boolean nextLeft = isKnownLeft(compact)
                || (data.length > 6 && (data[6] & 0x30) != 0)
                || (data.length > 6 && (data[3] & 0x01) != 0 && (data[6] & 0x04) != 0);
        boolean nextRight = isKnownRight(compact)
                || (data.length > 4 && (data[4] & 0x1A) != 0)
                || (data.length > 5 && (data[5] & 0x18) != 0)
                || (!nextLeft && data.length > 6 && (data[6] & 0x0C) != 0);
        boolean unknown = !nextLeft && !nextRight;
        update(context, nextLeft, nextRight, unknown, raw);
    }

    private static void update(Context context, boolean nextLeft, boolean nextRight, boolean nextUnknown, String raw) {
        long now = System.currentTimeMillis();
        boolean changed = left != nextLeft || right != nextRight || rearUnknown != nextUnknown;
        left = nextLeft;
        right = nextRight;
        rearUnknown = nextUnknown;
        if (left || right || rearUnknown) {
            eventAt = now;
            RctaAlertManager.onWarning(context, left || rearUnknown, right || rearUnknown);
        }
        if (changed) {
            String label = !left && !right && !rearUnknown ? "нет предупреждений"
                    : rearUnknown ? "сзади"
                    : left && right ? "слева и справа"
                    : left ? "слева" : "справа";
            AppLog.line(context, "RCTA: " + label + " " + raw);
            broadcast(context);
            AppService.refreshOverlays(context);
        }
    }

    private static boolean activeLeft() {
        return left && System.currentTimeMillis() - eventAt <= ACTIVE_MS;
    }

    private static boolean activeRight() {
        return right && System.currentTimeMillis() - eventAt <= ACTIVE_MS;
    }

    private static boolean activeUnknown() {
        return rearUnknown && System.currentTimeMillis() - eventAt <= ACTIVE_MS;
    }

    private static void broadcast(Context context) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    static final class Snapshot {
        final boolean reverse;
        final boolean left;
        final boolean right;
        final boolean unknown;
        final long eventAt;
        final String raw;

        Snapshot(boolean reverse, boolean left, boolean right, boolean unknown, long eventAt, String raw) {
            this.reverse = reverse;
            this.left = left;
            this.right = right;
            this.unknown = unknown;
            this.eventAt = eventAt;
            this.raw = raw == null ? "" : raw;
        }

        boolean active() {
            return left || right || unknown;
        }

        String statusText() {
            if (unknown) return "предупреждение сзади";
            if (left && right) return "предупреждение слева и справа";
            if (left) return "предупреждение слева";
            if (right) return "предупреждение справа";
            if (!reverse) return "ожидание / нет предупреждений";
            return raw.length() == 0 ? "нет данных" : "нет предупреждений";
        }
    }

    private static boolean isKnownLeft(String compact) {
        return "0001C00000003001".equals(compact)
                || "0001C00100080409".equals(compact);
    }

    private static boolean isKnownRight(String compact) {
        return "0001C00002000805".equals(compact)
                || "0001C00010000841".equals(compact)
                || "0001C00018000C61".equals(compact)
                || "0001C01218100C61".equals(compact)
                || "0001C01318180C19".equals(compact)
                || "0001C01303180C19".equals(compact)
                || "0001C00003000C07".equals(compact);
    }

    private static String compact(String raw) {
        return raw == null ? "" : raw.replace(" ", "").toUpperCase(java.util.Locale.US);
    }
}
