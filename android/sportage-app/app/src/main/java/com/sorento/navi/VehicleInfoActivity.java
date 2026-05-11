package com.sorento.navi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VehicleInfoActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.US);
    private InfoCell runtimeCell;
    private InfoCell dtcCell;
    private InfoCell voltageCell;
    private InfoCell intakeCell;
    private InfoCell loadCell;
    private InfoCell throttleCell;
    private InfoCell tempCell;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateValues();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppPrefs.obdEnabled(this)) {
            finish();
            return;
        }
        UiUtils.enterImmersive(this);
        buildUi();
        ObdMonitor.start(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AppPrefs.obdEnabled(this)) {
            finish();
            return;
        }
        UiUtils.enterImmersive(this);
        handler.post(tick);
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
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff03070c);
        root.addView(new VehicleChromeView(this), new FrameLayout.LayoutParams(-1, -1));

        Button back = button("BACK");
        back.setText("");
        back.setBackgroundResource(R.drawable.back_btn);
        back.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams backLp = new FrameLayout.LayoutParams(dp(80), dp(80), Gravity.RIGHT | Gravity.TOP);
        backLp.setMargins(0, dp(18), dp(22), 0);
        root.addView(back, backLp);

        Button dtc = button("DTC");
        dtc.setText("");
        dtc.setBackgroundResource(R.drawable.btn_dtc_detail);
        dtc.setOnClickListener(v -> startActivity(new Intent(this, TroubleCodeActivity.class)));
        FrameLayout.LayoutParams dtcLp = new FrameLayout.LayoutParams(dp(140), dp(60), Gravity.RIGHT | Gravity.TOP);
        dtcLp.setMargins(0, dp(28), dp(118), 0);
        root.addView(dtc, dtcLp);

        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(true);
        table.setShrinkAllColumns(false);
        FrameLayout.LayoutParams tableLp = new FrameLayout.LayoutParams(-1, dp(480));
        tableLp.setMargins(0, dp(148), 0, 0);
        root.addView(table, tableLp);

        TableRow row1 = new TableRow(this);
        row1.setMinimumHeight(dp(160));
        table.addView(row1, new TableLayout.LayoutParams(-1, dp(160)));
        runtimeCell = cell(row1, "Время пути", "00:00:00", R.mipmap.item_img_time);
        dtcCell = cell(row1, "Ошибки", "0Codes", R.mipmap.item_img_fault_code);
        voltageCell = cell(row1, "Напряжение", "0.0V", R.mipmap.item_img_voltage);
        intakeCell = cell(row1, "Входная темп-ра", "0", R.mipmap.ic_intake_air_temperature);

        TableRow row2 = new TableRow(this);
        row2.setMinimumHeight(dp(160));
        table.addView(row2, new TableLayout.LayoutParams(-1, dp(160)));
        loadCell = cell(row2, "Нагрузка двигателя", "0%", R.mipmap.ic_engine_load);
        throttleCell = cell(row2, "Положение заслонки", "0%", R.mipmap.throttle_position);
        tempCell = cell(row2, "Темп-ра антифриза", "0", R.mipmap.item_img_temper);
        InfoCell empty = cell(row2, "", "", R.mipmap.item_img_driving_time);
        empty.setVisibility(View.INVISIBLE);

        TableRow row3 = new TableRow(this);
        row3.setMinimumHeight(dp(160));
        table.addView(row3, new TableLayout.LayoutParams(-1, dp(160)));
        for (int i = 0; i < 4; i++) {
            InfoCell invisible = cell(row3, "", "", R.mipmap.item_img_temper);
            invisible.setVisibility(View.INVISIBLE);
        }

        setContentView(root);
        updateValues();
    }

    private InfoCell cell(TableRow row, String title, String value, int iconRes) {
        InfoCell cell = new InfoCell(this, iconRes);
        cell.set(title, value);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(0, -1, 1f);
        row.addView(cell, lp);
        return cell;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(0xffffffff);
        b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackgroundColor(0xaa0b4f82);
        return b;
    }

    private void updateValues() {
        VehicleDisplayState.Snapshot s = VehicleDisplayState.snapshot();
        int count = s.dtcCodes == null ? 0 : s.dtcCodes.size();
        runtimeCell.setValue(s.runtimeText());
        dtcCell.setValue(count + "Codes");
        voltageCell.setValue(s.voltageText());
        intakeCell.setValue(s.tempText(this, s.intakeTemp));
        loadCell.setValue(s.engineLoad + "%");
        throttleCell.setValue(s.throttle + "%");
        tempCell.setValue(s.tempText(this, s.coolantTemp));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class InfoCell extends LinearLayout {
        private final TextView value;

        InfoCell(VehicleInfoActivity activity, int iconRes) {
            super(activity);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER);
            setPadding(0, 0, 0, 0);

            ImageView icon = new ImageView(activity);
            icon.setImageResource(iconRes);
            icon.setAdjustViewBounds(true);
            addView(icon, new LinearLayout.LayoutParams(activity.dp(100), activity.dp(100)));

            LinearLayout texts = new LinearLayout(activity);
            texts.setOrientation(VERTICAL);
            texts.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(activity.dp(170), -2);
            textsLp.setMargins(activity.dp(10), 0, 0, 0);
            addView(texts, textsLp);

            TextView title = new TextView(activity);
            title.setTextColor(0xffffffff);
            title.setTextSize(16);
            title.setSingleLine(false);
            title.setMaxLines(2);
            texts.addView(title);

            value = new TextView(activity);
            value.setTextColor(0xff9aa4aa);
            value.setTextSize(14);
            value.setSingleLine(true);
            LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(-1, -2);
            valueLp.setMargins(0, activity.dp(10), 0, 0);
            value.setLayoutParams(valueLp);
            texts.addView(value);
            setTag(title);
        }

        void set(String titleText, String valueText) {
            ((TextView) getTag()).setText(titleText);
            setValue(valueText);
        }

        void setValue(String text) {
            value.setText(text);
        }
    }

    private final class VehicleChromeView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF dst = new RectF();
        private final Map<Integer, Bitmap> cache = new HashMap<>();

        VehicleChromeView(Activity activity) {
            super(activity);
            paint.setFilterBitmap(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(0xff03070c);
            Bitmap bottom = bitmap(R.mipmap.home_bottom_bg);
            if (bottom != null) {
                float scale = getWidth() / (float) bottom.getWidth();
                float bh = bottom.getHeight() * scale;
                dst.set(0, getHeight() - bh, getWidth(), getHeight());
                canvas.drawBitmap(bottom, null, dst, paint);
            }
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(dp(20));
            paint.setColor(0xffffffff);
            canvas.drawText(time.format(new Date()), dp(43), dp(58), paint);
            postInvalidateDelayed(1000);
        }

        private Bitmap bitmap(int resId) {
            Bitmap cached = cache.get(resId);
            if (cached != null) return cached;
            Bitmap decoded = BitmapFactory.decodeResource(getResources(), resId);
            if (decoded != null) cache.put(resId, decoded);
            return decoded;
        }
    }
}
