package kia.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.Arrays;

final class CanbusFirmwareUpdater {
    interface Listener {
        void onProgress(String text, int percent, boolean done);
    }

    private static final String ACTION_CONFIRMATION = "kia.app.CONFIRMATION";
    private static final int MAX_SIZE = 114688;

    private final Context context;
    private final byte[] data;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();
    private int lastConfirmation = -1;
    private boolean registered;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] frame = intent == null ? null : intent.getByteArrayExtra("confirmation");
            if (frame == null || frame.length < 6) return;
            synchronized (lock) {
                lastConfirmation = frame[5] & 0xff;
                lock.notifyAll();
            }
        }
    };

    CanbusFirmwareUpdater(Context context, byte[] data, Listener listener) {
        this.context = context.getApplicationContext();
        this.data = data == null ? new byte[0] : data;
        this.listener = listener;
    }

    void start() {
        Thread thread = new Thread(this::runUpdate, "canbus-firmware-update");
        thread.start();
    }

    private void runUpdate() {
        if (data.length == 0) {
            post("Файл прошивки пустой", 0, true);
            return;
        }
        if (data.length > MAX_SIZE) {
            post("Файл больше 112 КБ: адаптер его не примет", 0, true);
            return;
        }
        register();
        try {
            post("Подготовка адаптера к прошивке", 0, false);
            boolean started = false;
            for (int attempt = 1; attempt <= 10 && !started; attempt++) {
                resetConfirmation();
                CanbusControl.sendUpdateStart(context);
                post("Запуск режима прошивки, попытка " + attempt, 0, false);
                started = waitFor(1, 1200);
            }
            if (!started) {
                post("Адаптер не подтвердил режим прошивки", 0, true);
                return;
            }

            int blocks = (data.length + 15) / 16;
            for (int i = 0; i < blocks; i++) {
                int offset = i * 16;
                byte[] block = Arrays.copyOfRange(data, offset, Math.min(offset + 16, data.length));
                boolean accepted = false;
                for (int attempt = 1; attempt <= 3 && !accepted; attempt++) {
                    resetConfirmation();
                    CanbusControl.sendUpdateBlock(context, offset, block);
                    accepted = waitFor(2, 1200);
                    if (lastConfirmation == 0) {
                        post("Адаптер отменил прошивку на блоке " + i, percent(i, blocks), true);
                        return;
                    }
                }
                if (!accepted) {
                    post("Нет подтверждения блока " + i, percent(i, blocks), true);
                    return;
                }
                if (i == 0 || i == blocks - 1 || i % 16 == 0) {
                    post("Передано " + (i + 1) + " / " + blocks + " блоков", percent(i + 1, blocks), false);
                }
            }
            resetConfirmation();
            CanbusControl.sendUpdateFinish(context);
            post("Прошивка отправлена, адаптер перезапускается", 100, true);
        } finally {
            unregister();
        }
    }

    private void resetConfirmation() {
        synchronized (lock) {
            lastConfirmation = -1;
        }
    }

    private boolean waitFor(int expected, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        synchronized (lock) {
            while (System.currentTimeMillis() < end) {
                if (lastConfirmation == expected) return true;
                if (lastConfirmation == 0) return false;
                long remaining = end - System.currentTimeMillis();
                if (remaining <= 0) break;
                try {
                    lock.wait(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return lastConfirmation == expected;
        }
    }

    private int percent(int value, int total) {
        if (total <= 0) return 0;
        return Math.max(0, Math.min(100, Math.round(value * 100f / total)));
    }

    private void register() {
        IntentFilter filter = new IntentFilter(ACTION_CONFIRMATION);
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
        registered = true;
    }

    private void unregister() {
        if (!registered) return;
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }
        registered = false;
    }

    private void post(String text, int percent, boolean done) {
        if (listener == null) return;
        handler.post(() -> listener.onProgress(text, percent, done));
    }
}
