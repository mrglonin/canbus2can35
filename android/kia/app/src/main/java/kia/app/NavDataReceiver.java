package kia.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NavDataReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NavProtocol.handle(context, intent);
    }
}
