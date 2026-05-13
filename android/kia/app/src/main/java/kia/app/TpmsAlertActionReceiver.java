package kia.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TpmsAlertActionReceiver extends BroadcastReceiver {
    static final String ACTION_CLOSE = "kia.app.TPMS_ALERT_CLOSE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        if (!ACTION_CLOSE.equals(intent.getAction())) return;
        TpmsAlertManager.suppressAfterUserClose(context);
    }
}
