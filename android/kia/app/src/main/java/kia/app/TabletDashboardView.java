package kia.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

final class TabletDashboardView extends FrameLayout {
    private static final int TAB_HOME = 0;
    private static final int TAB_NAV = 1;
    private static final int TAB_MEDIA = 2;
    private static final int TAB_VEHICLE = 3;
    private static final int TAB_DIAG = 4;

    private final String[] tabLabels = {"Главная", "Навигация", "Медиа", "Машина", "Диагностика"};
    private final TextView[] menuItems = new TextView[tabLabels.length];
    private LinearLayout content;
    private TextView screenTitle;
    private TextView screenSubtitle;
    private TextView firmwareChip;
    private TextView usbChip;

    private TextView adapterUsbValue;
    private TextView adapterV20Value;
    private TextView adapterApiValue;
    private TextView adapterCapsValue;
    private TextView adapterUidValue;
    private TextView adapterVersionValue;
    private TextView navStatusValue;
    private TextView navDebugValue;
    private TextView mediaStatusValue;
    private TextView vehicleStatusValue;
    private TextView speedValue;
    private TextView rpmValue;
    private TextView tempValue;
    private TextView voltageValue;
    private TextView tpmsStatusValue;
    private TextView blindSpotValue;
    private TextView canCounterValue;
    private TextView canPreviewValue;
    private TextView appLogValue;
    private TextView updateStatusValue;
    private TextView updateReleaseValue;
    private TextView firmwareOtaStatusValue;
    private TextView firmwareOtaAssetValue;
    private Button navModeButton;
    private Button canRecordButton;
    private Button canModeButton;
    private Button updateInstallButton;
    private Button firmwareOtaFlashButton;
    private int currentTab = TAB_HOME;
    private boolean compactMode;
    private int lastWidthDp = -1;

    TabletDashboardView(Context context) {
        super(context);
        buildShell();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        float density = getResources().getDisplayMetrics().density;
        int widthDp = Math.round(w / density);
        boolean nextCompact = widthDp < 900 || h / density < 520;
        if (nextCompact != compactMode || Math.abs(widthDp - lastWidthDp) > 180) {
            compactMode = nextCompact;
            lastWidthDp = widthDp;
            buildShell();
        }
    }

    void refresh() {
        CanbusControl.Snapshot can = CanbusControl.snapshot();
        VehicleDisplayState.Snapshot vehicle = VehicleDisplayState.snapshot();
        TpmsState.Snapshot tpms = TpmsState.snapshot();
        BlindSpotState.Snapshot blind = BlindSpotState.snapshot();
        SidebandDebugState.Snapshot debug = SidebandDebugState.snapshot();

        if (firmwareChip != null) firmwareChip.setText(chipFirmware(can));
        if (usbChip != null) usbChip.setText(chipUsb(AppLog.usb()));
        if (adapterUsbValue != null) adapterUsbValue.setText(cleanUsb(AppLog.usb()));
        if (adapterV20Value != null) adapterV20Value.setText(can.v20Status);
        if (adapterApiValue != null) adapterApiValue.setText(can.v20Api);
        if (adapterCapsValue != null) adapterCapsValue.setText(can.v20Capabilities);
        if (adapterUidValue != null) adapterUidValue.setText(can.adapterUid);
        if (adapterVersionValue != null) adapterVersionValue.setText(can.firmwareVersion);
        if (navStatusValue != null) navStatusValue.setText(AppLog.nav());
        if (navDebugValue != null) navDebugValue.setText(navDebugText());
        if (mediaStatusValue != null) mediaStatusValue.setText(AppLog.media());
        if (vehicleStatusValue != null) vehicleStatusValue.setText(cleanVehicle(vehicle));
        if (speedValue != null) speedValue.setText(vehicle.speedText(getContext()));
        if (rpmValue != null) rpmValue.setText(vehicle.rpm + " rpm");
        if (tempValue != null) tempValue.setText(vehicle.tempText(getContext(), vehicle.coolantTemp));
        if (voltageValue != null) voltageValue.setText(vehicle.voltageText());
        if (tpmsStatusValue != null) tpmsStatusValue.setText(tpmsText(tpms));
        if (blindSpotValue != null) blindSpotValue.setText(blind.statusText());
        if (canCounterValue != null) {
            canCounterValue.setText("M-CAN " + debug.mSlotCount + " / C-CAN " + debug.cSlotCount);
        }
        if (canPreviewValue != null) {
            canPreviewValue.setText("M: " + dash(debug.lastMCan) + "\nC: " + dash(debug.lastCCan));
        }
        if (appLogValue != null) appLogValue.setText(shortLog(AppLog.text()));
        if (updateStatusValue != null) {
            AppUpdater.Snapshot update = AppUpdater.snapshot();
            updateStatusValue.setText(update.status);
            updateReleaseValue.setText(updateText(update));
            if (updateInstallButton != null) {
                updateInstallButton.setEnabled(update.updateAvailable || update.downloaded);
                updateInstallButton.setText(update.downloaded ? "Установить" : "Скачать");
            }
        }
        if (firmwareOtaStatusValue != null) {
            FirmwareReleaseUpdater.Snapshot firmware = FirmwareReleaseUpdater.snapshot();
            firmwareOtaStatusValue.setText(firmware.status);
            firmwareOtaAssetValue.setText(firmwareText(firmware));
            if (firmwareOtaFlashButton != null) {
                boolean canFlash = (!TextUtils.isEmpty(firmware.downloadUrl) || firmware.downloaded)
                        && (firmware.assetSize <= 0 || firmware.assetSize <= 114688);
                firmwareOtaFlashButton.setEnabled(canFlash);
                firmwareOtaFlashButton.setText(firmware.downloading ? "Загрузка" : "Скачать / прошить");
            }
        }
        if (navModeButton != null) navModeButton.setText(navModeText());
        if (canRecordButton != null) canRecordButton.setText(debug.canRecording ? "Остановить запись" : "Начать запись");
        if (canModeButton != null) canModeButton.setText("Лог: " + canModeLabel());
    }

