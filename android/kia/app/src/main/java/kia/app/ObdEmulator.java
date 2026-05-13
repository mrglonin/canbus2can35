package kia.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

final class ObdEmulator {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static Context appContext;
    private static boolean running;
    private static long startedAt;

    private static final Runnable TICK = new Runnable() {
        @Override
        public void run() {
            if (!running || appContext == null) return;
            tick(appContext);
            HANDLER.postDelayed(this, 1000);
        }
    };

    private ObdEmulator() {
    }

    static void start(Context context) {
        Context app = context.getApplicationContext();
        if (running && appContext == app) return;
        appContext = app;
        running = true;
        startedAt = System.currentTimeMillis();
        AppLog.line(app, "OBD: эмуляция данных включена");
        HANDLER.removeCallbacks(TICK);
        HANDLER.post(TICK);
    }

    static void stop() {
        running = false;
        HANDLER.removeCallbacks(TICK);
    }

    static void tickOnce(Context context) {
        tick(context.getApplicationContext());
    }

    private static void tick(Context context) {
        long elapsed = Math.max(0L, (System.currentTimeMillis() - startedAt) / 1000L);
        double wave = Math.sin(elapsed / 4.0d);
        int speed = Math.max(0, Math.round(52 + (float) (wave * 18d)));
        int rpm = Math.max(700, Math.round(1650 + speed * 22 + (float) (Math.cos(elapsed / 3.0d) * 180d)));
        int coolant = Math.min(96, 72 + (int) (elapsed / 6L));
        int load = Math.max(8, Math.min(92, 28 + (int) Math.round((wave + 1d) * 22d)));
        int throttle = Math.max(5, Math.min(88, 18 + (int) Math.round((Math.sin(elapsed / 2.5d) + 1d) * 18d)));
        float voltage = 13.7f + (float) (Math.sin(elapsed / 8.0d) * 0.2d);
        float fuel = 0.8f + speed / 18f;

        ObdState.emulation(context, speed, rpm, (int) elapsed, voltage, coolant, load, throttle,
                28, fuel, elapsed * speed / 3600d, 128450d + elapsed * speed / 3600d);
    }
}
