package kia.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ObdDashboardView dashboardView;
    private TpmsDashboardView homeTpmsView;
    private TextView usbView;
    private TextView navView;
    private TextView obdView;
    private TextView tpmsView;
    private TextView permissionView;
    private TextView mediaView;
    private TextView logView;
    private ScrollView logScroll;
    private TabletDashboardView tabletDashboardView;
    private Button navModeButton;
    private Button vehicleButton;
    private Button tpmsButton;
    private TextView disabledObdView;
    private FrameLayout settingsPanel;
    private UartOverlayView uartOverlayView;
    private BlindSpotOverlayView blindSpotOverlayView;
    private boolean touched;
    private boolean autoHideLaunch;
    private long lastTpmsOpenAt;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refreshState();
            if (homeTpmsView != null) homeTpmsView.invalidate();
            if (dashboardView != null) dashboardView.invalidate();
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
        autoHideLaunch = isUsbAttachIntent(getIntent());
        if (blockDisabledUsbAutostart(getIntent(), true)) return;
        UiUtils.enterImmersive(this);
        AppPrefs.setObdEmulation(this, false);
        ObdEmulator.stop();
        buildUi();
        hideLegacySettingsPanel();
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
        autoHideLaunch = isUsbAttachIntent(intent);
        if (blockDisabledUsbAutostart(intent, false)) return;
        touched = false;
        AppLog.line(this, "Запуск экрана: " + (intent == null ? "нет intent" : intent.getAction()));
        hideLegacySettingsPanel();
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
        UiUtils.enterImmersive(this);
        updatePermissionText();
        updateToggles();
        hideLegacySettingsPanel();
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

        dashboardView = null;
        homeTpmsView = null;
        disabledObdView = null;
        vehicleButton = null;
        tpmsButton = null;

        tabletDashboardView = new TabletDashboardView(this);
        root.addView(tabletDashboardView, new FrameLayout.LayoutParams(-1, -1));

        uartOverlayView = new UartOverlayView(this);
        uartOverlayView.setVisibility(View.GONE);
        root.addView(uartOverlayView, new FrameLayout.LayoutParams(-1, -1));

        blindSpotOverlayView = new BlindSpotOverlayView(this);
        blindSpotOverlayView.setVisibility(View.GONE);
        root.addView(blindSpotOverlayView, new FrameLayout.LayoutParams(-1, -1));

        settingsPanel = null;
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

    private TextView disabledView() {
        TextView view = new TextView(this);
        view.setText("OBD и TPMS выключены\nОткройте настройки");
        view.setTextColor(0xffd7deea);
        view.setTextSize(28);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(0xff03070c);
        view.setVisibility(View.GONE);
        return view;
    }

    private void buildSettingsPanel() {
        settingsPanel.removeAllViews();
        TextView toolbar = legacyToolbarTitle("Настройки");
        settingsPanel.addView(toolbar, frame(-1, dp(56), 0, 0));

        Button close = legacyToolbarButton("×", v -> toggleSettings());
        FrameLayout.LayoutParams closeLp = frame(dp(64), dp(56), 0, 0);
        closeLp.gravity = Gravity.RIGHT | Gravity.TOP;
        settingsPanel.addView(close, closeLp);

        int left = dp(64);
        int top = dp(108);
        int gap = dp(15);
        int buttonW = dp(300);
        int buttonH = dp(64);

        Button update = legacyButton("Обновить прошивку", v -> startActivity(new Intent(this, CanbusSettingsActivity.class)));
        settingsPanel.addView(update, frame(buttonW, buttonH, left, top));

        Button amp = legacyButton("Настройки AMP", v -> {
            AppLog.line(this, "AMP: приём кадров 0x30 подключён");
            startActivity(new Intent(this, CanbusSettingsActivity.class));
        });
        settingsPanel.addView(amp, frame(buttonW, buttonH, left, top + buttonH + gap));

        Button media = legacyButton("Доступ уведомлений", v -> PermissionHelper.openNotificationListener(this));
        settingsPanel.addView(media, frame(buttonW, buttonH, left, top + (buttonH + gap) * 2));

        RadioGroup speedGroup = new RadioGroup(this);
        speedGroup.setOrientation(RadioGroup.VERTICAL);
        speedGroup.setGravity(Gravity.LEFT);
        RadioButton radioName = legacyRadio("Названия улиц", 0);
        RadioButton radioSpeed = legacyRadio("Ограничение скорости", 1);
        RadioButton radioExceeded = legacyRadio("По превышению", 2);
        speedGroup.addView(radioName);
        speedGroup.addView(radioSpeed);
        speedGroup.addView(radioExceeded);
        speedGroup.check(legacyRadioId(AppPrefs.navTextMode(this)));
        speedGroup.setOnCheckedChangeListener((group, checkedId) -> {
            NavProtocol.setTextMode(this, checkedId - 9000);
            if (navModeButton != null) navModeButton.setText(navModeText());
        });
        settingsPanel.addView(speedGroup, frame(dp(340), dp(124), left, top + (buttonH + gap) * 3));

        int checksTop = top + (buttonH + gap) * 3 + dp(130);
        View tbt = legacyCheck("Тип навигации TBT", AppPrefs.navTbt(this), (button, checked) -> {
            NavProtocol.setTbtMode(this, checked);
        });
        settingsPanel.addView(tbt, frame(dp(360), dp(42), left, checksTop));

        View tempEng = legacyCheck("Температура двигателя", AppPrefs.engineTempEnabled(this), (button, checked) -> {
            AppPrefs.setEngineTempEnabled(this, checked);
            CanbusControl.setEngineTemp(this, checked);
        });
        settingsPanel.addView(tempEng, frame(dp(360), dp(42), left, checksTop + dp(42)));

        View autoHide = legacyCheck("Скрыть через 1-5 секунд", AppPrefs.autoHide(this), (button, checked) -> {
            AppPrefs.setAutoHide(this, checked);
            scheduleAutoHide();
        });
        settingsPanel.addView(autoHide, frame(dp(360), dp(42), left, checksTop + dp(84)));

        SeekBar delay = new SeekBar(this);
        delay.setMax(4);
        delay.setProgress(AppPrefs.autoHideDelaySeconds(this) - 1);
        delay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) AppPrefs.setAutoHideDelaySeconds(MainActivity.this, progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                scheduleAutoHide();
            }
        });
        settingsPanel.addView(delay, frame(buttonW, dp(48), left, checksTop + dp(126)));

        View debug = legacyCheck("Отладка", AppPrefs.debug(this), (button, checked) -> {
            AppPrefs.setDebug(this, checked);
            if (logView != null) logView.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (logScroll != null) logScroll.setVisibility(checked ? View.VISIBLE : View.GONE);
            updatePermissionText();
        });
        settingsPanel.addView(debug, frame(dp(360), dp(42), dp(512), dp(548)));

        permissionView = legacyInfo("");
        settingsPanel.addView(permissionView, frame(dp(720), dp(96), dp(512), top));

        ScrollView scroll = new ScrollView(this);
        logScroll = scroll;
        logView = legacyLog(AppLog.text());
        logView.setVisibility(AppPrefs.debug(this) ? View.VISIBLE : View.GONE);
        scroll.setVisibility(AppPrefs.debug(this) ? View.VISIBLE : View.GONE);
        scroll.addView(logView, new ScrollView.LayoutParams(-1, -2));
        settingsPanel.addView(scroll, frame(dp(720), dp(330), dp(512), top + dp(104)));

        android.widget.EditText angle = new android.widget.EditText(this);
        angle.setText(String.valueOf(AppPrefs.sasRatio(this)));
        angle.setHint("SAS Ratio:");
        angle.setTextColor(0xff171823);
        angle.setHintTextColor(0xff606060);
        angle.setTextSize(18);
        angle.setSingleLine(true);
        angle.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        angle.setGravity(Gravity.CENTER_VERTICAL);
        angle.setBackgroundColor(0xfff3f3f3);
        settingsPanel.addView(angle, frame(dp(150), buttonH, dp(512), dp(648)));

        Button setAngle = legacyButton("Отправить", v -> {
            int ratio = AppPrefs.sasRatio(this);
            try {
                ratio = Integer.parseInt(angle.getText().toString());
            } catch (NumberFormatException ignored) {
            }
            CanbusControl.setSasRatio(this, ratio);
        });
        settingsPanel.addView(setAngle, frame(buttonW, buttonH, dp(687), dp(648)));
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.TOP);
        return view;
    }

    private TextView title(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(0xfff4f7fb);
        v.setTextSize(27);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        v.setSingleLine(false);
        v.setPadding(0, 0, 0, 0);
        return v;
    }

    private TextView legacyToolbarTitle(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(0xffffffff);
        v.setTextSize(20);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        v.setPadding(dp(24), 0, dp(80), 0);
        v.setBackgroundColor(0xff171823);
        return v;
    }

    private Button legacyToolbarButton(String text, View.OnClickListener listener) {
        Button b = legacyButton(text, listener);
        b.setTextSize(26);
        b.setBackgroundColor(0xff171823);
        return b;
    }

    private TextView legacyInfo(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(0xff171823);
        v.setTextSize(16);
        v.setGravity(Gravity.LEFT | Gravity.TOP);
        v.setSingleLine(false);
        v.setPadding(0, 0, 0, 0);
        return v;
    }

    private TextView legacyLog(String text) {
        TextView v = legacyInfo(text);
        v.setTypeface(Typeface.MONOSPACE);
        v.setTextSize(12);
        v.setLineSpacing(dp(2), 1.0f);
        return v;
    }

    private Button legacyButton(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setTextColor(0xffffffff);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundColor(0xff171823);
        b.setOnClickListener(listener);
        return b;
    }

    private RadioButton legacyRadio(String text, int mode) {
        RadioButton radio = new RadioButton(this);
        radio.setId(legacyRadioId(mode));
        radio.setText(text);
        radio.setTextColor(0xff171823);
        radio.setTextSize(16);
        radio.setGravity(Gravity.CENTER_VERTICAL);
        radio.setMinHeight(dp(36));
        return radio;
    }

    private int legacyRadioId(int mode) {
        return 9000 + mode;
    }

    private View legacyCheck(String text, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        return switchRow(text, checked, 0xff171823, 16, listener);
    }

    private FrameLayout.LayoutParams frame(int width, int height, int left, int top) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
        lp.leftMargin = left;
        lp.topMargin = top;
        return lp;
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(0xffcbd5df);
        v.setTextSize(15);
        v.setSingleLine(false);
        v.setPadding(0, dp(4), 0, dp(4));
        return v;
    }

    private TextView section(String text) {
        TextView v = label(text);
        v.setTextColor(0xff70e8ff);
        v.setTextSize(13);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.LEFT | Gravity.BOTTOM);
        v.setPadding(0, dp(4), 0, dp(2));
        return v;
    }

    private TextView statusRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(0xff102333);
        row.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(40));
        rowLp.setMargins(0, 0, 0, dp(5));
        parent.addView(row, rowLp);

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(0xff7fa6b8);
        name.setTextSize(14);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        row.addView(name, new LinearLayout.LayoutParams(dp(120), -1));

        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(0xffffffff);
        text.setTextSize(13);
        text.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        text.setSingleLine(true);
        row.addView(text, new LinearLayout.LayoutParams(0, -1, 1));
        return text;
    }

    private TextView card(String text) {
        TextView v = label(text);
        v.setTextColor(0xfff7fafc);
        v.setTextSize(17);
        v.setBackgroundColor(0xff102333);
        v.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, 0);
        v.setLayoutParams(lp);
        return v;
    }

    private Button glassButton(String text, int sp) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(sp);
        b.setTextColor(0xffffffff);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackgroundColor(0xaa0b4f82);
        return b;
    }

    private Button smallButton(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTextColor(0xffffffff);
        b.setAllCaps(false);
        b.setBackgroundColor(0xff105c73);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(40));
        lp.setMargins(0, dp(4), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private Button smallSquareButton(String text, View.OnClickListener listener) {
        Button b = smallButton(text, listener);
        b.setTextSize(22);
        b.setPadding(0, 0, 0, dp(2));
        return b;
    }

    private void action(GridLayout grid, String text, View.OnClickListener listener) {
        Button b = smallButton(text, listener);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(46);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(b, lp);
    }

    private View check(String text, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        return switchRow(text, checked, 0xffe7edf3, 15, listener);
    }

    private View switchRow(String text, boolean checked, int textColor, int textSize, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(textColor);
        label.setTextSize(textSize);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setSingleLine(false);
        row.addView(label, new LinearLayout.LayoutParams(0, dp(40), 1));

        Switch toggle = new Switch(this);
        toggle.setShowText(false);
        toggle.setText(null);
        toggle.setMinWidth(dp(58));
        tintSwitch(toggle);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener(listener);
        row.addView(toggle, new LinearLayout.LayoutParams(dp(74), dp(40)));
        label.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        return row;
    }

    private void tintSwitch(Switch toggle) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        toggle.setThumbTintList(new ColorStateList(states, new int[]{0xffffffff, 0xffffffff}));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{0xff19c58b, 0xff858b96}));
    }

    private void toggleSettings() {
        settingsPanel.setVisibility(settingsPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        updatePermissionText();
        refreshState();
    }

    private void hideLegacySettingsPanel() {
        if (settingsPanel != null) settingsPanel.setVisibility(View.GONE);
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
        boolean runtime = runtimePermissionsOk();
        boolean notifications = PermissionHelper.hasNotificationAccess(this);
        String text = "Разрешения: Android " + yes(runtime)
                + "  |  уведомления " + yes(notifications)
                + "  |  USB/CAN авто\n"
                + cleanStatusLine(AppLog.usb()) + "\n"
                + AppLog.media() + "\n"
                + AppLog.nav() + "\n"
                + cleanStatusLine(VehicleDisplayState.snapshot().status) + "\n"
                + cleanStatusLine(TpmsState.snapshot().status);
        if (permissionView != null) permissionView.setText(text);
        if (logView != null) logView.setText(AppLog.text());
    }

    private void updateToggles() {
        if (navModeButton != null) navModeButton.setText(navModeText());
    }

    private void refreshState() {
        VehicleDisplayState.Snapshot vehicle = VehicleDisplayState.snapshot();
        TpmsState.Snapshot tpms = TpmsState.snapshot();
        if (usbView != null) usbView.setText(AppLog.usb());
        if (navView != null) navView.setText(AppLog.nav());
        if (mediaView != null) mediaView.setText(AppLog.media());
        if (obdView != null) obdView.setText(cleanStatusLine(vehicle.status));
        if (tpmsView != null) tpmsView.setText(cleanStatusLine(tpms.status));
        if (logView != null) logView.setText(AppLog.text());
        if (tabletDashboardView != null) tabletDashboardView.refresh();
        if (dashboardView != null && AppPrefs.obdEnabled(this)) dashboardView.setSnapshot(vehicle);
        if (homeTpmsView != null) homeTpmsView.setSnapshot(tpms);
        refreshSectionVisibility();
    }

    private void refreshSectionVisibility() {
        boolean obd = AppPrefs.obdEnabled(this);
        boolean tpms = AppPrefs.tpmsEnabled(this);
        boolean tpmsAsHome = !obd && tpms;
        if (dashboardView != null) dashboardView.setVisibility(obd ? View.VISIBLE : View.GONE);
        if (homeTpmsView != null) homeTpmsView.setVisibility(tpmsAsHome ? View.VISIBLE : View.GONE);
        if (disabledObdView != null) disabledObdView.setVisibility(!obd && !tpms ? View.VISIBLE : View.GONE);
        if (vehicleButton != null) vehicleButton.setVisibility(obd ? View.VISIBLE : View.GONE);
        if (tpmsButton != null) tpmsButton.setVisibility(tpms && obd ? View.VISIBLE : View.GONE);
        if (tabletDashboardView != null) tabletDashboardView.refresh();
        if (uartOverlayView != null) {
            uartOverlayView.setVisibility(View.GONE);
        }
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
        String usb = intent.getStringExtra(AppLog.EXTRA_USB);
        String nav = intent.getStringExtra(AppLog.EXTRA_NAV);
        String media = intent.getStringExtra(AppLog.EXTRA_MEDIA);
        String log = intent.getStringExtra(AppLog.EXTRA_LOG);
        if (usbView != null && !TextUtils.isEmpty(usb)) usbView.setText(usb);
        if (navView != null && !TextUtils.isEmpty(nav)) navView.setText(nav);
        if (mediaView != null && !TextUtils.isEmpty(media)) mediaView.setText(media);
        if (logView != null && log != null) logView.setText(log);
        refreshState();
        updatePermissionText();
    }

    private void openTpmsOnAlert() {
        if (!AppPrefs.tpmsEnabled(this)) return;
        if (!AppPrefs.tpmsAutoOpen(this)) return;
        if (TpmsAlertManager.isSuppressed(this)) return;
        TpmsState.Snapshot snapshot = TpmsState.snapshot();
        if (snapshot == null || snapshot.tires == null) return;
        boolean alert = false;
        for (TpmsState.Tire tire : snapshot.tires) {
            if (tire != null && tire.alert()) {
                alert = true;
                break;
            }
        }
        long now = System.currentTimeMillis();
        if (alert && now - lastTpmsOpenAt > 6000L) {
            lastTpmsOpenAt = now;
            startActivity(new Intent(this, TpmsActivity.class).putExtra("tpms_alert", true));
        }
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

    private String navModeText() {
        int mode = AppPrefs.navTextMode(this);
        if (mode == 1) return "Текст навигации: ограничение скорости";
        if (mode == 2) return "Текст навигации: по превышению";
        return "Текст навигации: названия улиц";
    }

    private String yes(boolean value) {
        return value ? "да" : "нет";
    }

    private String cleanStatusLine(String value) {
        if (value == null) return "";
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("адаптер не подключ") || lower.contains("адаптер не найден")) return "";
        if (lower.contains("usb-датчик не найден") || lower.contains("usb-адаптер не найден")) return "";
        return value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