    private void buildShell() {
        removeAllViews();
        setBackgroundColor(0xff111317);

        LinearLayout shell = new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.setPadding(dp(compactMode ? 8 : 12), dp(compactMode ? 8 : 12),
                dp(compactMode ? 8 : 12), dp(compactMode ? 8 : 12));
        addView(shell, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout rail = new LinearLayout(getContext());
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setPadding(dp(compactMode ? 8 : 14), dp(compactMode ? 8 : 14),
                dp(compactMode ? 8 : 14), dp(compactMode ? 8 : 14));
        rail.setBackground(roundedStroke(0xff181b20, 8, 0xff272b33));
        shell.addView(rail, new LinearLayout.LayoutParams(dp(compactMode ? 142 : 214), -1));

        TextView brand = text("Sportage", compactMode ? 19 : 27, 0xfff4f1ea, true);
        brand.setGravity(Gravity.LEFT);
        rail.addView(brand, new LinearLayout.LayoutParams(-1, dp(compactMode ? 30 : 38)));
        TextView sub = text(compactMode ? "2CAN35" : "2CAN35 V20 control",
                compactMode ? 11 : 13, 0xffa4abb6, false);
        rail.addView(sub, new LinearLayout.LayoutParams(-1, dp(compactMode ? 22 : 28)));

        View divider = new View(getContext());
        divider.setBackgroundColor(0xff2b3038);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(-1, dp(1));
        dividerLp.setMargins(0, dp(12), 0, dp(12));
        rail.addView(divider, dividerLp);

        for (int i = 0; i < tabLabels.length; i++) {
            rail.addView(menuItem(i), new LinearLayout.LayoutParams(-1, dp(compactMode ? 42 : 48)));
        }

        SpaceFill spacer = new SpaceFill(getContext());
        rail.addView(spacer, new LinearLayout.LayoutParams(-1, 0, 1));

        Button settings = actionButton("Настройки", 0xfff2b84b, v ->
                getContext().startActivity(new Intent(getContext(), CanbusSettingsActivity.class)));
        LinearLayout.LayoutParams settingsLp = new LinearLayout.LayoutParams(-1, dp(compactMode ? 44 : 50));
        settingsLp.setMargins(0, dp(10), 0, 0);
        rail.addView(settings, settingsLp);

        LinearLayout main = new LinearLayout(getContext());
        main.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mainLp = new LinearLayout.LayoutParams(0, -1, 1);
        mainLp.setMargins(dp(compactMode ? 8 : 12), 0, 0, 0);
        shell.addView(main, mainLp);

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(compactMode ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(compactMode ? 12 : 18), compactMode ? dp(10) : 0,
                dp(compactMode ? 12 : 18), compactMode ? dp(10) : 0);
        header.setBackground(roundedStroke(0xfff4f1ea, 8, 0xffddd7ca));
        main.addView(header, new LinearLayout.LayoutParams(-1, compactMode ? -2 : dp(74)));

        LinearLayout titleBox = new LinearLayout(getContext());
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(titleBox, compactMode
                ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(0, -1, 1));
        screenTitle = text("", compactMode ? 21 : 26, 0xff16181d, true);
        titleBox.addView(screenTitle, new LinearLayout.LayoutParams(-1, dp(compactMode ? 30 : 36)));
        screenSubtitle = text("", compactMode ? 12 : 13, 0xff66707d, false);
        titleBox.addView(screenSubtitle, new LinearLayout.LayoutParams(-1, dp(compactMode ? 22 : 24)));

        firmwareChip = chip("");
        usbChip = chip("");
        if (compactMode) {
            LinearLayout chips = new LinearLayout(getContext());
            chips.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams chipsLp = new LinearLayout.LayoutParams(-1, dp(40));
            chipsLp.setMargins(0, dp(8), 0, 0);
            header.addView(chips, chipsLp);
            LinearLayout.LayoutParams leftChipLp = new LinearLayout.LayoutParams(0, -1, 1);
            leftChipLp.setMargins(0, 0, dp(8), 0);
            chips.addView(firmwareChip, leftChipLp);
            chips.addView(usbChip, new LinearLayout.LayoutParams(0, -1, 1));
        } else {
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(dp(210), dp(42));
            chipLp.setMargins(0, 0, dp(10), 0);
            header.addView(firmwareChip, chipLp);
            header.addView(usbChip, new LinearLayout.LayoutParams(dp(230), dp(42)));
        }

        ScrollView scroll = new ScrollView(getContext());
        scroll.setFillViewport(false);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollLp.setMargins(0, dp(12), 0, 0);
        main.addView(scroll, scrollLp);
        content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(10));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        selectTab(TAB_HOME);
    }

