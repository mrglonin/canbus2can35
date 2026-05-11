package com.sorento.navi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TelemetryApiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (VehicleDisplayState.ACTION_API.equals(action) || VehicleDisplayState.LEGACY_ACTION_API.equals(action)) {
            VehicleDisplayState.applyIntent(context, intent);
        } else if (TpmsDisplayApi.ACTION_API.equals(action) || TpmsDisplayApi.LEGACY_ACTION_API.equals(action)) {
            TpmsDisplayApi.applyIntent(context, intent);
        }
    }
}
