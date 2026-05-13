package kia.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class TpmsActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TpmsDashboardView view;
    private FrameLayout warningBanner;
    private TextView warningText;
    private Button backButton;
    private Button moreButton;
    private boolean warningDismissed;
    private boolean blinkOn = true;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable blink = new Runnable() {
        @Override
        public void run() {
            blinkOn = !blinkOn;
            if (warningBanner != null && warningBanner.getVisibility() == View.VISIBLE) {
                warningBanner.setAlpha(blinkOn ? 1f : 0.55f);
                handler.postDelayed(this, 360);
            }
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppPrefs.tpmsEnabled(this)) {
            finish();
            return;
        }
        UiUtils.enterImmersive(this);
        buildUi();
        TpmsMonitor.start(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(TpmsState.ACTION_STATE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
        handler.post(tick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AppPrefs.tpmsEnabled(this)) {
            finish();
            return;
        }
        UiUtils.enterImmersive(this);
        TpmsMonitor.start(this);
        configureTopButton();
        refresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(tick);
        handler.removeCallbacks(blink);
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

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff02070b);
        view = new TpmsDashboardView(this);
        root.addView(view, new FrameLayout.LayoutParams(-1, -1));

        backButton = iconBackButton();
        configureTopButton();
        FrameLayout.LayoutParams backLp = new FrameLayout.LayoutParams(dp(80), dp(80), Gravity.RIGHT | Gravity.TOP);
        backLp.setMargins(0, dp(18), dp(22), 0);
        root.addView(backButton, backLp);

        warningBanner = buildWarningBanner();
        root.addView(warningBanner, new FrameLayout.LayoutParams(-1, dp(75), Gravity.TOP));

        setContentView(root);
        refresh();
    }

    private Button titleBarButton(String text, int textSizeDp) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(0xffffffff);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(textSizeDp));
        button.setTypeface(Typeface.DEFAULT);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);
        button.setIncludeFontPadding(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundResource(R.drawable.tpms_buton_style);
        button.setShadowLayer(dp(3), dp(2), dp(2), 0xff000000);
        return button;
    }

    private Button iconBackButton() {
        Button button = new Button(this);
        button.setText("");
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    private void configureTopButton() {
        if (backButton == null) return;
        if (AppPrefs.obdEnabled(this)) {
            backButton.setBackgroundResource(R.drawable.back_btn);
            backButton.setOnClickListener(v -> finish());
        } else {
            backButton.setBackgroundResource(R.drawable.btn_home_setting);
            backButton.setOnClickListener(v -> startActivity(new Intent(this, CanbusSettingsActivity.class)));
        }
    }

    private FrameLayout buildWarningBanner() {
        FrameLayout banner = new FrameLayout(this);
        banner.setBackgroundResource(R.drawable.tpms_warning_info);
        banner.setElevation(dp(40));
        banner.setVisibility(View.GONE);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.tpms_warning_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams logoLp = new FrameLayout.LayoutParams(dp(54), dp(54), Gravity.LEFT | Gravity.TOP);
        logoLp.setMargins(dp(40), dp(10), 0, 0);
        banner.addView(logo, logoLp);

        warningText = new TextView(this);
        warningText.setTextColor(0xffffffff);
        warningText.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(30));
        warningText.setTypeface(Typeface.DEFAULT_BOLD);
        warningText.setGravity(Gravity.CENTER);
        warningText.setSingleLine(true);
        warningText.setEllipsize(TextUtils.TruncateAt.END);
        warningText.setIncludeFontPadding(false);
        warningText.setShadowLayer(dp(3), dp(2), dp(2), 0xff000000);
        FrameLayout.LayoutParams textLp = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        textLp.setMargins(dp(110), 0, dp(110), 0);
        banner.addView(warningText, textLp);

        ImageButton close = new ImageButton(this);
        close.setBackgroundResource(R.drawable.tpms_warning_close_style);
        close.setPadding(0, 0, 0, 0);
        close.setScaleType(ImageView.ScaleType.FIT_CENTER);
        close.setOnClickListener(v -> {
            warningDismissed = true;
            TpmsAlertManager.suppressAfterUserClose(this);
            updateWarningBanner(TpmsState.snapshot());
        });
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(dp(43), dp(43), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        closeLp.setMargins(0, 0, dp(30), 0);
        banner.addView(close, closeLp);

        return banner;
    }

    private void refresh() {
        TpmsState.Snapshot snapshot = TpmsState.snapshot();
        if (view != null) view.setSnapshot(snapshot);
        updateWarningBanner(snapshot);
    }

    private void updateWarningBanner(TpmsState.Snapshot snapshot) {
        if (warningBanner == null) return;
        boolean alert = hasAlert(snapshot);
        if (!alert) warningDismissed = false;
        boolean visible = alert && !warningDismissed && !TpmsAlertManager.isSuppressed(this);
        if (view != null) view.setTitleSuppressed(visible);
        if (backButton != null) backButton.setVisibility(visible ? View.GONE : View.VISIBLE);
        if (moreButton != null) moreButton.setVisibility(visible ? View.GONE : View.VISIBLE);
        if (visible) {
            if (warningText != null) warningText.setText(warningText(snapshot));
            warningBanner.setVisibility(View.VISIBLE);
            warningBanner.bringToFront();
            handler.removeCallbacks(blink);
            handler.post(blink);
        } else {
            warningBanner.setVisibility(View.GONE);
            warningBanner.setAlpha(1f);
            handler.removeCallbacks(blink);
        }
    }

    private boolean hasAlert(TpmsState.Snapshot snapshot) {
        if (snapshot == null || snapshot.tires == null) return false;
        for (TpmsState.Tire tire : snapshot.tires) {
            if (tire != null && tire.alert()) return true;
        }
        return false;
    }

    private String warningText(TpmsState.Snapshot snapshot) {
        if (snapshot == null || snapshot.tires == null) return "Внимание, проверьте давление в шинах";
        String[] labels = {"Л.П.", "П.П.", "Л.З.", "П.З."};
        for (int i = 0; i < snapshot.tires.length; i++) {
            TpmsState.Tire tire = snapshot.tires[i];
            if (tire != null && tire.alert()) {
                String label = i < labels.length ? labels[i] : tire.label;
                return label + ": " + tire.warningText().toLowerCase(java.util.Locale.ROOT) + ", проверьте";
            }
        }
        return "Внимание, проверьте давление в шинах";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
