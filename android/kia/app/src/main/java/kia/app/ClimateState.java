package kia.app;

import android.content.Context;
import android.content.Intent;

import java.util.Locale;

final class ClimateState {
    static final String ACTION_STATE = "kia.app.CLIMATE_STATE";

    private static Snapshot state = new Snapshot();

    private ClimateState() {
    }

    static synchronized Snapshot snapshot() {
        return new Snapshot(state);
    }

    static synchronized void fromCan(Context context, int canId, byte[] data) {
        if (data == null || data.length == 0) return;
        Snapshot next = new Snapshot(state);
        boolean parsed = false;
        if (canId == 0x131 && data.length >= 5) {
            int left = climateTempFromDatc131(data[0] & 0xff);
            int right = climateTempFromDatc131(data[4] & 0xff);
            if (left > 0 || right > 0) {
                next.driverTempC = left;
                next.passengerTempC = right > 0 ? right : left;
                parsed = true;
            }
        } else if ((canId == 0x132 || canId == 0x134) && data.length >= 4) {
            int left = climateTempDirect(data[0] & 0xff);
            int right = climateTempDirect(data[1] & 0xff);
            if (left > 0 || right > 0) {
                next.driverTempC = left;
                next.passengerTempC = right > 0 ? right : left;
                next.fan = data[2] & 0xff;
                next.flags = data[3] & 0xff;
                parsed = true;
            }
        }
        if (!parsed) return;
        next.connected = true;
        next.updatedAt = System.currentTimeMillis();
        state = next;
        broadcast(context);
    }

    private static int climateTempFromDatc131(int raw) {
        if (raw == 0 || raw == 0xff) return 0;
        if (raw >= 0x0a && raw <= 0x20) return raw + 6;
        return climateTempDirect(raw);
    }

    private static int climateTempDirect(int raw) {
        if (raw == 0 || raw == 0xff) return 0;
        return raw >= 16 && raw <= 32 ? raw : 0;
    }

    private static void broadcast(Context context) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    static final class Snapshot {
        boolean connected;
        int driverTempC;
        int passengerTempC;
        int fan;
        int flags;
        long updatedAt;

        Snapshot() {
        }

        Snapshot(Snapshot other) {
            connected = other.connected;
            driverTempC = other.driverTempC;
            passengerTempC = other.passengerTempC;
            fan = other.fan;
            flags = other.flags;
            updatedAt = other.updatedAt;
        }

        String text(Context context) {
            if (!connected || driverTempC <= 0) return "--";
            String left = AppPrefs.tempText(context, driverTempC);
            if (passengerTempC > 0 && passengerTempC != driverTempC) {
                return left + "/" + AppPrefs.tempText(context, passengerTempC);
            }
            return left;
        }

        String debugText(Context context) {
            if (!connected) return "климат: нет данных";
            return String.format(Locale.US, "климат %s fan=%d flags=0x%02X", text(context), fan, flags);
        }
    }
}
