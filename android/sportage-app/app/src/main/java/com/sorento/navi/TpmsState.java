package com.sorento.navi;

import android.content.Context;
import android.content.Intent;

final class TpmsState {
    static final String ACTION_STATE = "com.sorento.navi.TPMS_STATE";

    private static Snapshot state = new Snapshot();

    private TpmsState() {
    }

    static synchronized Snapshot snapshot() {
        return new Snapshot(state);
    }

    static synchronized void status(Context context, String value, boolean connected) {
        state.status = value;
        state.connected = connected;
        state.updatedAt = System.currentTimeMillis();
        if (value != null && value.length() > 0) AppLog.line(context, value);
        broadcast(context);
    }

    static synchronized void tire(Context context, int index, float pressureBar, int temperatureC, int warning, boolean lowBattery) {
        if (context != null && !AppPrefs.tpmsEnabled(context)) return;
        if (index < 0 || index >= state.tires.length) return;
        Tire tire = state.tires[index];
        tire.hasData = true;
        tire.pressureBar = Math.max(0f, pressureBar);
        tire.temperatureC = temperatureC;
        tire.warning = thresholdWarning(context, tire.pressureBar, warning);
        tire.lowBattery = lowBattery;
        tire.updatedAt = System.currentTimeMillis();
        state.status = state.connected ? "TPMS: данные обновляются" : state.status;
        state.updatedAt = System.currentTimeMillis();
        broadcast(context);
        TpmsAlertManager.onTpmsUpdate(context, new Snapshot(state));
    }

    static synchronized void clear(Context context) {
        state = new Snapshot();
        broadcast(context);
    }

    private static void broadcast(Context context) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    private static int thresholdWarning(Context context, float pressureBar, int fallback) {
        if (context == null || pressureBar <= 0f) return fallback;
        if (pressureBar < AppPrefs.tpmsLowBar(context)) return 4;
        if (pressureBar > AppPrefs.tpmsHighBar(context)) return 2;
        return fallback;
    }

    static final class Snapshot {
        String status = "";
        boolean connected;
        long updatedAt;
        Tire[] tires = new Tire[]{
                new Tire("ЛЕВОЕ ПЕРЕДНЕЕ"),
                new Tire("ПРАВОЕ ПЕРЕДНЕЕ"),
                new Tire("ЛЕВОЕ ЗАДНЕЕ"),
                new Tire("ПРАВОЕ ЗАДНЕЕ")
        };

        Snapshot() {
        }

        Snapshot(Snapshot other) {
            status = other.status;
            connected = other.connected;
            updatedAt = other.updatedAt;
            tires = new Tire[other.tires.length];
            for (int i = 0; i < other.tires.length; i++) {
                tires[i] = new Tire(other.tires[i]);
            }
        }
    }

    static final class Tire {
        final String label;
        boolean hasData;
        float pressureBar;
        int temperatureC;
        int warning;
        boolean lowBattery;
        long updatedAt;

        Tire(String label) {
            this.label = label;
        }

        Tire(Tire other) {
            label = other.label;
            hasData = other.hasData;
            pressureBar = other.pressureBar;
            temperatureC = other.temperatureC;
            warning = other.warning;
            lowBattery = other.lowBattery;
            updatedAt = other.updatedAt;
        }

        String pressureText() {
            return hasData ? String.format(java.util.Locale.US, "%.1f", pressureBar) : "__";
        }

        String tempText() {
            return hasData ? String.valueOf(temperatureC) : "__";
        }

        String warningText() {
            if (lowBattery || warning == 5) return "НИЗКИЙ ЗАРЯД";
            switch (warning) {
                case 1:
                    return "БЫСТРАЯ УТЕЧКА";
                case 2:
                    return "ВЫСОКОЕ ДАВЛЕНИЕ";
                case 3:
                    return "ВЫСОКАЯ ТЕМП.";
                case 4:
                    return "НИЗКОЕ ДАВЛЕНИЕ";
                default:
                    return hasData ? "НОРМА" : "ОЖИДАНИЕ";
            }
        }

        boolean alert() {
            return warning > 0 || lowBattery;
        }
    }
}
