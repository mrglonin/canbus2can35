package kia.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

public class CanbusSettingsActivity extends Activity {
    private static final int REQ_FIRMWARE = 4410;
    private static final int TAB_OBD = 0;
    private static final int TAB_TPMS = 1;
    private static final int TAB_AMP = 2;
    private static final int TAB_CANBUS = 3;
    private static final int TAB_SETTINGS = 4;

    private final TextView[] tabs = new TextView[5];
    private final TextView[] tireValues = new TextView[4];
    private final TextView[] tireStates = new TextView[4];
    private final ArrayList<TextView> navAdapterRows = new ArrayList<>();
    private LinearLayout content;
    private int currentTab = TAB_OBD;

    private TextView statusValue;
    private TextView uidValue;
    private TextView versionValue;
    private TextView v20StatusValue;
    private TextView v20ApiValue;
    private TextView v20CapsValue;
    private TextView sourceValue;
    private TextView updateValue;
    private TextView vehicleStatusValue;
    private TextView blindSpotStatusValue;
    private TextView mediaValue;
    private TextView mediaPreviewValue;
    private TextView mediaClusterPreviewValue;
    private Button mediaFormatButton;
    private TextView usbValue;
    private TextView navValue;
    private TextView navDebugValue;
    private TextView runtimePermissionStatus;
    private TextView notificationPermissionStatus;
    private TextView overlayPermissionStatus;
    private TextView logValue;
    private TextView tpmsStatusValue;
    private TextView appVersionValue;
    private TextView appUpdateStatusValue;
    private TextView appUpdateReleaseValue;
    private TextView firmwareReleaseStatusValue;
    private TextView firmwareReleaseAssetValue;
    private Button appUpdateInstallButton;
    private Button firmwareReleaseFlashButton;
    private TextView tpmsLowValue;
    private TextView tpmsHighValue;
    private TextView ampFaderValue;
    private TextView ampBalanceValue;
    private TextView ampBassValue;
    private TextView ampMidValue;
    private TextView ampTrebleValue;
    private TextView ampVolumeValue;
    private TextView ampModeValue;
    private TextView speedMetric;
    private TextView rpmMetric;
    private TextView voltageMetric;
    private TextView tempMetric;
    private TextView runtimeMetric;
    private TextView canDebugStatusValue;
    private TextView canLogPreviewValue;
    private Button canRecordButton;
    private Button canModeButton;
    private Button speedButton;
    private Button tempButton;
    private Button navModeButton;
    private Button runtimePermissionButton;
    private Button notificationPermissionButton;
    private Button overlayPermissionButton;
    private EditText sasEdit;
    private ProgressBar progressBar;
    private int ampFader = 10;
    private int ampBalance = 10;
    private int ampBass = 10;
    private int ampMid = 10;
    private int ampTreble = 10;
    private int ampVolume = 6;
    private int ampMode = 2;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && CanbusControl.ACTION_FRAME_RECEIVED.equals(intent.getAction())) {
                parseAmpFrame(intent.getByteArrayExtra("frame"));
            }
            refresh();
            if (intent != null && TpmsState.ACTION_STATE.equals(intent.getAction())) {
                openTpmsOnAlert();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPrefs.applyDefaultProfileIfNeeded(this);
        UiUtils.enterImmersive(this);
        buildUi();
        AppService.start(this);
        CanbusControl.requestAdapterInfo(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(CanbusControl.ACTION_STATE);
        filter.addAction(VehicleDisplayState.ACTION_STATE);
        filter.addAction(TpmsState.ACTION_STATE);
        filter.addAction(AppLog.ACTION_STATE);
        filter.addAction(NavDebugState.ACTION_STATE);
        filter.addAction(SidebandDebugState.ACTION_STATE);
        filter.addAction(BlindSpotState.ACTION_STATE);
        filter.addAction(AppUpdater.ACTION_STATE);
        filter.addAction(FirmwareReleaseUpdater.ACTION_STATE);
        filter.addAction(CanbusControl.ACTION_FRAME_RECEIVED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiUtils.enterImmersive(this);
        refresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) UiUtils.enterImmersive(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FIRMWARE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            startFirmwareUpdate(data.getData());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQ_RUNTIME) refresh();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        boolean compact = compactUi();
        root.setBackgroundColor(0xff111317);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(0xff181b20);
        top.setPadding(dp(compact ? 8 : 18), 0, dp(compact ? 8 : 18), 0);
        page.addView(top, new LinearLayout.LayoutParams(-1, dp(compact ? 68 : 82)));

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setGravity(Gravity.CENTER_VERTICAL);
        tabRow.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams tabsLp = new LinearLayout.LayoutParams(0, dp(compact ? 44 : 48), 1);
        tabsLp.setMargins(0, 0, dp(compact ? 8 : 16), 0);
        top.addView(tabRow, tabsLp);
        addTab(tabRow, TAB_OBD, "OBD");
        addTab(tabRow, TAB_TPMS, "TPMS");
        addTab(tabRow, TAB_AMP, "AMP");
        addTab(tabRow, TAB_CANBUS, "CANBUS");
        addTab(tabRow, TAB_SETTINGS, "Настройки");

        Button close = iconButton("×", v -> closeSettings());
        top.addView(close, new LinearLayout.LayoutParams(dp(compact ? 48 : 56), dp(compact ? 48 : 56)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(compact ? 10 : 28), dp(compact ? 12 : 22),
                dp(compact ? 10 : 28), dp(compact ? 18 : 28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        setContentView(root);
        selectTab(firstAvailableTab());
    }

    @Override
    public void onBackPressed() {
        closeSettings();
    }

    private void addTab(LinearLayout parent, int id, String text) {
        TextView tab = new TextView(this);
        tab.setText(text);
        tab.setTextSize(compactUi() ? 11 : 14);
        tab.setTypeface(Typeface.DEFAULT_BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine(true);
        tab.setClickable(true);
        tab.setOnClickListener(v -> selectTab(id));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(compactUi() ? 40 : 46), 1);
        lp.setMargins(dp(compactUi() ? 2 : 3), 0, dp(compactUi() ? 2 : 3), 0);
        parent.addView(tab, lp);
        tabs[id] = tab;
    }

    private void selectTab(int tab) {
        currentTab = normalizeTab(tab);
        updateTabs();
        buildTabContent();
        onTabOpened(currentTab);
        refresh();
    }

    private void onTabOpened(int tab) {
        if (tab == TAB_CANBUS) {
            AppService.start(this);
            ObdMonitor.restart(this);
            CanbusControl.requestAdapterInfo(this);
            CanbusControl.requestV20Status(this);
        } else if (tab == TAB_AMP) {
            CanbusControl.requestAmpSettings(this);
        } else if (tab == TAB_SETTINGS) {
            MediaMonitor.scanNow(this);
            AppUpdater.checkOnLaunch(this);
        }
    }

    private void updateTabs() {
        for (int i = 0; i < tabs.length; i++) {
            TextView tab = tabs[i];
            if (tab == null) continue;
            boolean enabled = isTabAvailable(i);
            tab.setVisibility(enabled ? View.VISIBLE : View.GONE);
            boolean selected = i == currentTab;
            tab.setTextColor(selected ? 0xff14171c : 0xffd8dde6);
            tab.setBackground(roundedStroke(selected ? 0xfff2b84b : 0xff252a31, 8,
                    selected ? 0xfff2b84b : 0xff333842));
        }
    }

    private int normalizeTab(int tab) {
        return isTabAvailable(tab) ? tab : firstAvailableTab();
    }

    private int firstAvailableTab() {
        if (AppPrefs.obdEnabled(this)) return TAB_OBD;
        if (AppPrefs.tpmsEnabled(this)) return TAB_TPMS;
        return TAB_SETTINGS;
    }

    private boolean isTabAvailable(int tab) {
        if (tab == TAB_OBD) return AppPrefs.obdEnabled(this);
        if (tab == TAB_TPMS) return AppPrefs.tpmsEnabled(this);
        return true;
    }

    private void buildTabContent() {
        if (content == null) return;
        content.removeAllViews();
        resetDynamicViews();
        switch (currentTab) {
            case TAB_TPMS:
                buildTpmsTab();
                break;
            case TAB_AMP:
                buildAmpTab();
                break;
            case TAB_CANBUS:
                buildCanbusTab();
                break;
            case TAB_SETTINGS:
                buildSettingsTab();
                break;
            case TAB_OBD:
            default:
                buildObdTab();
                break;
        }
    }

    private void resetDynamicViews() {
        statusValue = null;
        uidValue = null;
        versionValue = null;
        v20StatusValue = null;
        v20ApiValue = null;
        v20CapsValue = null;
        sourceValue = null;
        updateValue = null;
        vehicleStatusValue = null;
        blindSpotStatusValue = null;
        mediaValue = null;
        mediaPreviewValue = null;
        mediaClusterPreviewValue = null;
        usbValue = null;
        navValue = null;
        navDebugValue = null;
        runtimePermissionStatus = null;
        notificationPermissionStatus = null;
        overlayPermissionStatus = null;
        logValue = null;
        tpmsStatusValue = null;
        appVersionValue = null;
        appUpdateStatusValue = null;
        appUpdateReleaseValue = null;
        firmwareReleaseStatusValue = null;
        firmwareReleaseAssetValue = null;
        appUpdateInstallButton = null;
        firmwareReleaseFlashButton = null;
        tpmsLowValue = null;
        tpmsHighValue = null;
        ampFaderValue = null;
        ampBalanceValue = null;
        ampBassValue = null;
        ampMidValue = null;
        ampTrebleValue = null;
        ampVolumeValue = null;
        ampModeValue = null;
        speedMetric = null;
        rpmMetric = null;
        voltageMetric = null;
        tempMetric = null;
        runtimeMetric = null;
        canDebugStatusValue = null;
        canLogPreviewValue = null;
        canRecordButton = null;
        canModeButton = null;
        speedButton = null;
        tempButton = null;
        navModeButton = null;
        runtimePermissionButton = null;
        notificationPermissionButton = null;
        overlayPermissionButton = null;
        sasEdit = null;
        progressBar = null;
        navAdapterRows.clear();
        for (int i = 0; i < tireValues.length; i++) {
            tireValues[i] = null;
            tireStates[i] = null;
        }
    }

    private void buildObdTab() {
        sectionHeader("Настройки OBD", "Источник данных и единицы отображения.");

        GridLayout metrics = grid(3);
        content.addView(metrics, matchWrap());
        speedMetric = metric(metrics, "Скорость", "--");
        rpmMetric = metric(metrics, "Обороты", "--");
        voltageMetric = metric(metrics, "Напряжение", "--");
        tempMetric = metric(metrics, "Охлаждение", "--");
        runtimeMetric = metric(metrics, "Время пути", "--");

        LinearLayout display = card();
        content.addView(display, cardLp());
        addCardTitle(display, "Параметры отображения OBD");
        GridLayout buttons = grid(2);
        display.addView(buttons, matchWrap());
        speedButton = gridButton(buttons, "", v -> {
            AppPrefs.setSpeedUnit(this, AppPrefs.speedUnit(this) == 0 ? 1 : 0);
            VehicleDisplayState.updateFromObd(this, ObdState.snapshot());
            savedToast();
            refresh();
        });
        tempButton = gridButton(buttons, "", v -> {
            AppPrefs.setTempUnit(this, AppPrefs.tempUnit(this) == 0 ? 1 : 0);
            VehicleDisplayState.updateFromObd(this, ObdState.snapshot());
            savedToast();
            refresh();
        });
    }

    private void buildTpmsTab() {
        sectionHeader("Настройки TPMS", "Пороги давления, уведомление поверх экрана и звук до закрытия.");

        LinearLayout alerts = card();
        content.addView(alerts, cardLp());
        addCardTitle(alerts, "Предупреждения давления");
        tpmsLowValue = thresholdRow(alerts, "Низкое давление", AppPrefs.tpmsLowBar(this), -0.1f, 0.1f, true);
        tpmsHighValue = thresholdRow(alerts, "Высокое давление", AppPrefs.tpmsHighBar(this), -0.1f, 0.1f, false);
        alerts.addView(check("Показывать уведомление о давлении", AppPrefs.tpmsAlertOverlay(this), (button, checked) -> {
            AppPrefs.setTpmsAlertOverlay(this, checked);
            AppService.refreshOverlays(this);
            AppLog.line(this, "TPMS: уведомление " + yes(checked));
            savedToast();
        }));
        alerts.addView(check("Звук до закрытия уведомления", AppPrefs.tpmsAlertSound(this), (button, checked) -> {
            AppPrefs.setTpmsAlertSound(this, checked);
            AppLog.line(this, "TPMS: звук предупреждения " + yes(checked));
            savedToast();
        }));
    }

    private void buildAmpTab() {
        sectionHeader("AMP", "Штатные настройки усилителя Kia через CAN кадры 0x30.");

        LinearLayout controls = card();
        content.addView(controls, cardLp());
        addCardTitle(controls, "Параметры AMP");
        CanbusControl.requestAmpSettings(this);
        Toast.makeText(this, "AMP: настройки прочитаны", Toast.LENGTH_SHORT).show();
        ampFaderValue = ampRow(controls, "Fader", "R", "F", () -> changeAmp("fader", -1), () -> changeAmp("fader", 1));
        ampBalanceValue = ampRow(controls, "Balance", "L", "R", () -> changeAmp("balance", -1), () -> changeAmp("balance", 1));
        ampBassValue = ampRow(controls, "Bass", "−", "+", () -> changeAmp("bass", -1), () -> changeAmp("bass", 1));
        ampMidValue = ampRow(controls, "Mid", "−", "+", () -> changeAmp("mid", -1), () -> changeAmp("mid", 1));
        ampTrebleValue = ampRow(controls, "Treble", "−", "+", () -> changeAmp("treble", -1), () -> changeAmp("treble", 1));
        ampVolumeValue = ampRow(controls, "Volume", "−", "+", () -> changeAmp("volume", -1), () -> changeAmp("volume", 1));
        ampModeValue = ampRow(controls, "AMP Mode", "−", "+", () -> changeAmp("mode", -1), () -> changeAmp("mode", 1));

        LinearLayout notes = card();
        content.addView(notes, cardLp());
        addCardTitle(notes, "Примечание");
        TextView text = bodyText("При открытии вкладки приложение запрашивает текущие настройки усилителя. Любое изменение сразу отправляет и сохраняет кадр AMP 0x30/0x00. Известные режимы: 10 - по умолчанию, 02 - штатная магнитола, 16 - большинство китайских адаптеров.");
        notes.addView(text);
    }

    private void buildCanbusTab() {
        sectionHeader("CANBUS", "Адаптер, навигация, RCTA и прошивка CAN адаптера.");

        LinearLayout state = card();
        content.addView(state, cardLp());
        addCardTitle(state, "Состояние адаптера");
        statusValue = infoRow(state, "CAN", "");
        uidValue = infoRow(state, "UID", "");
        versionValue = infoRow(state, "Версия", "");
        v20StatusValue = infoRow(state, "V21", "");
        v20ApiValue = infoRow(state, "API", "");
        v20CapsValue = infoRow(state, "Caps", "");
        updateValue = infoRow(state, "Прошивка", "");

        GridLayout actions = grid(2);
        state.addView(actions, matchWrap());
        gridButton(actions, "Обновить статус", v -> {
            AppService.start(this);
            ObdMonitor.restart(this);
            CanbusControl.requestAdapterInfo(this);
            CanbusControl.requestV20Status(this);
        });

        LinearLayout firmware = card();
        content.addView(firmware, cardLp());
        addCardTitle(firmware, "Прошивка CAN адаптера");
        TextView firmwareText = bodyText("Приложение берёт BIN из git-манифеста и запускает штатный USB update. Кнопка прошивки включается только когда обновление найдено или файл уже скачан.");
        firmwareText.setTextColor(0xffa4abb6);
        firmware.addView(firmwareText, matchWrap());
        firmwareReleaseStatusValue = infoRow(firmware, "GitHub", "");
        firmwareReleaseAssetValue = infoRow(firmware, "BIN", "");
        GridLayout releaseActions = grid(2);
        firmware.addView(releaseActions, matchWrap());
        gridButton(releaseActions, "Проверить обновление", v -> FirmwareReleaseUpdater.checkNow(this));
        firmwareReleaseFlashButton = gridButton(releaseActions, "Скачать и прошить", v -> FirmwareReleaseUpdater.downloadAndFlash(this));
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        firmware.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(32)));

        LinearLayout nav = card();
        content.addView(nav, cardLp());
        addCardTitle(nav, "Навигация в адаптер");
        TextView navText = bodyText("Старт навигатора сразу отправляет source/on и текст ожидания. При активном маршруте последние валидные 0x48/0x45/0x47/0x4A/0x44 повторяются раз в секунду, финиш отправляется отдельным флагом.");
        navText.setTextColor(0xff3a414b);
        nav.addView(navText, matchWrap());
        GridLayout navActions = grid(2);
        nav.addView(navActions, matchWrap());
        navModeButton = gridButton(navActions, "", v -> {
            int next = (AppPrefs.navTextMode(this) + 1) % 3;
            NavProtocol.setTextMode(this, next);
            savedToast();
            refresh();
        });
        nav.addView(check("TBT-иконки манёвров", AppPrefs.navTbt(this), (button, checked) -> {
            NavProtocol.setTbtMode(this, checked);
            savedToast();
            refresh();
        }));
        nav.addView(check("Компас когда маршрута нет", AppPrefs.navCompass(this), (button, checked) -> {
            AppPrefs.setNavCompass(this, checked);
            if (checked) AppService.start(this);
            CompassBridge.refresh(this);
            AppLog.line(this, "Навигация: компас без маршрута " + yes(checked));
            savedToast();
            refresh();
        }));
        TextView limitHint = bodyText("Ограничение скорости берётся из TEYES/Yandex/2GIS broadcast extras: speed_limit/speedLimit/limit/max_speed/roadLimit/cameraSpeed, а также из текста с km/h или км/ч.");
        limitHint.setTextColor(0xff6d7280);
        nav.addView(limitHint, matchWrap());
        addNavAdapterTable(nav);

        LinearLayout blindSpot = card();
        content.addView(blindSpot, cardLp());
        addCardTitle(blindSpot, "Слепые зоны / RCTA");
        blindSpotStatusValue = infoRow(blindSpot, "Состояние", "");
        blindSpot.addView(check("RCTA / слепые зоны", AppPrefs.blindSpotEnabled(this), (button, checked) -> {
            AppPrefs.setBlindSpotEnabled(this, checked);
            if (checked) AppService.start(this);
            else if (!AppPrefs.debugCan(this)) CanbusControl.stopCanStream(this);
            AppLog.line(this, "RCTA: система " + yes(checked));
            AppService.refreshOverlays(this);
            savedToast();
            refresh();
        }));
        blindSpot.addView(check("Показывать предупреждение RCTA", AppPrefs.blindSpotOverlay(this), (button, checked) -> {
            AppPrefs.setBlindSpotOverlay(this, checked);
            if (checked && !PermissionHelper.canDrawOverlays(this)) {
                Toast.makeText(this, "Разрешите показ поверх других окон", Toast.LENGTH_SHORT).show();
                PermissionHelper.openOverlaySettings(this);
            }
            AppService.refreshOverlays(this);
            savedToast();
            refresh();
        }));

        if (AppPrefs.debugCan(this)) {
            LinearLayout sas = card();
            content.addView(sas, cardLp());
            addCardTitle(sas, "Калибровка угла руля");
            TextView hint = bodyText("Сервисная настройка адаптера для случая, когда угол руля или парковочные линии смещены. В обычном режиме скрыта.");
            hint.setTextColor(0xff6d7280);
            sas.addView(hint, matchWrap());
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            sas.addView(row, new LinearLayout.LayoutParams(-1, dp(64)));

            sasEdit = new EditText(this);
            sasEdit.setTextColor(0xff10131f);
            sasEdit.setTextSize(22);
            sasEdit.setTypeface(Typeface.DEFAULT_BOLD);
            sasEdit.setSingleLine(true);
            sasEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
            sasEdit.setGravity(Gravity.CENTER);
            sasEdit.setBackground(roundedStroke(0xffffffff, 8, 0xffd6dce8));
            row.addView(sasEdit, new LinearLayout.LayoutParams(dp(130), dp(52)));

            Button send = button("Отправить", 0xff151928, v -> sendSas());
            LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(0, dp(52), 1);
            sendLp.setMargins(dp(12), 0, 0, 0);
            row.addView(send, sendLp);
        }
    }

    private void buildSettingsTab() {
        sectionHeader("Настройки", "Мультимедиа, разрешения, автозапуск, обновления и CAN log.");

        LinearLayout media = card();
        content.addView(media, cardLp());
        addCardTitle(media, "Мультимедиа");
        mediaValue = infoRow(media, "Сейчас", "");
        mediaPreviewValue = infoRow(media, "В приложении", "");
        mediaClusterPreviewValue = infoRow(media, "В приборку", "");
        TextView mediaHint = bodyText("Формат применяется ко всем источникам: USB, Bluetooth, Яндекс/облако, FM и AM. При смене формата ниже сразу видно итоговую строку.");
        mediaHint.setTextColor(0xff6d7280);
        media.addView(mediaHint, matchWrap());
        GridLayout mediaActions = grid(2);
        media.addView(mediaActions, new LinearLayout.LayoutParams(-1, -2));
        mediaFormatButton = gridButton(mediaActions, "", v -> {
            AppPrefs.setMediaTextFormat(this, (AppPrefs.mediaTextFormat(this) + 1) % 5);
            AppLog.line(this, "Мультимедиа: формат строки " + AppPrefs.mediaTextFormatLabel(this));
            refresh();
        });
        gridButton(mediaActions, "Обновить данные", v -> {
            MediaMonitor.scanNow(this);
            refresh();
        });

        LinearLayout app = card();
        content.addView(app, cardLp());
        addCardTitle(app, "Приложение");
        appVersionValue = infoRow(app, "Версия", "");
        usbValue = infoRow(app, "USB", "");
        navValue = infoRow(app, "Навигация", "");
        navDebugValue = bodyText("");
        navDebugValue.setTypeface(Typeface.MONOSPACE);
        navDebugValue.setTextSize(12);
        navDebugValue.setPadding(dp(12), dp(10), dp(12), dp(10));
        navDebugValue.setBackground(rounded(0xfff7f9fc, 8));
        app.addView(navDebugValue, new LinearLayout.LayoutParams(-1, dp(230)));
        app.addView(check("Включить раздел OBD", AppPrefs.obdEnabled(this), (button, checked) -> {
            AppPrefs.setObdEnabled(this, checked);
            if (checked) {
                ObdMonitor.restart(this);
                VehicleDisplayState.updateFromObd(this, ObdState.snapshot());
            } else {
                ObdMonitor.stop(this);
                ObdEmulator.stop();
                ObdState.status(this, "OBD: раздел выключен в настройках", false);
                if (AppPrefs.debugCan(this)) CanbusControl.startCanStream(this);
            }
            savedToast();
            selectTab(checked ? TAB_OBD : firstAvailableTab());
        }));
        app.addView(check("Включить раздел TPMS", AppPrefs.tpmsEnabled(this), (button, checked) -> {
            AppPrefs.setTpmsEnabled(this, checked);
            if (checked) {
                TpmsMonitor.restart(this);
            } else {
                TpmsMonitor.stop();
                TpmsState.status(this, "TPMS: раздел выключен в настройках", false);
            }
            savedToast();
            selectTab(!AppPrefs.obdEnabled(this) && checked ? TAB_TPMS : firstAvailableTab());
        }));
        app.addView(check("Автозапуск приложения", AppPrefs.autoStart(this), (button, checked) -> {
            AppPrefs.setAutoStart(this, checked);
            AppLog.line(this, "Настройки: автозапуск " + yes(checked));
            savedToast();
        }));
        app.addView(check("Фоновый автозапуск без открытия экрана", AppPrefs.backgroundAutoStart(this), (button, checked) -> {
            AppPrefs.setBackgroundAutoStart(this, checked);
            AppLog.line(this, "Настройки: фоновый автозапуск " + yes(checked));
            savedToast();
        }));
        app.addView(check("Показать журнал поверх экрана", AppPrefs.logOverlay(this), (button, checked) -> {
            AppPrefs.setLogOverlay(this, checked);
            if (checked && !PermissionHelper.canDrawOverlays(this)) {
                Toast.makeText(this, "Нужно разрешение поверх других окон", Toast.LENGTH_SHORT).show();
                PermissionHelper.openOverlaySettings(this);
            }
            AppLog.line(this, "Настройки: журнал поверх экрана " + yes(checked));
            AppService.refreshOverlays(this);
            savedToast();
            selectTab(TAB_SETTINGS);
        }));
        TextView logOverlayHint = bodyText("Журнал открывается поверх любых приложений в нижней половине экрана. CAN raw и полный debug от этого не включаются.");
        logOverlayHint.setTextColor(0xff6d7280);
        app.addView(logOverlayHint, matchWrap());

        LinearLayout updates = card();
        content.addView(updates, cardLp());
        addCardTitle(updates, "Обновление приложения");
        appUpdateStatusValue = infoRow(updates, "Статус", "");
        appUpdateReleaseValue = infoRow(updates, "GitHub", "");
        updates.addView(check("Проверять при запуске", AppPrefs.updateCheckOnLaunch(this), (button, checked) -> {
            AppPrefs.setUpdateCheckOnLaunch(this, checked);
            savedToast();
        }));
        GridLayout updateActions = grid(2);
        updates.addView(updateActions, matchWrap());
        gridButton(updateActions, "Проверить обновление", v -> AppUpdater.checkNow(this));
        appUpdateInstallButton = gridButton(updateActions, "Скачать / установить", v -> AppUpdater.downloadAndInstall(this));

        LinearLayout canDebug = card();
        content.addView(canDebug, cardLp());
        addCardTitle(canDebug, "Отладка CAN");
        canDebug.addView(check("Режим отладки CAN", AppPrefs.debugCan(this), (button, checked) -> {
            AppPrefs.setDebugCan(this, checked);
            if (checked) CanbusControl.startCanStream(this);
            else {
                SidebandDebugState.setCanRecording(this, false);
                if (!AppPrefs.obdEnabled(this) && !AppPrefs.blindSpotEnabled(this)) CanbusControl.stopCanStream(this);
            }
            savedToast();
            selectTab(TAB_SETTINGS);
        }));
        if (AppPrefs.debugCan(this)) {
            canDebugStatusValue = infoRow(canDebug, "Скорости", "");
            GridLayout canActions = grid(2);
            canDebug.addView(canActions, matchWrap());
            canRecordButton = gridButton(canActions, "", v -> {
                SidebandDebugState.Snapshot debug = SidebandDebugState.snapshot();
                SidebandDebugState.setCanRecording(this, !debug.canRecording);
                refresh();
            });
            canModeButton = gridButton(canActions, "", v -> {
                AppPrefs.setCanLogMode(this, (AppPrefs.canLogMode(this) + 1) % 3);
                savedToast();
                refresh();
            });
            gridButton(canActions, "Сохранить CAN log", v -> saveSidebandLog("can", SidebandDebugState.canExportText(AppPrefs.canLogMode(this))));
            gridButton(canActions, "Сжать CAN log", v -> saveSidebandLogCompressed("can", SidebandDebugState.canExportText(AppPrefs.canLogMode(this))));
            canLogPreviewValue = bodyText("");
            canLogPreviewValue.setTypeface(Typeface.MONOSPACE);
            canLogPreviewValue.setTextSize(12);
            canLogPreviewValue.setPadding(dp(12), dp(10), dp(12), dp(10));
            canLogPreviewValue.setBackground(rounded(0xfff7f9fc, 8));
            canDebug.addView(canLogPreviewValue, new LinearLayout.LayoutParams(-1, dp(120)));
        }

        if (AppPrefs.logOverlay(this)) {
            LinearLayout log = card();
            content.addView(log, cardLp());
            addCardTitle(log, "Журнал overlay");
            ScrollView scroll = new ScrollView(this);
            scroll.setBackground(rounded(0xfff7f9fc, 8));
            logValue = bodyText("");
            logValue.setTypeface(Typeface.MONOSPACE);
            logValue.setTextSize(12);
            logValue.setPadding(dp(12), dp(10), dp(12), dp(10));
            scroll.addView(logValue, new ScrollView.LayoutParams(-1, -2));
            log.addView(scroll, new LinearLayout.LayoutParams(-1, dp(250)));
        }

        LinearLayout permissions = card();
        content.addView(permissions, cardLp());
        addCardTitle(permissions, "Разрешения");
        Button[] runtime = new Button[1];
        runtimePermissionStatus = permissionStatusRow(permissions, "Android runtime", runtime, v -> requestRuntimePermissionsNow());
        runtimePermissionButton = runtime[0];
        Button[] notifications = new Button[1];
        notificationPermissionStatus = permissionStatusRow(permissions, "Уведомления / музыка", notifications,
                v -> PermissionHelper.openNotificationListener(this));
        notificationPermissionButton = notifications[0];
        Button[] overlay = new Button[1];
        overlayPermissionStatus = permissionStatusRow(permissions, "Поверх других окон", overlay,
                v -> PermissionHelper.openOverlaySettings(this));
        overlayPermissionButton = overlay[0];
    }

    private void refresh() {
        CanbusControl.Snapshot can = CanbusControl.snapshot();
        VehicleDisplayState.Snapshot vehicle = VehicleDisplayState.snapshot();
        TpmsState.Snapshot tpms = TpmsState.snapshot();
        BlindSpotState.Snapshot blind = BlindSpotState.snapshot();

        if (statusValue != null) {
            String last = can.lastFrameAt == 0 ? "" : "ответ " + ageText(System.currentTimeMillis() - can.lastFrameAt) + " назад";
            statusValue.setText(last);
        }
        if (uidValue != null) uidValue.setText(can.adapterUid);
        if (versionValue != null) versionValue.setText(can.firmwareVersion);
        if (v20StatusValue != null) v20StatusValue.setText(can.v20Status);
        if (v20ApiValue != null) v20ApiValue.setText(can.v20Api);
        if (v20CapsValue != null) v20CapsValue.setText(can.v20Capabilities);
        if (sourceValue != null) sourceValue.setText(vehicle.source);
        if (updateValue != null) updateValue.setText(can.updateStatus);
        if (vehicleStatusValue != null) vehicleStatusValue.setText(cleanVehicleStatus(vehicle));
        if (blindSpotStatusValue != null) blindSpotStatusValue.setText(blind.statusText()
                + " | overlay " + yes(AppPrefs.blindSpotOverlay(this)));
        if (tpmsStatusValue != null) tpmsStatusValue.setText(tpms.status + " | " + (tpms.connected ? "подключено" : "нет подключения"));
        if (appVersionValue != null) appVersionValue.setText(versionText());
        if (appUpdateStatusValue != null) {
            AppUpdater.Snapshot update = AppUpdater.snapshot();
            appUpdateStatusValue.setText(update.status);
            appUpdateReleaseValue.setText(updateText(update));
            if (appUpdateInstallButton != null) {
                boolean showInstall = update.updateAvailable || update.downloaded || update.downloading;
                appUpdateInstallButton.setVisibility(showInstall ? View.VISIBLE : View.GONE);
                appUpdateInstallButton.setEnabled(showInstall);
                appUpdateInstallButton.setText(update.downloaded ? "Установить APK"
                        : (update.downloading ? "Загрузка" : "Скачать / установить"));
            }
        }
        if (firmwareReleaseStatusValue != null) {
            FirmwareReleaseUpdater.Snapshot firmware = FirmwareReleaseUpdater.snapshot();
            firmwareReleaseStatusValue.setText(firmware.status);
            firmwareReleaseAssetValue.setText(firmwareUpdateText(firmware));
            if (firmwareReleaseFlashButton != null) {
                boolean canFlash = (!TextUtils.isEmpty(firmware.downloadUrl) || firmware.downloaded)
                        && (firmware.assetSize <= 0 || firmware.assetSize <= 114688);
                firmwareReleaseFlashButton.setEnabled(canFlash);
                firmwareReleaseFlashButton.setText(firmware.downloading || firmware.flashing ? "Идёт процесс"
                        : (firmware.downloaded ? "Прошить скачанное" : "Скачать и прошить"));
            }
            if (progressBar != null) {
                if (firmware.downloading || firmware.flashing) {
                    progressBar.setProgress(Math.min(100, Math.round(firmware.downloadedBytes * 100f / Math.max(1, firmware.totalBytes))));
                } else if (firmware.downloaded) {
                    progressBar.setProgress(100);
                } else {
                    progressBar.setProgress(0);
                }
            }
        }
        if (tpmsLowValue != null) tpmsLowValue.setText(String.format(Locale.US, "%.1f Bar", AppPrefs.tpmsLowBar(this)));
        if (tpmsHighValue != null) tpmsHighValue.setText(String.format(Locale.US, "%.1f Bar", AppPrefs.tpmsHighBar(this)));
        if (mediaValue != null) mediaValue.setText(AppLog.media());
        if (mediaPreviewValue != null) mediaPreviewValue.setText(MediaMonitor.settingsPreview(this));
        if (mediaClusterPreviewValue != null) mediaClusterPreviewValue.setText(MediaMonitor.clusterPreview(this));
        if (mediaFormatButton != null) mediaFormatButton.setText("Формат: " + AppPrefs.mediaTextFormatLabel(this));
        if (usbValue != null) usbValue.setText(cleanUsbStatus(AppLog.usb()));
        if (navValue != null) navValue.setText(AppLog.nav());
        if (navDebugValue != null) navDebugValue.setText(navDebugText());
        refreshNavAdapterTable();
        refreshPermissionStatus();
        if (logValue != null) logValue.setText(AppPrefs.logOverlay(this) ? AppLog.text() : "");
        if (speedMetric != null) speedMetric.setText(vehicle.speedText(this));
        if (rpmMetric != null) rpmMetric.setText(vehicle.rpm + " rpm");
        if (voltageMetric != null) voltageMetric.setText(vehicle.voltageText());
        if (tempMetric != null) tempMetric.setText(vehicle.tempText(this, vehicle.coolantTemp));
        if (runtimeMetric != null) runtimeMetric.setText(vehicle.runtimeText());
        if (speedButton != null) speedButton.setText("Скорость: " + AppPrefs.speedUnitLabel(this));
        if (tempButton != null) tempButton.setText("Температура: " + (AppPrefs.tempUnit(this) == 1 ? "°F" : "°C"));
        if (navModeButton != null) navModeButton.setText(navModeText());
        AppService.refreshOverlays(this);
        refreshSidebandDebug();
        if (AppPrefs.uartOverlay(this)) AppPrefs.setUartOverlay(this, false);
        if (sasEdit != null && !sasEdit.hasFocus()) sasEdit.setText(String.valueOf(AppPrefs.sasRatio(this)));
        refreshAmpTexts();
        refreshTires(tpms);
    }

    private void refreshSidebandDebug() {
        SidebandDebugState.Snapshot debug = SidebandDebugState.snapshot();
        if (canDebugStatusValue != null) {
            String status = "C-CAN bus0: " + debug.cSlotCount + " кадров, " + sidebandAge(debug.cAgeMs)
                    + " | M-CAN bus1: " + debug.mSlotCount + " кадров, " + sidebandAge(debug.mAgeMs);
            if (!TextUtils.isEmpty(debug.canCaptureStatus)) {
                status += "\n" + debug.canCaptureStatus;
            }
            canDebugStatusValue.setText(status);
        }
        if (canRecordButton != null) canRecordButton.setText(debug.canRecording ? "Остановить CAN" : "Начать CAN");
        if (canModeButton != null) canModeButton.setText("Писать: " + canModeLabel());
        if (canLogPreviewValue != null) {
            String text = "M-CAN: " + emptyDash(debug.lastMCan) + "\nC-CAN: " + emptyDash(debug.lastCCan)
                    + "\nЛимит записи: " + debug.canCaptureCount + "/" + debug.canCaptureLimit;
            if (!TextUtils.isEmpty(debug.lastSaved)) text += "\nФайл: " + debug.lastSaved;
            canLogPreviewValue.setText(text);
        }
    }

    private void refreshPermissionStatus() {
        setPermissionStatus(runtimePermissionStatus, runtimePermissionButton, runtimePermissionsOk());
        setPermissionStatus(notificationPermissionStatus, notificationPermissionButton, PermissionHelper.hasNotificationAccess(this));
        setPermissionStatus(overlayPermissionStatus, overlayPermissionButton, PermissionHelper.canDrawOverlays(this));
    }

    private void setPermissionStatus(TextView status, Button button, boolean granted) {
        if (status != null) {
            status.setText(granted ? "есть" : "нет");
            status.setTextColor(granted ? 0xff188038 : 0xffb42318);
        }
        if (button != null) button.setVisibility(granted ? View.GONE : View.VISIBLE);
    }

    private void requestRuntimePermissionsNow() {
        if (Build.VERSION.SDK_INT < 23) {
            refresh();
            return;
        }
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        for (String permission : PermissionHelper.runtimePermissions()) {
            if (!PermissionHelper.shouldRequestRuntime(this, permission)) continue;
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        if (missing.isEmpty()) {
            Toast.makeText(this, "Android разрешения уже есть", Toast.LENGTH_SHORT).show();
            refresh();
            return;
        }
        requestPermissions(missing.toArray(new String[0]), PermissionHelper.REQ_RUNTIME);
    }

    private String cleanVehicleStatus(VehicleDisplayState.Snapshot vehicle) {
        if (vehicle == null || vehicle.status == null) return "";
        String value = vehicle.status;
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("адаптер не подключ") || lower.contains("адаптер не найден")) return "";
        if (lower.contains("usb-адаптер не найден")) return "";
        return value;
    }

    private String cleanUsbStatus(String value) {
        if (value == null) return "";
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("usb-адаптер не найден") || lower.contains("адаптер не подключ")) return "";
        return value;
    }

    private void openTpmsOnAlert() {
        if (!AppPrefs.tpmsEnabled(this)) return;
        if (!AppPrefs.tpmsAlertOverlay(this)) return;
        if (TpmsAlertManager.isSuppressed(this)) return;
        if (TpmsAlertManager.alertMessage(TpmsState.snapshot()).length() == 0) return;
        AppService.refreshOverlays(this);
    }

    private String navDebugText() {
        NavDebugState.Snapshot nav = NavDebugState.snapshot();
        return NavProtocol.adapterStateText() + "\n"
                + "TEYES: " + shortDebug(nav.lastTeyes) + "\n"
                + "Intent: " + shortDebug(nav.lastEvent) + "\n"
                + "CAN: " + shortDebug(nav.lastFrame);
    }

    private String shortDebug(String value) {
        if (TextUtils.isEmpty(value)) return "-";
        String clean = value.replace('\n', ' ').replace('\r', ' ').trim();
        return clean.length() > 170 ? clean.substring(0, 170) + "..." : clean;
    }

    private String versionText() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
            return info.versionName + " (" + code + ")";
        } catch (Exception e) {
            return "не получена";
        }
    }

    private String updateText(AppUpdater.Snapshot update) {
        if (update == null) return "";
        StringBuilder text = new StringBuilder();
        text.append("сейчас ").append(emptyDash(update.currentVersion));
        if (update.currentRelease > 0) text.append(" / ").append(update.currentRelease);
        if (update.latestRelease > 0 || !TextUtils.isEmpty(update.assetName)) {
            text.append("  |  latest ");
            if (update.latestRelease > 0) text.append(update.latestRelease).append(' ');
            text.append(emptyDash(update.assetName));
        }
        if (update.downloading) {
            text.append("  |  ").append(downloadProgress(update.downloadedBytes, update.totalBytes));
        }
        return text.toString();
    }

    private String firmwareUpdateText(FirmwareReleaseUpdater.Snapshot firmware) {
        if (firmware == null) return "";
        StringBuilder text = new StringBuilder();
        if (!TextUtils.isEmpty(firmware.tagName)) text.append(firmware.tagName).append("  |  ");
        text.append(emptyDash(firmware.assetName));
        if (firmware.assetSize > 0) text.append("  |  ").append(firmware.assetSize / 1024).append(" KB");
        if (firmware.downloading || firmware.flashing) {
            text.append("  |  ").append(downloadProgress(firmware.downloadedBytes, firmware.totalBytes));
        }
        return text.toString();
    }

    private String downloadProgress(long done, long total) {
        if (total <= 0) return (done / 1024) + " KB";
        return Math.min(100, Math.round(done * 100f / total)) + "%";
    }

    private TextView thresholdRow(LinearLayout parent, String label, float value, float minusStep, float plusStep, boolean low) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(58)));

        TextView name = bodyText(label);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(name, new LinearLayout.LayoutParams(0, -1, 1));

        Button minus = button("-", 0xff151928, v -> adjustTpmsThreshold(low, minusStep));
        row.addView(minus, new LinearLayout.LayoutParams(dp(58), dp(48)));

        TextView text = bodyText(String.format(Locale.US, "%.1f Bar", value));
        text.setGravity(Gravity.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(18);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(dp(122), dp(48));
        textLp.setMargins(dp(8), 0, dp(8), 0);
        row.addView(text, textLp);

        Button plus = button("+", 0xff151928, v -> adjustTpmsThreshold(low, plusStep));
        row.addView(plus, new LinearLayout.LayoutParams(dp(58), dp(48)));
        return text;
    }

    private void addNavAdapterTable(LinearLayout parent) {
        navAdapterRows.clear();
        String[][] rows = NavProtocol.adapterRows();
        for (String[] row : rows) {
            TextView value = infoRow(parent, row[0], row[1]);
            value.setSingleLine(false);
            value.setMaxLines(3);
            navAdapterRows.add(value);
        }
    }

    private void refreshNavAdapterTable() {
        if (navAdapterRows.isEmpty()) return;
        String[][] rows = NavProtocol.adapterRows();
        int count = Math.min(navAdapterRows.size(), rows.length);
        for (int i = 0; i < count; i++) {
            navAdapterRows.get(i).setText(rows[i][1]);
        }
    }

    private void adjustTpmsThreshold(boolean low, float delta) {
        if (low) {
            float next = AppPrefs.tpmsLowBar(this) + delta;
            next = Math.min(next, AppPrefs.tpmsHighBar(this) - 0.1f);
            AppPrefs.setTpmsLowBar(this, next);
        } else {
            float next = AppPrefs.tpmsHighBar(this) + delta;
            next = Math.max(next, AppPrefs.tpmsLowBar(this) + 0.1f);
            AppPrefs.setTpmsHighBar(this, next);
        }
        AppLog.line(this, "TPMS: пороги " + AppPrefs.tpmsLowBar(this) + " / " + AppPrefs.tpmsHighBar(this) + " Bar");
        savedToast();
        refresh();
    }

    private TextView ampRow(LinearLayout parent, String label, String minusText, String plusText, Runnable minusAction, Runnable plusAction) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(58)));

        TextView name = bodyText(label);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(name, new LinearLayout.LayoutParams(0, -1, 1));

        Button minus = button(minusText, 0xff151928, v -> minusAction.run());
        row.addView(minus, new LinearLayout.LayoutParams(dp(64), dp(48)));

        TextView value = bodyText("0");
        value.setTextSize(20);
        value.setTypeface(Typeface.DEFAULT_BOLD);
        value.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(dp(100), dp(48));
        valueLp.setMargins(dp(8), 0, dp(8), 0);
        row.addView(value, valueLp);

        Button plus = button(plusText, 0xff151928, v -> plusAction.run());
        row.addView(plus, new LinearLayout.LayoutParams(dp(64), dp(48)));
        return value;
    }

    private void changeAmp(String key, int delta) {
        if ("fader".equals(key)) {
            ampFader = clamp(ampFader + delta, 0, 20);
        } else if ("balance".equals(key)) {
            ampBalance = clamp(ampBalance + delta, 0, 20);
        } else if ("bass".equals(key)) {
            ampBass = clamp(ampBass + delta, 0, 20);
        } else if ("mid".equals(key)) {
            ampMid = clamp(ampMid + delta, 0, 20);
        } else if ("treble".equals(key)) {
            ampTreble = clamp(ampTreble + delta, 0, 20);
        } else if ("volume".equals(key)) {
            ampVolume = clamp(ampVolume + delta, 0, 40);
        } else if ("mode".equals(key)) {
            ampMode = clamp(ampMode + delta, 0, 32);
        }
        sendAmp(true);
        Toast.makeText(this, "AMP: настройки сохранены", Toast.LENGTH_SHORT).show();
        refreshAmpTexts();
    }

    private void sendAmp(boolean save) {
        CanbusControl.setAmpSettings(this, ampVolume, ampBalance, ampFader, ampBass, ampMid, ampTreble, ampMode, save);
    }

    private void refreshAmpTexts() {
        if (ampFaderValue != null) ampFaderValue.setText(String.valueOf(ampFader - 10));
        if (ampBalanceValue != null) ampBalanceValue.setText(String.valueOf(ampBalance - 10));
        if (ampBassValue != null) ampBassValue.setText(String.valueOf(ampBass - 10));
        if (ampMidValue != null) ampMidValue.setText(String.valueOf(ampMid - 10));
        if (ampTrebleValue != null) ampTrebleValue.setText(String.valueOf(ampTreble - 10));
        if (ampVolumeValue != null) ampVolumeValue.setText(String.valueOf(ampVolume));
        if (ampModeValue != null) ampModeValue.setText(String.valueOf(ampMode));
    }

    private void parseAmpFrame(byte[] frame) {
        if (frame == null || frame.length <= 12 || (frame[4] & 0xff) != 0x30) return;
        ampVolume = clamp(frame[6] & 0xff, 0, 40);
        ampBalance = clamp(frame[7] & 0xff, 0, 20);
        ampFader = clamp(frame[8] & 0xff, 0, 20);
        ampBass = clamp(frame[9] & 0xff, 0, 20);
        ampMid = clamp(frame[10] & 0xff, 0, 20);
        ampTreble = clamp(frame[11] & 0xff, 0, 20);
        ampMode = clamp(frame[12] & 0x1f, 0, 32);
        AppLog.line(this, "AMP: настройки получены vol=" + ampVolume + " mode=" + ampMode);
    }

    private void refreshTires(TpmsState.Snapshot tpms) {
        if (tpms == null || tpms.tires == null) return;
        for (int i = 0; i < tpms.tires.length && i < tireValues.length; i++) {
            TpmsState.Tire tire = tpms.tires[i];
            if (tireValues[i] != null) {
                tireValues[i].setText((tire == null ? "__" : tire.pressureText()) + " Bar  /  "
                        + (tire == null ? "__" : tire.tempText()) + "°C");
            }
            if (tireStates[i] != null) {
                tireStates[i].setText(tire == null ? "нет данных" : tire.warningText());
                tireStates[i].setTextColor(tire != null && tire.alert() ? 0xffd93025 : 0xff188038);
            }
        }
    }

    private void sendSas() {
        String text = sasEdit == null ? "" : sasEdit.getText().toString();
        int ratio = AppPrefs.sasRatio(this);
        if (!TextUtils.isEmpty(text)) {
            try {
                ratio = Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
            }
        }
        ratio = Math.max(10, Math.min(50, ratio));
        if (sasEdit != null) sasEdit.setText(String.valueOf(ratio));
        CanbusControl.setSasRatio(this, ratio);
    }

    private void saveSidebandLog(String prefix, String text) {
        try {
            File file = SidebandDebugState.save(this, prefix, text);
            Toast.makeText(this, "Лог сохранён: " + file.getName(), Toast.LENGTH_LONG).show();
            refresh();
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось сохранить лог: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveSidebandLogCompressed(String prefix, String text) {
        try {
            File file = SidebandDebugState.saveCompressed(this, prefix, text);
            Toast.makeText(this, "Лог сжат: " + file.getName(), Toast.LENGTH_LONG).show();
            refresh();
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось сжать лог: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void closeSettings() {
        Intent home = new Intent(this, MainActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(home);
        finish();
    }

    private void pickFirmware() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            startActivityForResult(intent, REQ_FIRMWARE);
        } catch (Exception e) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.setType("*/*");
            startActivityForResult(fallback, REQ_FIRMWARE);
        }
    }

    private void startFirmwareUpdate(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show();
                return;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            byte[] data = out.toByteArray();
            if (progressBar != null) progressBar.setProgress(0);
            AppLog.line(this, "CAN прошивка: выбран файл " + data.length + " байт");
            new CanbusFirmwareUpdater(this, data, (text, percent, done) -> {
                if (progressBar != null) progressBar.setProgress(percent);
                if (updateValue != null) updateValue.setText(text + " (" + percent + "%)");
                AppLog.line(this, "CAN прошивка: " + text + " " + percent + "%");
                refresh();
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
            AppLog.line(this, "CAN прошивка: ошибка файла " + e.getClass().getSimpleName());
        }
    }

    private void sectionHeader(String title, String subtitle) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(0xfff4f1ea);
        t.setTextSize(compactUi() ? 21 : 25);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.LEFT);
        content.addView(t, new LinearLayout.LayoutParams(-1, -2));

        TextView s = bodyText(subtitle);
        s.setTextColor(0xffa4abb6);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(compactUi() ? 12 : 16));
        content.addView(s, lp);
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(compactUi() ? 12 : 18), dp(compactUi() ? 12 : 14),
                dp(compactUi() ? 12 : 18), dp(compactUi() ? 12 : 16));
        view.setBackground(roundedStroke(0xfff4f1ea, 8, 0xffddd7ca));
        view.setElevation(dp(2));
        return view;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(compactUi() ? 10 : 14));
        return lp;
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        return lp;
    }

    private void addCardTitle(LinearLayout parent, String title) {
        TextView v = new TextView(this);
        v.setText(title);
        v.setTextColor(0xff10131f);
        v.setTextSize(compactUi() ? 16 : 18);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.LEFT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        parent.addView(v, lp);
    }

    private TextView infoRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(0xff6d7280);
        name.setTextSize(compactUi() ? 12 : 14);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(name, new LinearLayout.LayoutParams(dp(compactUi() ? 112 : 145), -2));

        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(0xff111827);
        text.setTextSize(compactUi() ? 12 : 14);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setSingleLine(!compactUi());
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        return text;
    }

    private TextView permissionStatusRow(LinearLayout parent, String label, Button[] buttonOut, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(compactUi() ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(compactUi() ? 12 : 16), dp(6), dp(compactUi() ? 12 : 12), dp(6));
        row.setBackground(roundedStroke(0xfff7f9fc, 14, 0xffdfe5ef));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, compactUi() ? -2 : dp(62));
        lp.setMargins(0, dp(5), 0, dp(5));
        parent.addView(row, lp);

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(0xff1f2430);
        name.setTextSize(compactUi() ? 13.5f : 15.5f);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(name, compactUi()
                ? new LinearLayout.LayoutParams(-1, dp(30))
                : new LinearLayout.LayoutParams(0, -1, 1));

        TextView status = new TextView(this);
        status.setTextColor(0xff8a1f1f);
        status.setTextSize(compactUi() ? 13 : 15);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setGravity(Gravity.CENTER);
        row.addView(status, compactUi()
                ? new LinearLayout.LayoutParams(-1, dp(30))
                : new LinearLayout.LayoutParams(dp(124), -1));

        Button action = button("Запросить", 0xff151928, listener);
        LinearLayout.LayoutParams actionLp = compactUi()
                ? new LinearLayout.LayoutParams(-1, dp(44))
                : new LinearLayout.LayoutParams(dp(132), dp(46));
        actionLp.setMargins(compactUi() ? 0 : dp(10), compactUi() ? dp(6) : 0, 0, 0);
        row.addView(action, actionLp);
        buttonOut[0] = action;
        return status;
    }

    private TextView bodyText(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(0xff202433);
        v.setTextSize(compactUi() ? 13 : 15);
        v.setLineSpacing(dp(2), 1.0f);
        v.setSingleLine(false);
        return v;
    }

    private GridLayout grid(int columns) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(adaptiveColumns(columns));
        grid.setUseDefaultMargins(false);
        return grid;
    }

    private TextView metric(GridLayout grid, String title, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(compactUi() ? 12 : 16), dp(12), dp(compactUi() ? 12 : 16), dp(14));
        box.setBackground(roundedStroke(0xffffffff, 8, 0xffddd7ca));
        box.setElevation(dp(2));

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(0xff697080);
        label.setTextSize(13);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(label, new LinearLayout.LayoutParams(-1, -2));

        TextView data = new TextView(this);
        data.setText(value);
        data.setTextColor(0xff111827);
        data.setTextSize(compactUi() ? 23 : 27);
        data.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams dataLp = new LinearLayout.LayoutParams(-1, -2);
        dataLp.setMargins(0, dp(8), 0, 0);
        box.addView(data, dataLp);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(compactUi() ? 92 : 104);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        grid.addView(box, lp);
        return data;
    }

    private Button gridButton(GridLayout grid, String text, View.OnClickListener listener) {
        Button b = button(text, 0xff151928, listener);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(compactUi() ? 48 : 52);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        grid.addView(b, lp);
        return b;
    }

    private void tireCard(GridLayout grid, int index, String title) {
        LinearLayout box = card();
        addCardTitle(box, title);
        tireValues[index] = bodyText("-- Bar / --°C");
        tireValues[index].setTextSize(24);
        tireValues[index].setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(tireValues[index], new LinearLayout.LayoutParams(-1, -2));
        tireStates[index] = bodyText("нет данных");
        LinearLayout.LayoutParams stateLp = new LinearLayout.LayoutParams(-1, -2);
        stateLp.setMargins(0, dp(6), 0, 0);
        box.addView(tireStates[index], stateLp);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(compactUi() ? 124 : 134);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        grid.addView(box, lp);
    }

    private View check(String text, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(0, dp(4), 0, dp(4));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(12), 0);
        row.setBackground(roundedStroke(0xfff7f9fc, 14, 0xffdfe5ef));
        row.setClickable(true);
        wrap.addView(row, new FrameLayout.LayoutParams(-1, dp(compactUi() ? 64 : 58)));

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(0xff1f2430);
        label.setTextSize(compactUi() ? 13.5f : 15.5f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setSingleLine(false);
        row.addView(label, new LinearLayout.LayoutParams(0, -1, 1));

        Switch toggle = new Switch(this);
        toggle.setShowText(false);
        toggle.setText(null);
        toggle.setMinWidth(dp(60));
        toggle.setPadding(dp(8), 0, 0, 0);
        toggle.setScaleX(compactUi() ? 0.94f : 1.06f);
        toggle.setScaleY(compactUi() ? 0.94f : 1.06f);
        tintSwitch(toggle);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener(listener);
        row.addView(toggle, new LinearLayout.LayoutParams(dp(76), dp(52)));
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        label.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        return wrap;
    }

    private void tintSwitch(Switch toggle) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        toggle.setThumbTintList(new ColorStateList(states, new int[]{0xffffffff, 0xffffffff}));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{0xff19c58b, 0xff8a909b}));
    }

    private Button button(String text, int color, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(compactUi() ? 13 : 15);
        b.setTextColor(0xffffffff);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(8), 0, dp(8), 0);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setBackground(rounded(color, 8));
        b.setOnClickListener(listener);
        return b;
    }

    private Button iconButton(String text, View.OnClickListener listener) {
        Button b = button(text, 0xff252a3d, listener);
        b.setTextSize(compactUi() ? 22 : 24);
        return b;
    }

    private boolean compactUi() {
        return screenWidthDp() < 720;
    }

    private int adaptiveColumns(int requested) {
        int width = screenWidthDp();
        if (width < 720) return 1;
        if (width < 980) return Math.min(requested, 2);
        return requested;
    }

    private int screenWidthDp() {
        float density = getResources().getDisplayMetrics().density;
        int px = getResources().getDisplayMetrics().widthPixels;
        return density <= 0 ? px : Math.round(px / density);
    }

    private void savedToast() {
        Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        return bg;
    }

    private GradientDrawable roundedStroke(int color, int radius, int strokeColor) {
        GradientDrawable bg = rounded(color, radius);
        bg.setStroke(dp(1), strokeColor);
        return bg;
    }

    private boolean runtimePermissionsOk() {
        if (Build.VERSION.SDK_INT < 23) return true;
        for (String permission : PermissionHelper.runtimePermissions()) {
            if (!PermissionHelper.shouldRequestRuntime(this, permission)) continue;
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private String canModeLabel() {
        int mode = AppPrefs.canLogMode(this);
        if (mode == 0) return "M-CAN";
        if (mode == 1) return "C-CAN";
        return "оба CAN";
    }

    private String navModeText() {
        int mode = AppPrefs.navTextMode(this);
        if (mode == 1) return "0x4A: лимит скорости";
        if (mode == 2) return "0x4A: лимит при превышении";
        return "0x4A: улица";
    }

    private String sidebandAge(long ms) {
        return ms < 0 ? "нет данных" : ageText(ms);
    }

    private String emptyDash(String value) {
        return TextUtils.isEmpty(value) ? "—" : value;
    }

    private String ageText(long ms) {
        long seconds = Math.max(0, ms / 1000);
        if (seconds < 60) return seconds + " сек";
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    private String yes(boolean value) {
        return value ? "да" : "нет";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
