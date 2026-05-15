package kia.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TabletDashboardView tabletDashboardView;
    private BlindSpotOverlayView blindSpotOverlayView;
    private boolean touched;
    private boolean autoHideLaunch;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refreshState();
            handler.postDelayed(this, 1000);
        }
    };

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            applyState(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPrefs.applyDefaultProfileIfNeeded(this);
        autoHideLaunch = isUsbAttachIntent(getIntent());
        if (blockDisabledUsbAutostart(getIntent(), true)) return;
        UiUtils.enterImmersive(this);
        AppPrefs.setObdEmulation(this, false);
        ObdEmulator.stop();
        buildUi();
        AppService.start(this);
        ObdMonitor.start(this);
        TpmsMonitor.start(this);
        AppUpdater.checkOnLaunch(this);
        requestNextRuntimePermission();
        updatePermissionText();
        refreshState();
        scheduleAutoHide();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppPrefs.applyDefaultProfileIfNeeded(this);
        autoHideLaunch = isUsbAttachIntent(intent);
        if (blockDisabledUsbAutostart(intent, false)) return;
        touched = false;
        AppLog.line(this, "Запуск экрана: " + (intent == null ? "нет intent" : intent.getAction()));
        AppService.start(this);
        ObdMonitor.start(this);
        TpmsMonitor.start(this);
        scheduleAutoHide();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(AppLog.ACTION_STATE);
        filter.addAction(ObdState.ACTION_STATE);
        filter.addAction(VehicleDisplayState.ACTION_STATE);
        filter.addAction(TpmsState.ACTION_STATE);
        filter.addAction(CanbusControl.ACTION_STATE);
        filter.addAction(BlindSpotState.ACTION_STATE);
        filter.addAction(AppUpdater.ACTION_STATE);
        filter.addAction(FirmwareReleaseUpdater.ACTION_STATE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CompassBridge.start(this);
        CompassBridge.markAppResumed();
        UiUtils.enterImmersive(this);
        updatePermissionText();
        MediaMonitor.scanNow(this);
        AppService.refreshOverlays(this);
        ObdMonitor.start(this);
        TpmsMonitor.start(this);
        refreshState();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) UiUtils.enterImmersive(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(tick);
        try {
            unregisterReceiver(stateReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN) touched = true;
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQ_RUNTIME) {
            String name = permissions != null && permissions.length > 0 ? permissions[0] : "?";
            boolean ok = grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            AppLog.line(this, "Разрешение " + name + ": " + (ok ? "да" : "нет"));
            updatePermissionText();
            requestNextRuntimePermission();
            ObdMonitor.start(this);
            TpmsMonitor.start(this);
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff111317);

        tabletDashboardView = new TabletDashboardView(this);
        root.addView(tabletDashboardView, new FrameLayout.LayoutParams(-1, -1));

        blindSpotOverlayView = new BlindSpotOverlayView(this);
        blindSpotOverlayView.setVisibility(View.GONE);
        root.addView(blindSpotOverlayView, new FrameLayout.LayoutParams(-1, -1));

        setContentView(root);
        refreshSectionVisibility();
    }

    private boolean blockDisabledUsbAutostart(Intent intent, boolean finishScreen) {
        if (intent == null) return false;
        if (!isUsbAttachIntent(intent)) return false;
        if (AppPrefs.autoStart(this) && AppPrefs.backgroundAutoStart(this)) {
            AppLog.line(this, "USB автозапуск: фоновый режим без открытия экрана");
            AppService.start(this);
            if (finishScreen) finish();
            return true;
        }
        if (AppPrefs.autoStart(this)) return false;
        AppLog.line(this, "USB автозапуск: выключен в настройках");
        if (finishScreen) finish();
        return true;
    }

    private void requestNextRuntimePermission() {
        if (Build.VERSION.SDK_INT < 23) return;
        for (String permission : PermissionHelper.runtimePermissions()) {
            if (!PermissionHelper.shouldRequestRuntime(this, permission)) continue;
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, PermissionHelper.REQ_RUNTIME);
                return;
            }
        }
    }

    private boolean runtimePermissionsOk() {
        if (Build.VERSION.SDK_INT < 23) return true;
        for (String permission : PermissionHelper.runtimePermissions()) {
            if (!PermissionHelper.shouldRequestRuntime(this, permission)) continue;
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private void updatePermissionText() {
        // The current tablet dashboard owns all visible status rows.
    }

    private void refreshState() {
        if (tabletDashboardView != null) tabletDashboardView.refresh();
        refreshSectionVisibility();
    }

    private void refreshSectionVisibility() {
        if (tabletDashboardView != null) tabletDashboardView.refresh();
        if (blindSpotOverlayView != null) {
            boolean active = AppPrefs.blindSpotEnabled(this) && BlindSpotState.snapshot().active();
            blindSpotOverlayView.setVisibility(active ? View.VISIBLE : View.GONE);
            if (active) blindSpotOverlayView.invalidate();
        }
    }

    private void applyState(Intent intent) {
        if (intent == null) {
            refreshState();
            return;
        }
        if (TpmsState.ACTION_STATE.equals(intent.getAction())) {
            openTpmsOnAlert();
        }
        refreshState();
        updatePermissionText();
    }

    private void openTpmsOnAlert() {
        if (!AppPrefs.tpmsEnabled(this)) return;
        if (!AppPrefs.tpmsAlertOverlay(this)) return;
        if (TpmsAlertManager.isSuppressed(this)) return;
        if (TpmsAlertManager.alertMessage(TpmsState.snapshot()).length() == 0) return;
        AppService.refreshOverlays(this);
    }

    private void scheduleAutoHide() {
        handler.removeCallbacksAndMessages(null);
        handler.post(tick);
        if (!AppPrefs.autoHide(this)) return;
        if (!autoHideLaunch) return;
        handler.postDelayed(() -> {
            boolean permissionsDone = runtimePermissionsOk() && PermissionHelper.hasNotificationAccess(this);
            if (!touched && permissionsDone) {
                AppLog.line(this, "Автосворачивание");
                moveTaskToBack(true);
            } else {
                updatePermissionText();
            }
        }, AppPrefs.autoHideDelaySeconds(this) * 1000L);
    }

    private boolean isUsbAttachIntent(Intent intent) {
        return intent != null && "android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(intent.getAction());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
