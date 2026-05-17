package kia.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Locale;

final class VehicleDisplayState {
    static final String ACTION_STATE = "com.kia.navi.VEHICLE_DISPLAY_STATE";
    static final String ACTION_API = "com.kia.navi.ACTION_VEHICLE_TELEMETRY";
    static final String LEGACY_ACTION_API = "kia.app.ACTION_VEHICLE_TELEMETRY";

    static final String EXTRA_SOURCE = "source";
    static final String EXTRA_STATUS = "status";
    static final String EXTRA_CONNECTED = "connected";
    static final String EXTRA_SPEED_KMH = "speed_kmh";
    static final String EXTRA_RPM = "rpm";
    static final String EXTRA_RUNTIME_SECONDS = "runtime_seconds";
    static final String EXTRA_VOLTAGE = "voltage";
    static final String EXTRA_COOLANT_C = "coolant_c";
    static final String EXTRA_ENGINE_LOAD = "engine_load";
    static final String EXTRA_THROTTLE = "throttle";
    static final String EXTRA_INTAKE_C = "intake_c";
    static final String EXTRA_FUEL_RATE = "fuel_rate";
    static final String EXTRA_CURRENT_MILEAGE_KM = "current_mileage_km";
    static final String EXTRA_TOTAL_MILEAGE_KM = "total_mileage_km";
    static final String EXTRA_TRIP_DISTANCE_KM = "trip_distance_km";

    private static Snapshot state = new Snapshot();

    private VehicleDisplayState() {
    }

    static synchronized Snapshot snapshot() {
        return new Snapshot(state);
    }

    static synchronized Snapshot fromObd(ObdState.Snapshot obd) {
        Snapshot next = new Snapshot();
        copyFromObd(next, obd);
        return next;
    }

    static synchronized void updateFromObd(Context context, ObdState.Snapshot obd) {
        copyFromObd(state, obd);
        broadcast(context);
    }

    static synchronized void applyIntent(Context context, Intent intent) {
        if (context != null && !AppPrefs.obdEnabled(context)) return;
        if (intent == null) return;
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        if (extras.containsKey(EXTRA_SOURCE)) state.source = intent.getStringExtra(EXTRA_SOURCE);
        if (extras.containsKey(EXTRA_STATUS)) state.status = intent.getStringExtra(EXTRA_STATUS);
        if (extras.containsKey(EXTRA_CONNECTED)) state.connected = intent.getBooleanExtra(EXTRA_CONNECTED, state.connected);
        if (extras.containsKey(EXTRA_SPEED_KMH)) state.speedKmh = clamp(intent.getIntExtra(EXTRA_SPEED_KMH, state.speedKmh), 0, 260);
        if (extras.containsKey(EXTRA_RPM)) state.rpm = clamp(intent.getIntExtra(EXTRA_RPM, state.rpm), 0, 8000);
        if (extras.containsKey(EXTRA_RUNTIME_SECONDS)) state.runtimeSeconds = Math.max(0, intent.getIntExtra(EXTRA_RUNTIME_SECONDS, state.runtimeSeconds));
        if (extras.containsKey(EXTRA_VOLTAGE)) {
            state.voltage = Math.max(0f, intent.getFloatExtra(EXTRA_VOLTAGE, state.voltage));
            state.voltageKnown = true;
        }
        if (extras.containsKey(EXTRA_COOLANT_C)) state.coolantTemp = clamp(intent.getIntExtra(EXTRA_COOLANT_C, state.coolantTemp), -40, 140);
        if (extras.containsKey(EXTRA_ENGINE_LOAD)) state.engineLoad = clamp(intent.getIntExtra(EXTRA_ENGINE_LOAD, state.engineLoad), 0, 100);
        if (extras.containsKey(EXTRA_THROTTLE)) state.throttle = clamp(intent.getIntExtra(EXTRA_THROTTLE, state.throttle), 0, 100);
        if (extras.containsKey(EXTRA_INTAKE_C)) state.intakeTemp = clamp(intent.getIntExtra(EXTRA_INTAKE_C, state.intakeTemp), -40, 140);
        if (extras.containsKey(EXTRA_FUEL_RATE)) state.fuelRate = Math.max(0f, intent.getFloatExtra(EXTRA_FUEL_RATE, state.fuelRate));
        if (extras.containsKey(EXTRA_CURRENT_MILEAGE_KM)) state.currentMileageKm = Math.max(0d, intent.getDoubleExtra(EXTRA_CURRENT_MILEAGE_KM, state.currentMileageKm));
        if (extras.containsKey(EXTRA_TOTAL_MILEAGE_KM)) state.totalMileageKm = Math.max(0d, intent.getDoubleExtra(EXTRA_TOTAL_MILEAGE_KM, state.totalMileageKm));
        if (extras.containsKey(EXTRA_TRIP_DISTANCE_KM)) state.tripDistanceKm = Math.max(0d, intent.getDoubleExtra(EXTRA_TRIP_DISTANCE_KM, state.tripDistanceKm));
        if (state.source == null || state.source.length() == 0) state.source = "API";
        if (state.status == null || state.status.length() == 0) {
            state.status = state.source + ": данные получены";
        }
        state.updatedAt = System.currentTimeMillis();
        AppLog.line(context, "API авто: источник=" + state.source + " скорость=" + state.speedKmh + " rpm=" + state.rpm);
        broadcast(context);
    }

