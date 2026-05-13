package kia.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

final class TpmsDisplayApi {
    static final String ACTION_API = "com.kia.navi.ACTION_TPMS_TELEMETRY";
    static final String LEGACY_ACTION_API = "kia.app.ACTION_TPMS_TELEMETRY";

    static final String EXTRA_STATUS = "status";
    static final String EXTRA_CONNECTED = "connected";
    static final String EXTRA_INDEX = "index";
    static final String EXTRA_PRESSURE_BAR = "pressure_bar";
    static final String EXTRA_TEMPERATURE_C = "temperature_c";
    static final String EXTRA_WARNING = "warning";
    static final String EXTRA_LOW_BATTERY = "low_battery";

    private TpmsDisplayApi() {
    }

    static void applyIntent(Context context, Intent intent) {
        if (context != null && !AppPrefs.tpmsEnabled(context)) return;
        if (intent == null) return;
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        if (extras.containsKey(EXTRA_STATUS) || extras.containsKey(EXTRA_CONNECTED)) {
            String status = intent.getStringExtra(EXTRA_STATUS);
            boolean connected = intent.getBooleanExtra(EXTRA_CONNECTED, TpmsState.snapshot().connected);
            if (status == null || status.length() == 0) {
                status = connected ? "TPMS: данные получены через API" : "TPMS: источник API отключён";
            }
            TpmsState.status(context, status, connected);
        }
        if (extras.containsKey(EXTRA_INDEX)) {
            int index = intent.getIntExtra(EXTRA_INDEX, -1);
            float pressure = intent.getFloatExtra(EXTRA_PRESSURE_BAR, 0f);
            int temp = intent.getIntExtra(EXTRA_TEMPERATURE_C, 0);
            int warning = intent.getIntExtra(EXTRA_WARNING, 0);
            boolean lowBattery = intent.getBooleanExtra(EXTRA_LOW_BATTERY, false);
            if (!TpmsState.snapshot().connected) {
                TpmsState.status(context, "TPMS: данные получены через API", true);
            }
            TpmsState.tire(context, index, pressure, temp, warning, lowBattery);
            AppLog.line(context, "API TPMS: колесо=" + index + " давление=" + pressure + " темп=" + temp);
        }
    }
}
