package kia.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;

public class TpmsActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TpmsDashboardView view;
    private Button backButton;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, 1000);
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

        setContentView(root);
        refresh();
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

    private void refresh() {
        TpmsState.Snapshot snapshot = TpmsState.snapshot();
        if (view != null) view.setSnapshot(snapshot);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
