package com.sorento.navi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ObdDashboardView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dst = new RectF();
    private final Map<Integer, Bitmap> cache = new HashMap<>();
    private final SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.US);
    private VehicleDisplayState.Snapshot snapshot = VehicleDisplayState.snapshot();

    public ObdDashboardView(Context context) {
        super(context);
        paint.setFilterBitmap(true);
    }

    void setSnapshot(ObdState.Snapshot value) {
        snapshot = value == null ? VehicleDisplayState.snapshot() : VehicleDisplayState.fromObd(value);
        invalidate();
    }

    void setSnapshot(VehicleDisplayState.Snapshot value) {
        snapshot = value == null ? VehicleDisplayState.snapshot() : value;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        boolean animated = AppPrefs.backgroundAnimation(getContext());
        if (animated) {
            drawAnimatedBackground(canvas, w, h);
        } else {
            drawCover(canvas, R.mipmap.background_1, w, h);
        }
        drawHomeChrome(canvas, w, h);
        drawTeyesGauges(canvas, w, h);
        drawSpeed(canvas, w, h);
        drawBottomData(canvas, w, h);
        if (animated) postInvalidateDelayed(120);
    }

    private void drawAnimatedBackground(Canvas canvas, int w, int h) {
        int frame = (int) ((System.currentTimeMillis() / 100L) % 13L) + 1;
        drawCover(canvas, name("background_" + frame), w, h);
    }

    private void drawHomeChrome(Canvas canvas, int w, int h) {
        Bitmap bottom = bitmap(R.mipmap.home_bottom_bg);
        if (bottom != null) {
            float scale = w / (float) bottom.getWidth();
            float bh = bottom.getHeight() * scale;
            drawFit(canvas, R.mipmap.home_bottom_bg, 0, h - bh, w, bh);
        }
        Bitmap logo = bitmap(R.mipmap.home_bottom_logo);
        if (logo != null && (bottom == null || bottom.getWidth() < dp(1200))) {
            float x = (w - logo.getWidth()) / 2f;
            float y = h - dp(70) - logo.getHeight();
            canvas.drawBitmap(logo, x, y, paint);
        }
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(sp(20));
        paint.setColor(0xffffffff);
        canvas.drawText(time.format(new Date()), dp(43), dp(58), paint);
    }

    private void drawTeyesGauges(Canvas canvas, int w, int h) {
        float centerY = h / 2f;
        Bitmap rpm = bitmap(name("rpm_" + rounded(snapshot.rpm, 200, 0, 8000)));
        if (rpm == null) return;
        float rpmX = rpm.getWidth() > dp(1000) ? (w - rpm.getWidth()) / 2f : dp(50);
        canvas.drawBitmap(rpm, rpmX, centerY - rpm.getHeight() / 2f, paint);

        float inset = rpm.getWidth() > dp(1000) ? dp(140) : dp(130);
        drawAt(canvas, name("engine_load_" + rounded(snapshot.engineLoad, 5, 0, 100)), rpmX + inset, centerY, true);
        Bitmap temp = bitmap(name("temper_" + tempAsset(snapshot.coolantTemp)));
        if (temp != null) {
            canvas.drawBitmap(temp, rpmX + rpm.getWidth() - inset - temp.getWidth(), centerY - temp.getHeight() / 2f, paint);
        }
    }

    private void drawSpeed(Canvas canvas, int w, int h) {
        int displaySpeed = snapshot.displaySpeed(getContext());
        Bitmap speed = bitmap(name("speed_" + rounded(displaySpeed, 3, 0, 180)));
        float target = speed != null && speed.getWidth() >= dp(700) ? dp(670) : dp(613);
        float size = Math.min(target, Math.min(w, h) * 0.96f);
        float x = (w - size) / 2f;
        float y = (h - size) / 2f;
        if (speed != null) {
            dst.set(x, y, x + size, y + size);
            canvas.drawBitmap(speed, null, dst, paint);
        }

        String value = String.valueOf(displaySpeed);
        float digitScale = Math.max(0.64f, (size / 613f) * 0.70f);
        float total = 0;
        for (int i = 0; i < value.length(); i++) {
            Bitmap d = bitmap(name("number_" + value.charAt(i)));
            if (d != null) total += d.getWidth() * digitScale;
        }
        float dx = w / 2f - total / 2f;
        float maxDigitH = 0f;
        for (int i = 0; i < value.length(); i++) {
            Bitmap d = bitmap(name("number_" + value.charAt(i)));
            if (d != null) maxDigitH = Math.max(maxDigitH, d.getHeight() * digitScale);
        }
        float dy = h / 2f - dp(10) - maxDigitH / 2f;
        for (int i = 0; i < value.length(); i++) {
            Bitmap d = bitmap(name("number_" + value.charAt(i)));
            if (d == null) continue;
            float dw = d.getWidth() * digitScale;
            float dh = d.getHeight() * digitScale;
            dst.set(dx, dy + (maxDigitH - dh) / 2f, dx + dw, dy + (maxDigitH + dh) / 2f);
            canvas.drawBitmap(d, null, dst, paint);
            dx += dw;
        }
        drawSpeedUnit(canvas, w, dy + maxDigitH + dp(18));
    }

    private void drawBottomData(Canvas canvas, int w, int h) {
        float y = h - dp(55);
        float dataWidth = Math.min(dp(1050), w - dp(190));
        float itemW = dataWidth / 4f;
        float x = dp(42);
        bottomItem(canvas, R.mipmap.home_voltage, x, y, snapshot.voltageText());
        bottomItem(canvas, R.mipmap.home_mileage, x + itemW, y, snapshot.speedText(getContext()));
        bottomItem(canvas, R.mipmap.rpm_icon, x + itemW * 2f, y, snapshot.rpm + "rpm");
        bottomItem(canvas, R.mipmap.engine_runtime, x + itemW * 3f, y, snapshot.runtimeText());
    }

    private void bottomItem(Canvas canvas, int resId, float x, float y, String text) {
        Bitmap icon = bitmap(resId);
        if (icon != null) {
            float iy = y - icon.getHeight() / 2f;
            canvas.drawBitmap(icon, x, iy, paint);
            x += icon.getWidth() + dp(10);
        }
        paint.setShader(null);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(sp(18));
        paint.setColor(0xffffffff);
        canvas.drawText(text, x, y + dp(7), paint);
    }

    private void drawSpeedUnit(Canvas canvas, int w, float y) {
        String text = AppPrefs.speedUnit(getContext()) == 0 ? "km/h" : AppPrefs.speedUnitLabel(getContext());
        paint.setShader(null);
        paint.clearShadowLayer();
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(sp(30));
        paint.setColor(0xffffffff);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, w / 2f, y + dp(28), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private int rounded(int value, int step, int min, int max) {
        int clamped = Math.max(min, Math.min(max, value));
        int result = Math.round(clamped / (float) step) * step;
        return Math.max(min, Math.min(max, result));
    }

    private String tempAsset(int value) {
        int rounded = rounded(value, 5, -10, 100);
        return rounded < 0 ? "_" + Math.abs(rounded) : String.valueOf(rounded);
    }

    private void drawAt(Canvas canvas, int resId, float x, float centerY, boolean verticalCenter) {
        Bitmap bitmap = bitmap(resId);
        if (bitmap == null) return;
        float y = verticalCenter ? centerY - bitmap.getHeight() / 2f : centerY;
        canvas.drawBitmap(bitmap, x, y, paint);
    }

    private void drawFit(Canvas canvas, int resId, float x, float y, float w, float h) {
        Bitmap bitmap = bitmap(resId);
        if (bitmap == null) return;
        dst.set(x, y, x + w, y + h);
        canvas.drawBitmap(bitmap, null, dst, paint);
    }

    private void drawCover(Canvas canvas, int resId, float w, float h) {
        Bitmap bitmap = bitmap(resId);
        if (bitmap == null) return;
        float scale = Math.max(w / bitmap.getWidth(), h / bitmap.getHeight());
        float bw = bitmap.getWidth() * scale;
        float bh = bitmap.getHeight() * scale;
        float x = (w - bw) / 2f;
        float y = (h - bh) / 2f;
        dst.set(x, y, x + bw, y + bh);
        canvas.drawBitmap(bitmap, null, dst, paint);
    }

    private int name(String resourceName) {
        return getResources().getIdentifier(resourceName, "mipmap", getContext().getPackageName());
    }

    private Bitmap bitmap(int resId) {
        if (resId == 0) return null;
        Bitmap cached = cache.get(resId);
        if (cached != null) return cached;
        Bitmap decoded = BitmapFactory.decodeResource(getResources(), resId);
        if (decoded != null) cache.put(resId, decoded);
        return decoded;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
