package kia.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private TextView navAdapterValue;
    private TextView[] navAdapterRowValues;
    private TextView mediaStatusValue;
    private TextView mediaPreviewValue;
    private TextView mediaClusterPreviewValue;
    private TextView vehicleStatusValue;
    private TextView speedValue;
    private TextView rpmValue;
    private TextView tempValue;
    private TextView cabinTempValue;
    private TextView climateTempValue;
    private TextView voltageValue;
    private TextView tripTimeValue;
    private LinearLayout homeNavCard;
    private LinearLayout homeMusicCard;
    private TextView homeNavPrimaryValue;
    private TextView homeNavSecondaryValue;
    private TextView homeMusicPrimaryValue;
    private TextView homeMusicSecondaryValue;
    private TextView tpmsStatusValue;
    private TextView blindSpotValue;
    private TextView canCounterValue;
    private TextView canPreviewValue;
    private TextView appLogValue;
    private TextView updateStatusValue;
    private TextView updateReleaseValue;
    private TextView firmwareOtaStatusValue;
    private TextView firmwareOtaAssetValue;
    private Button canRecordButton;
    private Button canModeButton;
    private Button updateInstallButton;
    private Button firmwareOtaFlashButton;
    private TurnSignalMode turnSignalMode = TurnSignalMode.OFF;
    private ImageView leftTurnIndicator;
    private FrameLayout rightTurnIndicator;
    private ImageView rightTurnRingIndicator;
    private ImageView rightTurnArrowIndicator;
    private ObjectAnimator leftTurnAnimation;
    private ObjectAnimator rightTurnAnimation;
    private int currentTab = TAB_HOME;
    private boolean compactMode;
    private boolean metricOnlyMode;
    private int lastWidthDp = -1;
    private int lastHeightDp = -1;
    private int homeLayoutSignature = -1;

    private enum TurnSignalMode {
        OFF,
        LEFT,
        RIGHT,
        HAZARD
    }

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
        int heightDp = Math.round(h / density);
        boolean nextMetricOnly = widthDp < 700 || heightDp < 430;
        boolean nextCompact = nextMetricOnly || widthDp < 900 || heightDp < 520;
        if (nextCompact != compactMode
                || nextMetricOnly != metricOnlyMode
                || Math.abs(widthDp - lastWidthDp) > 180
                || Math.abs(heightDp - lastHeightDp) > 120) {
            compactMode = nextCompact;
            metricOnlyMode = nextMetricOnly;
            lastWidthDp = widthDp;
            lastHeightDp = heightDp;
            buildShell();
        }
    }

    void refresh() {
        if (currentTab == TAB_HOME) {
            int signature = homeLayoutSignature();
            if (signature != homeLayoutSignature) {
                buildHome();
                refresh();
                return;
            }
        }
        CanbusControl.Snapshot can = CanbusControl.snapshot();
        VehicleDisplayState.Snapshot vehicle = VehicleDisplayState.snapshot();
        ClimateState.Snapshot climate = ClimateState.snapshot();
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
        if (navAdapterValue != null) navAdapterValue.setText(NavProtocol.adapterStateText());
        refreshNavAdapterTable();
        if (mediaStatusValue != null) mediaStatusValue.setText(AppLog.media());
        if (mediaPreviewValue != null) mediaPreviewValue.setText(MediaMonitor.settingsPreview(getContext()));
        if (mediaClusterPreviewValue != null) mediaClusterPreviewValue.setText(MediaMonitor.clusterPreview(getContext()));
        if (vehicleStatusValue != null) vehicleStatusValue.setText(cleanVehicle(vehicle));
        if (speedValue != null) speedValue.setText(vehicle.speedText(getContext()));
        if (rpmValue != null) rpmValue.setText(vehicle.rpm + " rpm");
        if (tempValue != null) tempValue.setText(vehicle.tempText(getContext(), vehicle.coolantTemp));
        if (cabinTempValue != null) cabinTempValue.setText(vehicle.tempText(getContext(), vehicle.intakeTemp));
        if (climateTempValue != null) climateTempValue.setText(climate.text(getContext()));
        if (voltageValue != null) voltageValue.setText(vehicle.voltageText());
        if (tripTimeValue != null) tripTimeValue.setText(homeTripSummary(vehicle));
        HomeSummary navSummary = homeNavSummary();
        if (homeNavCard != null) homeNavCard.setVisibility(navSummary.visible ? View.VISIBLE : View.GONE);
        if (homeNavPrimaryValue != null) homeNavPrimaryValue.setText(navSummary.primary);
        if (homeNavSecondaryValue != null) homeNavSecondaryValue.setText(navSummary.secondary);
        HomeSummary musicSummary = homeMusicSummary();
        if (homeMusicCard != null) homeMusicCard.setVisibility(musicSummary.visible ? View.VISIBLE : View.GONE);
        if (homeMusicPrimaryValue != null) homeMusicPrimaryValue.setText(musicSummary.primary);
        if (homeMusicSecondaryValue != null) homeMusicSecondaryValue.setText(musicSummary.secondary);
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
                boolean showInstall = update.updateAvailable || update.downloaded || update.downloading;
                updateInstallButton.setVisibility(showInstall ? View.VISIBLE : View.GONE);
                updateInstallButton.setEnabled(showInstall);
                updateInstallButton.setText(update.downloaded ? "Установить"
                        : (update.downloading ? "Загрузка" : "Скачать"));
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
                firmwareOtaFlashButton.setText(firmware.downloading || firmware.flashing ? "Процесс"
                        : (firmware.downloaded ? "Прошить" : "Скачать и прошить"));
            }
        }
        if (canRecordButton != null) canRecordButton.setText(debug.canRecording ? "Остановить запись" : "Начать запись");
        if (canModeButton != null) canModeButton.setText("Лог: " + canModeLabel());
    }

    private void buildShell() {
        removeAllViews();
        stopTurnSignalAnimation();
        resetFields();
        setBackgroundColor(0xff0e1014);
        currentTab = TAB_HOME;

        content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        addView(content, new FrameLayout.LayoutParams(-1, -1));

        buildHome();
        refresh();
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
        onTabSelected(currentTab);
        refresh();
    }

    private void onTabSelected(int tab) {
        if (tab == TAB_DIAG) {
            AppService.start(getContext());
            CanbusControl.requestAdapterInfo(getContext());
            CanbusControl.requestV20Status(getContext());
        } else if (tab == TAB_MEDIA) {
            MediaMonitor.scanNow(getContext());
        }
    }

    private void buildCurrentTab() {
        if (content == null) return;
        stopTurnSignalAnimation();
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
        content.removeAllViews();
        homeLayoutSignature = homeLayoutSignature();

        FrameLayout home = new FrameLayout(getContext());
        home.setPadding(dp(compactMode ? 16 : 28), dp(compactMode ? 14 : 24),
                dp(compactMode ? 16 : 28), dp(compactMode ? 14 : 24));
        home.setBackground(rounded(0xff0e1014, 0));
        content.addView(home, new LinearLayout.LayoutParams(-1, -1));

        if (metricOnlyMode) {
            buildMetricOnlyHome(home);
            return;
        }

        FrameLayout carContainer = new FrameLayout(getContext());
        carContainer.setClipChildren(false);
        carContainer.setClipToPadding(false);
        carContainer.setClickable(true);
        carContainer.setFocusable(true);
        carContainer.setContentDescription("Нажмите для поворотников/аварийки.");
        carContainer.setOnClickListener(v -> cycleTurnSignalMode());
        FrameLayout.LayoutParams carContainerLp = new FrameLayout.LayoutParams(dp(compactMode ? 330 : 520), -2,
                Gravity.CENTER);
        carContainerLp.setMargins(0, dp(compactMode ? 12 : 18), 0, dp(compactMode ? 8 : 14));
        home.addView(carContainer, carContainerLp);

        ImageView car = new ImageView(getContext());
        car.setImageResource(R.drawable.mid);
        car.setAdjustViewBounds(true);
        car.setScaleType(ImageView.ScaleType.FIT_CENTER);
        car.setAlpha(0.98f);
        car.setElevation(0f);
        car.setTranslationZ(0f);
        car.setContentDescription("Изображение автомобиля. Нажмите для поворотников/аварийки.");
        FrameLayout.LayoutParams carLp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        carContainer.addView(car, carLp);

        FrameLayout turnIndicatorLayer = new FrameLayout(getContext());
        turnIndicatorLayer.setClickable(false);
        turnIndicatorLayer.setFocusable(false);
        turnIndicatorLayer.setTranslationZ(10f);
        turnIndicatorLayer.setElevation(10f);
        FrameLayout.LayoutParams indicatorLayerLp = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        carContainer.addView(turnIndicatorLayer, indicatorLayerLp);
        turnIndicatorLayer.bringToFront();
        buildTurnIndicatorLayer(turnIndicatorLayer);
        applyTurnSignalMode();

        LinearLayout left = metricColumn();
        FrameLayout.LayoutParams leftLp = new FrameLayout.LayoutParams(dp(compactMode ? 168 : 236), -2,
                Gravity.LEFT | Gravity.CENTER_VERTICAL);
        home.addView(left, leftLp);
        LinearLayout right = metricColumn();
        FrameLayout.LayoutParams rightLp = new FrameLayout.LayoutParams(dp(compactMode ? 168 : 236), -2,
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        home.addView(right, rightLp);
        addOrderedHomeWidgets(left, right, true);

        LinearLayout icons = topActionsRow();
        FrameLayout.LayoutParams iconsLp = new FrameLayout.LayoutParams(-2, dp(compactMode ? 50 : 60),
                Gravity.RIGHT | Gravity.TOP);
        home.addView(icons, iconsLp);
    }

    private void buildMetricOnlyHome(FrameLayout home) {
        ScrollView scroll = new ScrollView(getContext());
        scroll.setFillViewport(false);
        home.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout shell = new LinearLayout(getContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(0, 0, 0, dp(10));
        scroll.addView(shell, new ScrollView.LayoutParams(-1, -2));

        LinearLayout icons = topActionsRow();
        icons.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams iconsLp = new LinearLayout.LayoutParams(-1, dp(50));
        iconsLp.setMargins(0, 0, 0, dp(8));
        shell.addView(icons, iconsLp);

        int widthDp = lastWidthDp > 0 ? lastWidthDp : Math.round(getWidth() / getResources().getDisplayMetrics().density);
        boolean twoColumns = widthDp >= 520;
        LinearLayout metrics = new LinearLayout(getContext());
        metrics.setOrientation(twoColumns ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        shell.addView(metrics, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout left = metricColumn();
        LinearLayout right = metricColumn();
        if (twoColumns) {
            LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, -2, 1);
            leftLp.setMargins(0, 0, dp(10), 0);
            metrics.addView(left, leftLp);
            metrics.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
        } else {
            metrics.addView(left, new LinearLayout.LayoutParams(-1, -2));
        }

        addOrderedHomeWidgets(left, twoColumns ? right : left, false);
    }

    private void addOrderedHomeWidgets(LinearLayout left, LinearLayout right, boolean carMode) {
        Context context = getContext();
        String[] order = AppPrefs.homeWidgetOrder(context);
        int enabledIndex = 0;
        for (String id : order) {
            if (!AppPrefs.homeWidgetEnabled(context, id)) continue;
            LinearLayout target = carMode
                    ? (enabledIndex < 4 ? left : right)
                    : (left == right || enabledIndex % 2 == 0 ? left : right);
            addHomeWidget(target, id);
            enabledIndex++;
        }
    }

    private void addHomeWidget(LinearLayout parent, String id) {
        if (AppPrefs.HOME_WIDGET_SPEED.equals(id)) {
            speedValue = homeMetric(parent, "Скорость", "--");
        } else if (AppPrefs.HOME_WIDGET_RPM.equals(id)) {
            rpmValue = homeMetric(parent, "Обороты", "--");
        } else if (AppPrefs.HOME_WIDGET_NAV.equals(id)) {
            HomeInfoCard navCard = homeInfoMetric(parent, "Навигация");
            homeNavCard = navCard.card;
            homeNavPrimaryValue = navCard.primary;
            homeNavSecondaryValue = navCard.secondary;
        } else if (AppPrefs.HOME_WIDGET_MUSIC.equals(id)) {
            HomeInfoCard musicCard = homeInfoMetric(parent, "Музыка");
            homeMusicCard = musicCard.card;
            homeMusicPrimaryValue = musicCard.primary;
            homeMusicSecondaryValue = musicCard.secondary;
        } else if (AppPrefs.HOME_WIDGET_ENGINE_TEMP.equals(id)) {
            tempValue = homeMetric(parent, "Двигатель", "--");
        } else if (AppPrefs.HOME_WIDGET_OUTSIDE_TEMP.equals(id)) {
            cabinTempValue = homeMetric(parent, "Улица", "--");
        } else if (AppPrefs.HOME_WIDGET_CLIMATE_TEMP.equals(id)) {
            climateTempValue = homeMetric(parent, "Климат", "--");
        } else if (AppPrefs.HOME_WIDGET_VOLTAGE.equals(id)) {
            voltageValue = homeMetric(parent, "Вольтаж", "--");
        } else if (AppPrefs.HOME_WIDGET_TRIP.equals(id)) {
            tripTimeValue = homeTripMetric(parent);
        }
    }

    private LinearLayout topActionsRow() {
        LinearLayout icons = new LinearLayout(getContext());
        icons.setOrientation(LinearLayout.HORIZONTAL);
        icons.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        ImageButton settings = topActionButton(R.drawable.ic_top_settings_24, 0xffffffff, "Настройки",
                v -> openScreen(CanbusSettingsActivity.class));
        int iconSize = dp(compactMode ? 48 : 58);
        if (AppPrefs.tpmsEnabled(getContext())) {
            ImageButton tpms = topActionButton(R.drawable.ic_top_tpms_24, 0xffffa640, "TPMS",
                    v -> openScreen(TpmsActivity.class));
            LinearLayout.LayoutParams tpmsLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            tpmsLp.setMargins(0, 0, dp(10), 0);
            icons.addView(tpms, tpmsLp);
        }
        icons.addView(settings, new LinearLayout.LayoutParams(iconSize, iconSize));
        return icons;
    }

    private int homeLayoutSignature() {
        Context context = getContext();
        int signature = 0;
        if (AppPrefs.tpmsEnabled(context)) signature |= 1;
        if (AppPrefs.homeWidgetSpeed(context)) signature |= 1 << 1;
        if (AppPrefs.homeWidgetRpm(context)) signature |= 1 << 2;
        if (AppPrefs.homeWidgetNav(context)) signature |= 1 << 3;
        if (AppPrefs.homeWidgetMusic(context)) signature |= 1 << 4;
        if (AppPrefs.homeWidgetEngineTemp(context)) signature |= 1 << 5;
        if (AppPrefs.homeWidgetCabinTemp(context)) signature |= 1 << 6;
        if (AppPrefs.homeWidgetClimateTemp(context)) signature |= 1 << 7;
        if (AppPrefs.homeWidgetVoltage(context)) signature |= 1 << 8;
        if (AppPrefs.homeWidgetTrip(context)) signature |= 1 << 9;
        return signature * 31 + AppPrefs.homeWidgetOrderSignature(context);
    }

    private void cycleTurnSignalMode() {
        switch (turnSignalMode) {
            case OFF:
                turnSignalMode = TurnSignalMode.LEFT;
                break;
            case LEFT:
                turnSignalMode = TurnSignalMode.RIGHT;
                break;
            case RIGHT:
                turnSignalMode = TurnSignalMode.HAZARD;
                break;
            case HAZARD:
            default:
                turnSignalMode = TurnSignalMode.OFF;
                break;
        }
        applyTurnSignalMode();
    }

    private void applyTurnSignalMode() {
        stopTurnSignalAnimation();
        if (leftTurnIndicator == null || rightTurnIndicator == null) {
            return;
        }
        leftTurnIndicator.setVisibility(View.INVISIBLE);
        rightTurnIndicator.setVisibility(View.INVISIBLE);
        leftTurnIndicator.setAlpha(1f);
        rightTurnIndicator.setAlpha(1f);
        leftTurnIndicator.setScaleX(1f);
        leftTurnIndicator.setScaleY(1f);
        rightTurnIndicator.setScaleX(1f);
        rightTurnIndicator.setScaleY(1f);

        switch (turnSignalMode) {
            case LEFT:
                leftTurnIndicator.setVisibility(View.VISIBLE);
                leftTurnAnimation = startTurnFade(leftTurnIndicator);
                break;
            case RIGHT:
                rightTurnIndicator.setVisibility(View.VISIBLE);
                rightTurnAnimation = startTurnFade(rightTurnIndicator);
                break;
            case HAZARD:
                leftTurnIndicator.setVisibility(View.VISIBLE);
                rightTurnIndicator.setVisibility(View.VISIBLE);
                leftTurnAnimation = startTurnFade(leftTurnIndicator);
                rightTurnAnimation = startTurnFade(rightTurnIndicator);
                break;
            case OFF:
            default:
                break;
        }
    }

    private void buildTurnIndicatorLayer(FrameLayout parent) {
        leftTurnIndicator = new ImageView(getContext());
        leftTurnIndicator.setImageResource(R.drawable.left_turn_signal);
        leftTurnIndicator.setScaleType(ImageView.ScaleType.FIT_CENTER);
        leftTurnIndicator.setVisibility(View.INVISIBLE);
        leftTurnIndicator.setTranslationZ(12f);
        leftTurnIndicator.setElevation(12f);
        FrameLayout.LayoutParams leftLp = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        parent.addView(leftTurnIndicator, leftLp);

        rightTurnIndicator = new FrameLayout(getContext());
        rightTurnIndicator.setClickable(false);
        rightTurnIndicator.setFocusable(false);
        rightTurnIndicator.setVisibility(View.INVISIBLE);
        rightTurnIndicator.setTranslationZ(12f);
        rightTurnIndicator.setElevation(12f);
        FrameLayout.LayoutParams rightLp = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        parent.addView(rightTurnIndicator, rightLp);

        rightTurnRingIndicator = new ImageView(getContext());
        rightTurnRingIndicator.setImageResource(R.drawable.turn_signal_right_ring_overlay);
        rightTurnRingIndicator.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams ringLp = new FrameLayout.LayoutParams(-1, -1);
        rightTurnIndicator.addView(rightTurnRingIndicator, ringLp);
        rightTurnArrowIndicator = null;
    }

    private void stopTurnSignalAnimation() {
        if (leftTurnAnimation != null) {
            leftTurnAnimation.cancel();
            leftTurnAnimation = null;
        }
        if (rightTurnAnimation != null) {
            rightTurnAnimation.cancel();
            rightTurnAnimation = null;
        }
    }

    private ObjectAnimator startTurnFade(View target) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(target, "alpha", 1f, 0.18f);
        alpha.setDuration(520L);
        alpha.setRepeatMode(ValueAnimator.REVERSE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.start();
        return alpha;
    }

    private void buildNavigation() {
        LinearLayout row = row();
        content.addView(row, sectionLp());
        LinearLayout status = card("Маршрут");
        row.addView(status, weightedCardLp());
        navStatusValue = info(status, "Состояние", "");
        status.addView(body("Фиксированный режим: навигация отдает source, on/off, maneuver, ETA/distance, street и speed limit отдельными командами. Компас работает только без активного маршрута, чтобы не затирать 0x45 маневра."), textBlockLp());

        LinearLayout live = card("Что уходит в адаптер");
        row.addView(live, weightedCardLp());
        addNavAdapterTable(live);
        addButtonRow(live,
                actionButton("Обновить", 0xff2f5f8f, v -> refresh()),
                actionButton("Диагностика", 0xfff2b84b, v -> selectTab(TAB_DIAG)));

        LinearLayout debug = card("Входящие данные");
        content.addView(debug, sectionTopLp());
        navDebugValue = mono("");
        debug.addView(navDebugValue, monoLp(128, 112));
    }

    private void buildMedia() {
        LinearLayout row = row();
        content.addView(row, sectionLp());
        LinearLayout now = card("Текущий источник");
        row.addView(now, weightedCardLp());
        mediaStatusValue = info(now, "Сейчас", "");
        mediaPreviewValue = info(now, "В приложении", "");
        mediaClusterPreviewValue = info(now, "В приборку", "");
        addButtonRow(now,
                actionButton("Обновить данные", 0xff2f5f8f, v -> MediaMonitor.scanNow(getContext())),
                actionButton("Уведомления", 0xfff2b84b, v -> openNotificationSettings()));

        LinearLayout access = card("Доступ");
        row.addView(access, weightedCardLp());
        access.addView(body("Музыка берется из TEYES/SPD bridge, widget, MediaSession и уведомлений. На голове нужен доступ к уведомлениям."), textBlockLp());
        addButtonRow(access,
                actionButton("Уведомления", 0xfff2b84b, v -> openNotificationSettings()),
                actionButton("Настройки", 0xff2f5f8f, v -> openScreen(CanbusSettingsActivity.class)));
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
                actionButton("TPMS", 0xff2f5f8f, v -> openScreen(TpmsActivity.class)),
                actionButton("Настройки", 0xff2f5f8f, v -> openScreen(CanbusSettingsActivity.class)));
    }

    private void buildDiagnostics() {
        LinearLayout row = row();
        content.addView(row, sectionLp());
        LinearLayout adapter = card("Адаптер");
        row.addView(adapter, weightedCardLp());
        adapterV20Value = info(adapter, "Состояние", "");
        adapterApiValue = info(adapter, "API", "");
        adapterCapsValue = info(adapter, "Caps", "");
        adapterUidValue = info(adapter, "UID", "");
        addButtonRow(adapter,
                actionButton("Обновить", 0xff1f7a67, v -> CanbusControl.requestV20Status(getContext())),
                actionButton("ID / версия", 0xff2f5f8f, v -> CanbusControl.requestAdapterInfo(getContext())));

        LinearLayout raw = card("Raw CAN");
        row.addView(raw, weightedCardLp());
        canCounterValue = info(raw, "Счетчики", "");
        if (AppPrefs.debugCan(getContext())) {
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
        } else {
            info(raw, "Режим", "Vehicle/RCTA");
            info(raw, "Debug", "выкл");
        }

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
                actionButton("Проверить обновление", 0xff2f5f8f, v -> AppUpdater.checkNow(getContext())),
                updateInstallButton);

        LinearLayout firmware = card("Прошивка адаптера");
        content.addView(firmware, sectionTopLp());
        firmwareOtaStatusValue = info(firmware, "Статус", "");
        firmwareOtaAssetValue = info(firmware, "GitHub", "");
        firmwareOtaFlashButton = actionButton("Скачать и прошить", 0xff1f7a67, v -> flashFirmwareRelease());
        addButtonRow(firmware,
                actionButton("Проверить обновление", 0xff2f5f8f, v -> FirmwareReleaseUpdater.checkNow(getContext())),
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
        navAdapterValue = null;
        navAdapterRowValues = null;
        mediaStatusValue = null;
        mediaPreviewValue = null;
        mediaClusterPreviewValue = null;
        vehicleStatusValue = null;
        speedValue = null;
        rpmValue = null;
        tempValue = null;
        cabinTempValue = null;
        climateTempValue = null;
        voltageValue = null;
        tripTimeValue = null;
        homeNavCard = null;
        homeMusicCard = null;
        homeNavPrimaryValue = null;
        homeNavSecondaryValue = null;
        homeMusicPrimaryValue = null;
        homeMusicSecondaryValue = null;
        tpmsStatusValue = null;
        blindSpotValue = null;
        canCounterValue = null;
        canPreviewValue = null;
        appLogValue = null;
        updateStatusValue = null;
        updateReleaseValue = null;
        firmwareOtaStatusValue = null;
        firmwareOtaAssetValue = null;
        canRecordButton = null;
        canModeButton = null;
        updateInstallButton = null;
        firmwareOtaFlashButton = null;
        leftTurnIndicator = null;
        rightTurnArrowIndicator = null;
        rightTurnRingIndicator = null;
        rightTurnIndicator = null;
        leftTurnAnimation = null;
        rightTurnAnimation = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        stopTurnSignalAnimation();
        super.onDetachedFromWindow();
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

    private void openScreen(Class<?> cls) {
        Context context = getContext();
        Intent intent = new Intent(context, cls);
        if (context instanceof Activity) {
            UiUtils.startActivityWithTransition((Activity) context, intent);
        } else {
            context.startActivity(intent);
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

    private LinearLayout metricColumn() {
        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        return column;
    }

    private TextView homeMetric(LinearLayout parent, String label, String value) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(compactMode ? 12 : 18), dp(compactMode ? 9 : 13),
                dp(compactMode ? 12 : 18), dp(compactMode ? 10 : 14));
        box.setBackground(roundedStroke(0xee191d23, 8, 0xff343a44));
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(-1, dp(compactMode ? 76 : 92));
        boxLp.setMargins(0, 0, 0, dp(compactMode ? 12 : 12));
        parent.addView(box, boxLp);

        TextView name = text(label, compactMode ? 12 : 15, 0xff9aa3af, true);
        name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        box.addView(name, new LinearLayout.LayoutParams(-1, dp(compactMode ? 22 : 28)));

        TextView data = text(value, compactMode ? 24 : 30, 0xfff4f1ea, true);
        data.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        data.setSingleLine(true);
        data.setIncludeFontPadding(false);
        box.addView(data, new LinearLayout.LayoutParams(-1, 0, 1));
        return data;
    }

    private TextView homeTripMetric(LinearLayout parent) {
        TextView data = homeMetric(parent, "Поездка", "0.0 km");
        data.setTextSize(compactMode ? 18 : 24);
        return data;
    }

    private HomeInfoCard homeInfoMetric(LinearLayout parent, String label) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(compactMode ? 12 : 18), dp(compactMode ? 9 : 13),
                dp(compactMode ? 12 : 18), dp(compactMode ? 10 : 14));
        box.setBackground(roundedStroke(0xee191d23, 8, 0xff343a44));
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(-1, dp(compactMode ? 80 : 104));
        boxLp.setMargins(0, 0, 0, dp(compactMode ? 12 : 16));
        parent.addView(box, boxLp);

        TextView name = text(label, compactMode ? 11 : 13, 0xff9aa3af, true);
        name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        box.addView(name, new LinearLayout.LayoutParams(-1, dp(compactMode ? 20 : 24)));

        LinearLayout values = new LinearLayout(getContext());
        values.setOrientation(LinearLayout.VERTICAL);
        box.addView(values, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView top = text("--", compactMode ? 13 : 16, 0xfff4f1ea, true);
        top.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        top.setSingleLine(true);
        top.setEllipsize(TextUtils.TruncateAt.END);
        top.setIncludeFontPadding(false);
        values.addView(top, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView bottom = text("--", compactMode ? 11 : 13, 0xffc6ccd6, false);
        bottom.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        bottom.setSingleLine(true);
        bottom.setEllipsize(TextUtils.TruncateAt.END);
        bottom.setIncludeFontPadding(false);
        values.addView(bottom, new LinearLayout.LayoutParams(-1, 0, 1));
        return new HomeInfoCard(box, top, bottom);
    }

    private ImageButton iconButton(int backgroundRes, int imageRes, String description, View.OnClickListener listener) {
        ImageButton button = new ImageButton(getContext());
        button.setContentDescription(description);
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(9), dp(9), dp(9), dp(9));
        button.setAdjustViewBounds(false);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        if (backgroundRes != 0) {
            button.setBackgroundResource(backgroundRes);
        } else {
            button.setBackground(roundedStroke(0xdd191d23, 8, 0xff343a44));
        }
        if (imageRes != 0) button.setImageResource(imageRes);
        button.setOnClickListener(listener);
        return button;
    }

    private ImageButton topActionButton(int imageRes, int tintColor, String description, View.OnClickListener listener) {
        ImageButton button = iconButton(R.drawable.top_action_circle, imageRes, description, listener);
        button.setImageTintList(ColorStateList.valueOf(tintColor));
        int padding = dp(compactMode ? 12 : 14);
        button.setPadding(padding, padding, padding, padding);
        return button;
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

    private void addNavAdapterTable(LinearLayout parent) {
        String[][] rows = NavProtocol.adapterRows();
        navAdapterRowValues = new TextView[rows.length];
        for (int i = 0; i < rows.length; i++) {
            navAdapterRowValues[i] = info(parent, rows[i][0], rows[i][1]);
        }
    }

    private void refreshNavAdapterTable() {
        if (navAdapterRowValues == null) return;
        String[][] rows = NavProtocol.adapterRows();
        int count = Math.min(navAdapterRowValues.length, rows.length);
        for (int i = 0; i < count; i++) {
            if (navAdapterRowValues[i] != null) navAdapterRowValues[i].setText(rows[i][1]);
        }
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
                return "Маршрут и штатные команды адаптера";
            case TAB_MEDIA:
                return "Источник, артист, трек и отправка на адаптер";
            case TAB_VEHICLE:
                return "OBD, snapshot, TPMS и предупреждения";
            case TAB_DIAG:
                return "Адаптер, обновления, CAN log и журнал";
            case TAB_HOME:
            default:
                return "Планшетная панель управления адаптером";
        }
    }

    private String chipFirmware(CanbusControl.Snapshot can) {
        if (can == null) return "V21: ?";
        String age = can.lastV20At == 0 ? "" : " " + ageText(System.currentTimeMillis() - can.lastV20At);
        return "V21: " + can.v20Status + age;
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

    private HomeSummary homeNavSummary() {
        String[][] rows = NavProtocol.adapterRows();
        String route = normalizeHomeValue(navRow(rows, "Маршрут"));
        String maneuver = normalizeHomeValue(navPart(navRow(rows, "0x45"), 0));
        String distance = normalizeHomeValue(navPart(navRow(rows, "0x45"), 1));
        String eta = normalizeHomeValue(navPart(navRow(rows, "0x47"), 0));
        String street = normalizeHomeValue(navPart(navRow(rows, "0x4A"), 0));
        String limit = normalizeHomeValue(navPart(navRow(rows, "0x44"), 0));

        boolean hasData = hasHomeValue(street)
                || hasHomeValue(distance)
                || hasHomeValue(eta)
                || hasHomeValue(limit)
                || hasHomeValue(maneuver)
                || hasNavState(route);
        if (!hasData) {
            return new HomeSummary(false, "--", "--");
        }
        String primary = firstNonEmpty(
                hasHomeValue(street) ? "Улица: " + street : null,
                hasHomeValue(maneuver) ? "Манёвр: " + navManeuverText(maneuver) : null,
                hasHomeValue(limit) ? "Лимит: " + limit : null,
                navStateText(route)
        );
        String secondary = joinNonEmpty(" • ",
                hasHomeValue(limit) ? "Лимит " + limit : null,
                hasHomeValue(distance) ? "До манёвра " + distance : null,
                hasHomeValue(eta) ? "Осталось " + eta : null
        );
        if (TextUtils.isEmpty(secondary)) secondary = navStateText(route);
        return new HomeSummary(true, homeLine(primary), homeLine(secondary));
    }

    private HomeSummary homeMusicSummary() {
        String raw = stripPrefix(AppLog.media(), "Мультимедиа:");
        String source = normalizeHomeValue(mediaPart(raw, 0));
        String artist = normalizeHomeValue(mediaPart(raw, 1));
        String title = normalizeHomeValue(mediaPart(raw, 2));
        String duration = normalizeHomeValue(mediaPart(raw, 3));
        String pretty = normalizeHomeValue(mediaPart(raw, 4));

        boolean hasData = hasHomeValue(source) || hasHomeValue(artist) || hasHomeValue(title) || hasHomeValue(pretty);
        if (!hasData) {
            return new HomeSummary(false, "--", "--");
        }

        String sourceLabel = musicSourceLabel(source);
        String track = firstNonEmpty(
                joinNonEmpty(" — ", artist, title),
                hasHomeValue(title) ? title : null,
                hasHomeValue(artist) ? artist : null,
                hasHomeValue(pretty) ? pretty : null
        );
        if (hasHomeValue(duration) && !TextUtils.equals(duration, "--:--")
                && !TextUtils.equals(duration, "-")
                && !TextUtils.isEmpty(track) && track.length() < (compactMode ? 24 : 34)) {
            track += " • " + duration;
        }
        if (TextUtils.isEmpty(track)) track = "Источник активен";
        return new HomeSummary(true, homeLine(sourceLabel), homeLine(track));
    }

    private String homeTripTime(int runtimeSeconds) {
        int safe = Math.max(0, runtimeSeconds);
        int hours = safe / 3600;
        int minutes = (safe % 3600) / 60;
        int seconds = safe % 60;
        if (hours <= 0) {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String homeTripSummary(VehicleDisplayState.Snapshot vehicle) {
        if (vehicle == null) return "0.0 km / 00:00";
        return vehicle.tripDistanceText(getContext()) + " / " + homeTripTime(vehicle.runtimeSeconds);
    }

    private String navRow(String[][] rows, String key) {
        if (rows == null || key == null) return "";
        for (String[] row : rows) {
            if (row == null || row.length < 2) continue;
            if (key.equals(row[0])) return row[1];
        }
        return "";
    }

    private String navPart(String value, int index) {
        if (TextUtils.isEmpty(value)) return "";
        String[] parts = value.split("\\s*/\\s*", -1);
        if (index < 0 || index >= parts.length) return "";
        return parts[index];
    }

    private boolean hasNavState(String route) {
        if (TextUtils.isEmpty(route)) return false;
        String lower = route.toLowerCase(Locale.ROOT);
        if ("off".equals(lower) || "idle".equals(lower)) return false;
        if (lower.startsWith("ignored")) return false;
        return true;
    }

    private String navStateText(String route) {
        if (TextUtils.isEmpty(route)) return "Маршрут не активен";
        if ("active".equals(route)) return "Маршрут активен";
        if (route.contains("finish")) return "Маршрут завершён";
        if (route.contains("waiting route")) return "Ожидание данных маршрута";
        return "Состояние: " + route;
    }

    private String navManeuverText(String value) {
        String clean = normalizeHomeValue(value);
        if (TextUtils.isEmpty(clean)) return "";
        clean = clean.replace("context_ra_", "").replace('_', ' ');
        return clean;
    }

    private String musicSourceLabel(String source) {
        String lower = source == null ? "" : source.toLowerCase(Locale.ROOT);
        if (lower.contains("carplay")) return "CarPlay";
        if (lower.contains("android auto") || lower.contains("androidauto")) return "Android Auto";
        if (lower.contains("bluetooth") || lower.contains("btmusic") || lower.contains("bt_music")
                || lower.contains("bt-audio") || lower.equals("bt")) return "BT музыка";
        if (lower.equals("fm") || lower.startsWith("fm ")) return "FM";
        if (lower.equals("am") || lower.startsWith("am ")) return "AM";
        if (lower.contains("usb") || lower.contains("local_music")) return "USB музыка";
        if (lower.contains("radio") || lower.contains("радио")) return "FM/AM";
        if (TextUtils.isEmpty(source)) return "Музыка";
        return source;
    }

    private String mediaPart(String value, int index) {
        if (TextUtils.isEmpty(value)) return "";
        String[] parts = value.split("\\s*/\\s*", -1);
        if (index < 0 || index >= parts.length) return "";
        return parts[index];
    }

    private String stripPrefix(String value, String prefix) {
        if (TextUtils.isEmpty(value)) return "";
        String out = value.trim();
        if (TextUtils.isEmpty(prefix)) return out;
        String lower = out.toLowerCase(Locale.ROOT);
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        if (lower.startsWith(lowerPrefix)) {
            out = out.substring(prefix.length()).trim();
        }
        if (out.startsWith(":")) out = out.substring(1).trim();
        return out;
    }

    private boolean hasHomeValue(String value) {
        if (TextUtils.isEmpty(value)) return false;
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return !"-".equals(lower)
                && !"--".equals(lower)
                && !"none".equals(lower)
                && !"unknown".equals(lower)
                && !"null".equals(lower)
                && !lower.contains("ожидание")
                && !lower.contains("нет данных");
    }

    private String normalizeHomeValue(String value) {
        if (value == null) return "";
        String out = value.replace('\n', ' ').replace('\r', ' ').trim();
        while (out.contains("  ")) out = out.replace("  ", " ");
        if (out.startsWith(":")) out = out.substring(1).trim();
        if (out.startsWith("- ")) out = out.substring(2).trim();
        return out;
    }

    private String homeLine(String value) {
        String clean = normalizeHomeValue(value);
        if (TextUtils.isEmpty(clean)) return "--";
        int max = compactMode ? 30 : 44;
        return clean.length() > max ? clean.substring(0, max - 3).trim() + "..." : clean;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            String clean = normalizeHomeValue(value);
            if (!TextUtils.isEmpty(clean)) return clean;
        }
        return "";
    }

    private String joinNonEmpty(String separator, String... values) {
        StringBuilder out = new StringBuilder();
        if (values == null) return "";
        for (String value : values) {
            String clean = normalizeHomeValue(value);
            if (TextUtils.isEmpty(clean)) continue;
            if (out.length() > 0) out.append(separator);
            out.append(clean);
        }
        return out.toString();
    }

    private String shortLog(String value) {
        if (TextUtils.isEmpty(value)) return "";
        String clean = value.trim();
        int max = 1700;
        return clean.length() > max ? clean.substring(clean.length() - max) : clean;
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

    private static final class HomeInfoCard {
        final LinearLayout card;
        final TextView primary;
        final TextView secondary;

        HomeInfoCard(LinearLayout card, TextView primary, TextView secondary) {
            this.card = card;
            this.primary = primary;
            this.secondary = secondary;
        }
    }

    private static final class HomeSummary {
        final boolean visible;
        final String primary;
        final String secondary;

        HomeSummary(boolean visible, String primary, String secondary) {
            this.visible = visible;
            this.primary = primary;
            this.secondary = secondary;
        }
    }

    private static final class SpaceFill extends View {
        SpaceFill(Context context) {
            super(context);
        }
    }
}