    private TextView menuItem(int tab) {
        TextView item = text(tabLabels[tab], compactMode ? 13 : 16, 0xffd8dde6, true);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(compactMode ? 8 : 14), 0, dp(compactMode ? 6 : 10), 0);
        item.setClickable(true);
        item.setOnClickListener(v -> selectTab(tab));
        menuItems[tab] = item;
        return item;
    }

    private void selectTab(int tab) {
        currentTab = tab;
        for (int i = 0; i < menuItems.length; i++) {
            TextView item = menuItems[i];
            if (item == null) continue;
            boolean selected = i == currentTab;
            item.setTextColor(selected ? 0xff15171c : 0xffd8dde6);
            item.setBackground(rounded(selected ? 0xfff2b84b : 0x0015171c, 8));
        }
        if (screenTitle != null) screenTitle.setText(tabLabels[currentTab]);
        if (screenSubtitle != null) screenSubtitle.setText(subtitle(currentTab));
        buildCurrentTab();
        refresh();
    }

    private void buildCurrentTab() {
        if (content == null) return;
        content.removeAllViews();
        resetFields();
        switch (currentTab) {
            case TAB_NAV:
                buildNavigation();
                break;
            case TAB_MEDIA:
                buildMedia();
                break;
            case TAB_VEHICLE:
                buildVehicle();
                break;
            case TAB_DIAG:
                buildDiagnostics();
                break;
            case TAB_HOME:
            default:
                buildHome();
                break;
        }
    }

    private void buildHome() {
        LinearLayout row = row();
        content.addView(row, sectionLp());

        LinearLayout adapter = card("Адаптер");
        row.addView(adapter, weightedCardLp());
        adapterUsbValue = info(adapter, "USB", "");
        adapterV20Value = info(adapter, "V20", "");
        adapterApiValue = info(adapter, "API", "");
        addButtonRow(adapter,
                actionButton("Проверить", 0xff1f7a67, v -> CanbusControl.requestAdapterInfo(getContext())),
                actionButton("USB/CAN", 0xff2f5f8f, v -> startStack()));

        LinearLayout nav = card("Навигация");
        row.addView(nav, weightedCardLp());
        navStatusValue = info(nav, "Статус", "");
        nav.addView(switchRow("Компас", AppPrefs.navCompass(getContext()), (button, checked) -> {
            AppPrefs.setNavCompass(getContext(), checked);
            CompassBridge.refresh(getContext());
            toast("Компас " + yes(checked));
        }), controlLp());
        addButtonRow(nav,
                actionButton("Навигация", 0xff2f5f8f, v -> selectTab(TAB_NAV)),
                actionButton("Журнал", 0xff2f5f8f, v -> selectTab(TAB_DIAG)));

        LinearLayout media = card("Медиа");
        row.addView(media, weightedCardLp());
        mediaStatusValue = info(media, "Сейчас", "");
        addButtonRow(media,
                actionButton("Скан", 0xff2f5f8f, v -> MediaMonitor.scanNow(getContext())),
                actionButton("Доступ", 0xfff2b84b, v -> openNotificationSettings()));

        LinearLayout metrics = row();
        content.addView(metrics, sectionTopLp());
        speedValue = metric(metrics, "Скорость", "--");
        rpmValue = metric(metrics, "Обороты", "--");
        tempValue = metric(metrics, "Темп.", "--");
        voltageValue = metric(metrics, "Вольтаж", "--");

        LinearLayout bottom = row();
        content.addView(bottom, sectionTopLp());
        LinearLayout car = card("Машина");
        bottom.addView(car, weightedCardLp());
        vehicleStatusValue = info(car, "CAN", "");
        blindSpotValue = info(car, "RCTA", "");
        addButtonRow(car,
                actionButton("Vehicle", 0xff2f5f8f, v -> getContext().startActivity(new Intent(getContext(), VehicleInfoActivity.class))),
                actionButton("TPMS", 0xff2f5f8f, v -> getContext().startActivity(new Intent(getContext(), TpmsActivity.class))));

        LinearLayout diag = card("Диагностика");
        bottom.addView(diag, weightedCardLp());
        canCounterValue = info(diag, "Поток", "");
        adapterVersionValue = info(diag, "FW", "");
        addButtonRow(diag,
                actionButton("Диагностика", 0xfff2b84b, v -> selectTab(TAB_DIAG)),
                actionButton("Настройки", 0xff2f5f8f, v -> getContext().startActivity(new Intent(getContext(), CanbusSettingsActivity.class))));
    }

    private void buildNavigation() {
        LinearLayout row = row();
        content.addView(row, sectionLp());
        LinearLayout status = card("Маршрут и TBT");
        row.addView(status, weightedCardLp());
        navStatusValue = info(status, "Состояние", "");
        navModeButton = actionButton("", 0xff2f5f8f, v -> {
            int next = (AppPrefs.navTextMode(getContext()) + 1) % 3;
            NavProtocol.setTextMode(getContext(), next);
            refresh();
        });
        status.addView(navModeButton, buttonLp());
        status.addView(switchRow("TBT-иконки", AppPrefs.navTbt(getContext()), (button, checked) -> {
            NavProtocol.setTbtMode(getContext(), checked);
            toast("TBT " + yes(checked));
        }), controlLp());
        status.addView(switchRow("Компас без маршрута", AppPrefs.navCompass(getContext()), (button, checked) -> {
            AppPrefs.setNavCompass(getContext(), checked);
            CompassBridge.refresh(getContext());
            toast("Компас " + yes(checked));
        }), controlLp());

        LinearLayout live = card("Живые данные");
        row.addView(live, weightedCardLp());
        live.addView(body("Маршрут, TBT, текст и компас обновляются штатными командами адаптера V20."), textBlockLp());
        addButtonRow(live,
                actionButton("Скан медиа", 0xff2f5f8f, v -> MediaMonitor.scanNow(getContext())),
                actionButton("Диагностика", 0xfff2b84b, v -> selectTab(TAB_DIAG)));

        LinearLayout debug = card("Последние данные");
        content.addView(debug, sectionTopLp());
        navDebugValue = mono("");
        debug.addView(navDebugValue, monoLp(128, 112));
    }

    private void buildMedia() {
        LinearLayout row = row();
        content.addView(row, sectionLp());
        LinearLayout now = card("Текущий источник");
        row.addView(now, weightedCardLp());
        mediaStatusValue = info(now, "Строка", "");
        addButtonRow(now,
                actionButton("Сканировать", 0xff2f5f8f, v -> MediaMonitor.scanNow(getContext())),
                actionButton("Уведомления", 0xfff2b84b, v -> openNotificationSettings()));

        LinearLayout access = card("Доступ");
        row.addView(access, weightedCardLp());
        access.addView(body("Музыка берется из TEYES/SPD bridge, widget, MediaSession и уведомлений. На голове нужен доступ к уведомлениям."), textBlockLp());
        addButtonRow(access,
                actionButton("Уведомления", 0xfff2b84b, v -> openNotificationSettings()),
                actionButton("Настройки", 0xff2f5f8f, v -> getContext().startActivity(new Intent(getContext(), CanbusSettingsActivity.class))));
    }

    private void buildVehicle() {
        LinearLayout metrics = row();
        content.addView(metrics, sectionLp());
        speedValue = metric(metrics, "Скорость", "--");
        rpmValue = metric(metrics, "Обороты", "--");
        tempValue = metric(metrics, "ОЖ", "--");
        voltageValue = metric(metrics, "АКБ", "--");

        LinearLayout row = row();
        content.addView(row, sectionTopLp());
        LinearLayout status = card("CAN состояние");
        row.addView(status, weightedCardLp());
        vehicleStatusValue = info(status, "Машина", "");
        blindSpotValue = info(status, "RCTA", "");
        status.addView(switchRow("OBD / Vehicle", AppPrefs.obdEnabled(getContext()), (button, checked) -> {
            AppPrefs.setObdEnabled(getContext(), checked);
            if (checked) ObdMonitor.restart(getContext());
            else ObdMonitor.stop(getContext());
            toast("OBD " + yes(checked));
            refresh();
        }), controlLp());

        LinearLayout tpms = card("TPMS");
        row.addView(tpms, weightedCardLp());
        tpmsStatusValue = info(tpms, "Шины", "");
        tpms.addView(switchRow("TPMS экран и тревоги", AppPrefs.tpmsEnabled(getContext()), (button, checked) -> {
            AppPrefs.setTpmsEnabled(getContext(), checked);
            if (checked) TpmsMonitor.restart(getContext());
            else TpmsMonitor.stop();
            toast("TPMS " + yes(checked));
            refresh();
        }), controlLp());
        addButtonRow(tpms,
                actionButton("Vehicle", 0xff2f5f8f, v -> getContext().startActivity(new Intent(getContext(), VehicleInfoActivity.class))),
                actionButton("TPMS", 0xff2f5f8f, v -> getContext().startActivity(new Intent(getContext(), TpmsActivity.class))));
    }

    private void buildDiagnostics() {
        LinearLayout row = row();
        content.addView(row, sectionLp());
        LinearLayout adapter = card("V20 адаптер");
        row.addView(adapter, weightedCardLp());
        adapterV20Value = info(adapter, "Health", "");
        adapterApiValue = info(adapter, "API", "");
        adapterCapsValue = info(adapter, "Caps", "");
        adapterUidValue = info(adapter, "UID", "");
        addButtonRow(adapter,
                actionButton("0x79", 0xff1f7a67, v -> CanbusControl.requestV20Status(getContext())),
                actionButton("0x56/0x60", 0xff2f5f8f, v -> CanbusControl.requestAdapterInfo(getContext())));

        LinearLayout raw = card("Raw CAN");
        row.addView(raw, weightedCardLp());
        canCounterValue = info(raw, "Счетчики", "");
        canRecordButton = actionButton("", 0xff2f5f8f, v -> {
            SidebandDebugState.Snapshot debug = SidebandDebugState.snapshot();
            SidebandDebugState.setCanRecording(getContext(), !debug.canRecording);
            refresh();
        });
        canModeButton = actionButton("", 0xff2f5f8f, v -> {
            AppPrefs.setCanLogMode(getContext(), (AppPrefs.canLogMode(getContext()) + 1) % 3);
            refresh();
        });
        addButtonRow(raw, canRecordButton, canModeButton);
        addButtonRow(raw,
                actionButton("Stream on", 0xff1f7a67, v -> CanbusControl.startCanStream(getContext())),
                actionButton("Stream off", 0xff6a4752, v -> CanbusControl.stopCanStream(getContext())));

        LinearLayout preview = card("Последние CAN кадры");
        content.addView(preview, sectionTopLp());
        canPreviewValue = mono("");
        preview.addView(canPreviewValue, monoLp(116, 100));

        LinearLayout update = card("Обновление APK");
        content.addView(update, sectionTopLp());
        updateStatusValue = info(update, "Статус", "");
        updateReleaseValue = info(update, "GitHub", "");
        updateInstallButton = actionButton("Скачать", 0xff1f7a67, v -> downloadUpdate());
        addButtonRow(update,
                actionButton("Проверить", 0xff2f5f8f, v -> AppUpdater.checkNow(getContext())),
                updateInstallButton);

        LinearLayout firmware = card("Прошивка адаптера");
        content.addView(firmware, sectionTopLp());
        firmwareOtaStatusValue = info(firmware, "Статус", "");
        firmwareOtaAssetValue = info(firmware, "GitHub", "");
        firmwareOtaFlashButton = actionButton("Скачать / прошить", 0xff1f7a67, v -> flashFirmwareRelease());
        addButtonRow(firmware,
                actionButton("Проверить BIN", 0xff2f5f8f, v -> FirmwareReleaseUpdater.checkNow(getContext())),
                firmwareOtaFlashButton);

        LinearLayout log = card("Журнал");
        content.addView(log, sectionTopLp());
        appLogValue = mono("");
        log.addView(appLogValue, monoLp(150, 120));
    }

    private void resetFields() {
        adapterUsbValue = null;
        adapterV20Value = null;
        adapterApiValue = null;
        adapterCapsValue = null;
        adapterUidValue = null;
        adapterVersionValue = null;
        navStatusValue = null;
        navDebugValue = null;
        mediaStatusValue = null;
        vehicleStatusValue = null;
        speedValue = null;
        rpmValue = null;
        tempValue = null;
        voltageValue = null;
        tpmsStatusValue = null;
        blindSpotValue = null;
        canCounterValue = null;
        canPreviewValue = null;
        appLogValue = null;
        updateStatusValue = null;
        updateReleaseValue = null;
        firmwareOtaStatusValue = null;
        firmwareOtaAssetValue = null;
        navModeButton = null;
        canRecordButton = null;
        canModeButton = null;
        updateInstallButton = null;
        firmwareOtaFlashButton = null;
    }

    private void startStack() {
        AppService.start(getContext());
        ObdMonitor.restart(getContext());
        TpmsMonitor.start(getContext());
        CanbusControl.requestAdapterInfo(getContext());
        MediaMonitor.scanNow(getContext());
    }

    private void openNotificationSettings() {
        Context context = getContext();
        if (context instanceof Activity) {
            PermissionHelper.openNotificationListener((Activity) context);
        }
    }

    private void downloadUpdate() {
        Context context = getContext();
        if (context instanceof Activity) {
            AppUpdater.downloadAndInstall((Activity) context);
        }
    }

    private void flashFirmwareRelease() {
        Context context = getContext();
        if (context instanceof Activity) {
            FirmwareReleaseUpdater.downloadAndFlash((Activity) context);
        }
    }

    private LinearLayout.LayoutParams sectionLp() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams sectionTopLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(compactMode ? 10 : 12), 0, 0);
        return lp;
    }

    private LinearLayout.LayoutParams controlLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(compactMode ? 48 : 54));
        lp.setMargins(0, dp(6), 0, 0);
        return lp;
    }

    private LinearLayout.LayoutParams buttonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(compactMode ? 42 : 48));
        lp.setMargins(0, dp(8), 0, 0);
        return lp;
    }

    private LinearLayout.LayoutParams textBlockLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(8));
        return lp;
    }

    private LinearLayout.LayoutParams monoLp(int normalDp, int compactDp) {
        return new LinearLayout.LayoutParams(-1, dp(compactMode ? compactDp : normalDp));
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(compactMode ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        return row;
    }

    private LinearLayout card(String title) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(compactMode ? 12 : 16), dp(compactMode ? 11 : 14),
                dp(compactMode ? 12 : 16), dp(compactMode ? 12 : 14));
        card.setBackground(roundedStroke(0xfff4f1ea, 8, 0xffddd7ca));
        card.setElevation(dp(2));
        TextView label = text(title, compactMode ? 15 : 18, 0xff17191e, true);
        label.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        card.addView(label, new LinearLayout.LayoutParams(-1, dp(compactMode ? 26 : 32)));
        return card;
    }

    private LinearLayout.LayoutParams weightedCardLp() {
        LinearLayout.LayoutParams lp = compactMode
                ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(0, 0, compactMode ? 0 : dp(12), compactMode ? dp(10) : 0);
        return lp;
    }

    private TextView metric(LinearLayout parent, String label, String value) {
        LinearLayout box = card(label);
        LinearLayout.LayoutParams lp = compactMode
                ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(0, 0, compactMode ? 0 : dp(12), compactMode ? dp(10) : 0);
        parent.addView(box, lp);
        TextView data = text(value, compactMode ? 22 : 29, 0xff111317, true);
        data.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        box.addView(data, new LinearLayout.LayoutParams(-1, -2));
        return data;
    }

    private TextView info(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));

        TextView name = text(label, compactMode ? 11 : 12, 0xff68717e, true);
        name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        row.addView(name, new LinearLayout.LayoutParams(dp(compactMode ? 62 : 76), -2));

        TextView text = text(value, compactMode ? 12 : 13, 0xff17191e, false);
        text.setGravity(Gravity.LEFT | Gravity.TOP);
        text.setSingleLine(false);
        text.setMaxLines(compactMode ? 3 : 2);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setLineSpacing(dp(1), 1f);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        return text;
    }

    private void addButtonRow(LinearLayout parent, Button left, Button right) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(compactMode ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, compactMode ? -2 : dp(48));
        rowLp.setMargins(0, dp(8), 0, 0);
        parent.addView(row, rowLp);
        if (compactMode) {
            LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(-1, dp(42));
            first.setMargins(0, 0, 0, dp(8));
            row.addView(left, first);
            row.addView(right, new LinearLayout.LayoutParams(-1, dp(42)));
        } else {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
            lp.setMargins(0, 0, dp(8), 0);
            row.addView(left, lp);
            row.addView(right, new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    private View switchRow(String label, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(10), 0);
        row.setBackground(roundedStroke(0xffffffff, 8, 0xffd7d1c5));

        TextView text = text(label, compactMode ? 12 : 14, 0xff17191e, true);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setSingleLine(false);
        row.addView(text, new LinearLayout.LayoutParams(0, -1, 1));

        Switch toggle = new Switch(getContext());
        toggle.setShowText(false);
        toggle.setText(null);
        toggle.setChecked(checked);
        tintSwitch(toggle);
        toggle.setOnCheckedChangeListener(listener);
        row.addView(toggle, new LinearLayout.LayoutParams(dp(70), -1));
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        return row;
    }

    private TextView body(String value) {
        TextView text = text(value, compactMode ? 12 : 14, 0xff3a414b, false);
        text.setSingleLine(false);
        text.setLineSpacing(dp(2), 1f);
        return text;
    }

    private TextView mono(String value) {
        TextView text = body(value);
        text.setTypeface(Typeface.MONOSPACE);
        text.setTextSize(12);
        text.setPadding(dp(12), dp(10), dp(12), dp(10));
        text.setBackground(rounded(0xffffffff, 8));
        return text;
    }

    private Button actionButton(String label, int color, View.OnClickListener listener) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setTextColor(color == 0xfff2b84b ? 0xff15171c : 0xffffffff);
        button.setTextSize(compactMode ? 12 : 14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setBackground(rounded(color, 8));
        button.setOnClickListener(listener);
        return button;
    }

    private TextView chip(String value) {
        TextView chip = text(value, compactMode ? 11 : 13, 0xff17191e, true);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setPadding(dp(10), 0, dp(10), 0);
        chip.setBackground(roundedStroke(0xffffffff, 8, 0xffd7d1c5));
        return chip;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(sp);
        text.setIncludeFontPadding(true);
        if (bold) text.setTypeface(Typeface.DEFAULT_BOLD);
        return text;
    }

    private void tintSwitch(Switch toggle) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        toggle.setThumbTintList(new ColorStateList(states, new int[]{0xffffffff, 0xffffffff}));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{0xff1f7a67, 0xff8d929a}));
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

    private String subtitle(int tab) {
        switch (tab) {
            case TAB_NAV:
                return "Маршрут, TBT, компас и тестовые кадры";
            case TAB_MEDIA:
                return "Источник, артист, трек и отправка на адаптер";
            case TAB_VEHICLE:
                return "OBD, snapshot, TPMS и предупреждения";
            case TAB_DIAG:
                return "V20 health, raw stream, ACK и журнал";
            case TAB_HOME:
            default:
                return "Планшетная панель управления адаптером";
        }
    }

    private String chipFirmware(CanbusControl.Snapshot can) {
        if (can == null) return "V20: ?";
        String age = can.lastV20At == 0 ? "" : " " + ageText(System.currentTimeMillis() - can.lastV20At);
        return "V20: " + can.v20Status + age;
    }

    private String chipUsb(String value) {
        String clean = cleanUsb(value);
        return TextUtils.isEmpty(clean) ? "USB: ожидание" : clean;
    }

    private String cleanUsb(String value) {
        if (TextUtils.isEmpty(value)) return "USB: ожидание адаптера";
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("usb-адаптер не найден") || lower.contains("адаптер не подключ")) {
            return "USB: ожидание адаптера";
        }
        return value;
    }

    private String cleanVehicle(VehicleDisplayState.Snapshot vehicle) {
        if (vehicle == null || vehicle.status == null) return "нет данных";
        String lower = vehicle.status.toLowerCase(Locale.ROOT);
        if (lower.contains("адаптер не подключ") || lower.contains("usb-адаптер не найден")) return "нет данных";
        return vehicle.status;
    }

    private String tpmsText(TpmsState.Snapshot tpms) {
        if (tpms == null) return "нет данных";
        return tpms.status + " / " + (tpms.connected ? "online" : "offline");
    }

    private String navDebugText() {
        NavDebugState.Snapshot nav = NavDebugState.snapshot();
        return "TEYES: " + shortLine(nav.lastTeyes) + "\n"
                + "Intent: " + shortLine(nav.lastEvent) + "\n"
                + "CAN: " + shortLine(nav.lastFrame);
    }

    private String shortLine(String value) {
        if (TextUtils.isEmpty(value)) return "-";
        String clean = value.replace('\n', ' ').replace('\r', ' ').trim();
        return clean.length() > 190 ? clean.substring(0, 190) + "..." : clean;
    }

    private String shortLog(String value) {
        if (TextUtils.isEmpty(value)) return "";
        String clean = value.trim();
        int max = 1700;
        return clean.length() > max ? clean.substring(clean.length() - max) : clean;
    }

    private String navModeText() {
        int mode = AppPrefs.navTextMode(getContext());
        if (mode == 1) return "Текст: speed limit";
        if (mode == 2) return "Текст: по превышению";
        return "Текст: улица";
    }

    private String updateText(AppUpdater.Snapshot update) {
        if (update == null) return "";
        String text = "текущая " + dash(update.currentVersion);
        if (update.latestRelease > 0 || !TextUtils.isEmpty(update.assetName)) {
            text += " / latest " + (update.latestRelease > 0 ? update.latestRelease + " " : "") + dash(update.assetName);
        }
        if (update.downloading) {
            text += " / " + downloadProgress(update.downloadedBytes, update.totalBytes);
        }
        return text;
    }

    private String firmwareText(FirmwareReleaseUpdater.Snapshot firmware) {
        if (firmware == null) return "";
        String text = dash(firmware.assetName);
        if (firmware.assetSize > 0) text += " / " + (firmware.assetSize / 1024) + " KB";
        if (firmware.downloading || firmware.flashing) {
            text += " / " + downloadProgress(firmware.downloadedBytes, firmware.totalBytes);
        }
        return text;
    }

    private String downloadProgress(long done, long total) {
        if (total <= 0) return (done / 1024) + " KB";
        return Math.min(100, Math.round(done * 100f / total)) + "%";
    }

    private String canModeLabel() {
        int mode = AppPrefs.canLogMode(getContext());
        if (mode == 0) return "M-CAN";
        if (mode == 1) return "C-CAN";
        return "оба";
    }

    private String ageText(long ms) {
        long seconds = Math.max(0, ms / 1000);
        if (seconds < 60) return seconds + "с";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "м";
        return (minutes / 60) + "ч";
    }

    private String dash(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    private String yes(boolean value) {
        return value ? "вкл" : "выкл";
    }

    private void toast(String value) {
        Toast.makeText(getContext(), value, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class SpaceFill extends View {
        SpaceFill(Context context) {
            super(context);
        }
    }
}
