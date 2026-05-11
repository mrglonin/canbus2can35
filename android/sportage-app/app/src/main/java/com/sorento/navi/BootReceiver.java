package com.sorento.navi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AppLog.line(context, "Автозапуск: " + (intent == null ? "нет intent" : intent.getAction()));
        if (!AppPrefs.autoStart(context)) {
            AppLog.line(context, "Автозапуск: выключен в настройках");
            return;
        }
        AppService.start(context);
        if (AppPrefs.backgroundAutoStart(context)) {
            AppLog.line(context, "Автозапуск: фоновый режим без открытия экрана");
            return;
        }
        try {
            Intent open = new Intent(context, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(open);
        } catch (Exception e) {
            AppLog.line(context, "Автозапуск экрана заблокирован: " + e.getClass().getSimpleName());
        }
    }
}
