package kia.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Locale;

public class RctaDebugReceiver extends BroadcastReceiver {
    static final String ACTION = "kia.app.DEBUG_RCTA";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String side = intent.getStringExtra("side");
        if (side == null || side.length() == 0) side = "both";
        side = side.toLowerCase(Locale.US);

        AppPrefs.setBlindSpotEnabled(context, true);
        AppPrefs.setBlindSpotOverlay(context, true);
        AppService.start(context);
        AppService.refreshOverlays(context);

        if ("off".equals(side) || "clear".equals(side) || "none".equals(side)) {
            BlindSpotState.reverse(context, false);
            AppLog.line(context, "RCTA debug: off");
            return;
        }

        BlindSpotState.reverse(context, true);
        byte[] data = new byte[8];
        if ("left".equals(side)) {
            data[1] = 0x01;
        } else if ("right".equals(side)) {
            data[1] = 0x02;
        } else {
            data[1] = 0x03;
            side = "both";
        }
        BlindSpotState.fromCan(context, 0x58B, data);
        AppLog.line(context, "RCTA debug: " + side);
    }
}
