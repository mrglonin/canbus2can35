package com.sorento.navi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TeyesNavInfoReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NavProtocol.handleTeyesNavInfo(context, intent);
    }
}
