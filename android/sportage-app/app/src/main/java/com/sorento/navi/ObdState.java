package com.sorento.navi;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class ObdState {
    static final String ACTION_STATE = "com.sorento.navi.OBD_STATE";

    private static Snapshot state = new Snapshot();

    private ObdState() {
    }

    static synchronized Snapshot snapshot() {
        return new Snapshot(state);
    }

    static synchronized void status(Context context, String value, boolean connected) {
        state.status = value;
        state.connected = connected;
        state.updatedAt = System.currentTimeMillis();
        if (value != null && value.length() > 0) AppLog.line(context, value);
        VehicleDisplayState.updateFromObd(context, state);
        broadcast(context);
    }

    static synchronized void speed(Context context, int value) {
        state.speedKmh = clamp(value, 0, 260);
        touch(context);
    }

    static synchronized void rpm(Context context, int value) {
        state.rpm = clamp(value, 0, 8000);
        touch(context);
    }

    static synchronized void runtime(Context context, int seconds) {
        state.runtimeSeconds = Math.max(0, seconds);
        touch(context);
    }

    static synchronized void voltage(Context context, float value) {
        state.voltage = Math.max(0f, value);
        touch(context);
    }

    static synchronized void coolant(Context context, int value) {
        state.coolantTemp = clamp(value, -40, 140);
        touch(context);
    }

    static synchronized void load(Context context, int value) {
        state.engineLoad = clamp(value, 0, 100);
        touch(context);
    }

    static synchronized void throttle(Context context, int value) {
        state.throttle = clamp(value, 0, 100);
        touch(context);
    }

    static synchronized void intake(Context context, int value) {
        state.intakeTemp = clamp(value, -40, 140);
        touch(context);
    }

    static synchronized void fuelRate(Context context, float value) {
        state.fuelRate = Math.max(0f, value);
        touch(context);
    }

    static synchronized void currentMileage(Context context, double value) {
        state.currentMileageKm = Math.max(0d, value);
        touch(context);
    }

    static synchronized void totalMileage(Context context, double value) {
        state.totalMileageKm = Math.max(0d, value);
        touch(context);
    }

    static synchronized void dtc(Context context, List<String> codes) {
        state.dtcCodes = codes == null ? Collections.emptyList() : new ArrayList<>(codes);
        touch(context);
    }

    static synchronized void emulation(Context context, int speedKmh, int rpm, int runtimeSeconds,
                                       float voltage, int coolantTemp, int engineLoad, int throttle,
                                       int intakeTemp, float fuelRate, double currentMileageKm,
                                       double totalMileageKm, String[] dtcCodes) {
        state.status = "OBD: данные эмуляции";
        state.connected = true;
        state.speedKmh = clamp(speedKmh, 0, 260);
        state.rpm = clamp(rpm, 0, 8000);
        state.runtimeSeconds = Math.max(0, runtimeSeconds);
        state.voltage = Math.max(0f, voltage);
        state.coolantTemp = clamp(coolantTemp, -40, 140);
        state.engineLoad = clamp(engineLoad, 0, 100);
        state.throttle = clamp(throttle, 0, 100);
        state.intakeTemp = clamp(intakeTemp, -40, 140);
        state.fuelRate = Math.max(0f, fuelRate);
        state.currentMileageKm = Math.max(0d, currentMileageKm);
        state.totalMileageKm = Math.max(0d, totalMileageKm);
        state.dtcCodes = dtcCodes == null ? Collections.emptyList() : new ArrayList<>(Arrays.asList(dtcCodes));
        state.updatedAt = System.currentTimeMillis();
        VehicleDisplayState.updateFromObd(context, state);
        broadcast(context);
    }

    private static void touch(Context context) {
        if (context != null && !AppPrefs.obdEnabled(context)) return;
        state.status = state.connected ? "Kia Canbus: подключено, данные обновляются" : state.status;
        state.updatedAt = System.currentTimeMillis();
        VehicleDisplayState.updateFromObd(context, state);
        broadcast(context);
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
        String status = "";
        boolean connected;
        int speedKmh;
        int rpm;
        int runtimeSeconds;
        float voltage = 12.2f;
        int coolantTemp;
        int engineLoad;
        int throttle;
        int intakeTemp;
        float fuelRate;
        double currentMileageKm;
        double totalMileageKm;
        long updatedAt;
        List<String> dtcCodes = Collections.emptyList();

        Snapshot() {
        }

        Snapshot(Snapshot other) {
            status = other.status;
            connected = other.connected;
            speedKmh = other.speedKmh;
            rpm = other.rpm;
            runtimeSeconds = other.runtimeSeconds;
            voltage = other.voltage;
            coolantTemp = other.coolantTemp;
            engineLoad = other.engineLoad;
            throttle = other.throttle;
            intakeTemp = other.intakeTemp;
            fuelRate = other.fuelRate;
            currentMileageKm = other.currentMileageKm;
            totalMileageKm = other.totalMileageKm;
            updatedAt = other.updatedAt;
            dtcCodes = new ArrayList<>(other.dtcCodes);
        }

        String runtimeText() {
            int hours = runtimeSeconds / 3600;
            int minutes = (runtimeSeconds % 3600) / 60;
            int seconds = runtimeSeconds % 60;
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

        String voltageText() {
            return String.format(java.util.Locale.US, "%.1fV", voltage);
        }
    }
}
