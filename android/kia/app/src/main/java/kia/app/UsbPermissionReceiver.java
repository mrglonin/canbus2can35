package kia.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

public class UsbPermissionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean granted = intent != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
        AppLog.line(context, "USB-разрешение: " + (granted ? "дано" : "отклонено"));
        AppService.start(context);
        TpmsMonitor.start(context);
    }
}
