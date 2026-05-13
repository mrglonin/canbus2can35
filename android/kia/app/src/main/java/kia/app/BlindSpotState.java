package kia.app;

import android.content.Context;
import android.content.Intent;

final class BlindSpotState {
    static final String ACTION_STATE = "com.kia.navi.BLIND_SPOT_STATE";

    private static final long ACTIVE_MS = 2200L;

    private static boolean reverse;
    private static boolean left;
    private static boolean right;
    private static long eventAt;
    private static long lastLogAt;
    private static String lastRaw = "";

    private BlindSpotState() {
    }

    static synchronized Snapshot snapshot() {
        return new Snapshot(reverse, activeLeft(), activeRight(), eventAt, lastRaw);
    }

    static synchronized void reverse(Context context, boolean value) {
        if (reverse == value) return;
        reverse = value;
        if (!reverse) {
            left = false;
            right = false;
            eventAt = 0L;
        }
        broadcast(context);
        AppService.refreshOverlays(context);
    }

    static synchronized void fromCan(Context context, int canId, byte[] data) {
        if (context == null || data == null || data.length < 8) return;
        if (!AppPrefs.blindSpotEnabled(context)) return;
        if (canId != 0x58B) return;

        String raw = CanbusControl.hex(data);
        if (!raw.equals(lastRaw)) {
            lastRaw = raw;
            long now = System.currentTimeMillis();
            if (now - lastLogAt > 500L) {
                lastLogAt = now;
                AppLog.line(context, "CAN слепые зоны 0x58B: " + raw);
            }
        }

        if (!reverse) {
            update(context, false, false, raw);
            return;
        }

        boolean nextLeft = (data[1] & 0x01) != 0 || (data[2] & 0x01) != 0;
        boolean nextRight = (data[1] & 0x02) != 0 || (data[2] & 0x02) != 0;
        update(context, nextLeft, nextRight, raw);
    }

    private static void update(Context context, boolean nextLeft, boolean nextRight, String raw) {
        long now = System.currentTimeMillis();
        boolean changed = left != nextLeft || right != nextRight;
        left = nextLeft;
        right = nextRight;
        if (left || right) {
            eventAt = now;
            RctaAlertManager.onWarning(context, left, right);
        }
        if (changed) {
            String label = !left && !right ? "нет предупреждений" : left && right ? "слева и справа" : left ? "слева" : "справа";
            AppLog.line(context, "RCTA: " + label + " " + raw);
            broadcast(context);
            AppService.refreshOverlays(context);
        }
    }

    private static boolean activeLeft() {
        return reverse && left && System.currentTimeMillis() - eventAt <= ACTIVE_MS;
    }

    private static boolean activeRight() {
        return reverse && right && System.currentTimeMillis() - eventAt <= ACTIVE_MS;
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
        final long eventAt;
        final String raw;

        Snapshot(boolean reverse, boolean left, boolean right, long eventAt, String raw) {
            this.reverse = reverse;
            this.left = left;
            this.right = right;
            this.eventAt = eventAt;
            this.raw = raw == null ? "" : raw;
        }

        boolean active() {
            return left || right;
        }

        String statusText() {
            if (!reverse) return "ожидание заднего хода";
            if (left && right) return "предупреждение слева и справа";
            if (left) return "предупреждение слева";
            if (right) return "предупреждение справа";
            return raw.length() == 0 ? "нет данных" : "нет предупреждений";
        }
    }
}