    private static void copyFromObd(Snapshot target, ObdState.Snapshot obd) {
        if (obd == null) return;
        target.source = obd.status != null && obd.status.toLowerCase(Locale.ROOT).contains("эмуля")
                ? "OBD эмуляция"
                : "Kia Canbus";
        target.status = obd.status;
        target.connected = obd.connected;
        target.speedKmh = obd.speedKmh;
        target.rpm = obd.rpm;
        target.runtimeSeconds = obd.runtimeSeconds;
        target.voltage = obd.voltage;
        target.voltageKnown = obd.voltageKnown;
        target.coolantTemp = obd.coolantTemp;
        target.engineLoad = obd.engineLoad;
        target.throttle = obd.throttle;
        target.intakeTemp = obd.intakeTemp;
        target.fuelRate = obd.fuelRate;
        target.currentMileageKm = obd.currentMileageKm;
        target.totalMileageKm = obd.totalMileageKm;
        target.tripDistanceKm = obd.tripDistanceKm;
        target.updatedAt = obd.updatedAt;
    }

    private static void broadcast(Context context) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static final class Snapshot {
        String source = "Kia Canbus";
        String status = "";
        boolean connected;
        int speedKmh;
        int rpm;
        int runtimeSeconds;
        float voltage = 12.2f;
        boolean voltageKnown;
        int coolantTemp;
        int engineLoad;
        int throttle;
        int intakeTemp;
        float fuelRate;
        double currentMileageKm;
        double totalMileageKm;
        double tripDistanceKm;
        long updatedAt;

        Snapshot() {
        }

        Snapshot(Snapshot other) {
            source = other.source;
            status = other.status;
            connected = other.connected;
            speedKmh = other.speedKmh;
            rpm = other.rpm;
            runtimeSeconds = other.runtimeSeconds;
            voltage = other.voltage;
            voltageKnown = other.voltageKnown;
            coolantTemp = other.coolantTemp;
            engineLoad = other.engineLoad;
            throttle = other.throttle;
            intakeTemp = other.intakeTemp;
            fuelRate = other.fuelRate;
            currentMileageKm = other.currentMileageKm;
            totalMileageKm = other.totalMileageKm;
            tripDistanceKm = other.tripDistanceKm;
            updatedAt = other.updatedAt;
        }

        int displaySpeed(Context context) {
            return AppPrefs.displaySpeed(context, speedKmh);
        }

        String speedText(Context context) {
            return displaySpeed(context) + AppPrefs.speedUnitLabel(context);
        }

        String tempText(Context context, int celsius) {
            return AppPrefs.tempText(context, celsius);
        }

        String runtimeText() {
            int hours = runtimeSeconds / 3600;
            int minutes = (runtimeSeconds % 3600) / 60;
            int seconds = runtimeSeconds % 60;
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        }

        String voltageText() {
            if (!voltageKnown) return "--";
            return String.format(Locale.US, "%.1fV", voltage);
        }

        String fuelRateText() {
            return String.format(Locale.US, "%.1fL/h", fuelRate);
        }

        String tripDistanceText(Context context) {
            double km = Math.max(0d, tripDistanceKm);
            if (AppPrefs.speedUnit(context) == 1) {
                double miles = km * 0.621371d;
                return miles < 10d
                        ? String.format(Locale.US, "%.1f mi", miles)
                        : String.format(Locale.US, "%.0f mi", miles);
            }
            return km < 10d
                    ? String.format(Locale.US, "%.1f km", km)
                    : String.format(Locale.US, "%.0f km", km);
        }
    }
}
