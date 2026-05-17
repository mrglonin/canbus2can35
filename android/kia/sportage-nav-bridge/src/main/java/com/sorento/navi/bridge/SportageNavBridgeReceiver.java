package com.sorento.navi.bridge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SportageNavBridgeReceiver extends BroadcastReceiver {
    private static final String SPORTAGE_PREFIX = "com.sorento.navi.";
    private static final String KIA_PREFIX = "kia.app.";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (!action.startsWith(SPORTAGE_PREFIX)) return;

        Intent out = new Intent(KIA_PREFIX + action.substring(SPORTAGE_PREFIX.length()));
        out.setComponent(new ComponentName("kia.app", "kia.app.NaviReceiver"));
        Bundle extras = intent.getExtras();
        if (extras != null) out.putExtras(extras);
        context.sendBroadcast(out);
    }
}
