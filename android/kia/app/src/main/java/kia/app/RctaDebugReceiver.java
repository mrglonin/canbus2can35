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
        byte[] data;
        if ("left".equals(side)) {
            data = hex("0001C00000003001");
        } else if ("right".equals(side)) {
            data = hex("0001C00018000C61");
        } else if ("unknown".equals(side) || "rear".equals(side)) {
            data = hex("0001C00000000002");
        } else {
            data = hex("0001C00018003061");
            side = "both";
        }
        BlindSpotState.fromCan(context, 0x4F4, data);
        AppLog.line(context, "RCTA debug: " + side);
    }

    private static byte[] hex(String value) {
        byte[] out = new byte[value.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
