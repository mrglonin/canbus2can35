package kia.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

final class TpmsDashboardView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dst = new RectF();
    private final Map<Integer, Bitmap> cache = new HashMap<>();
    private TpmsState.Snapshot snapshot = TpmsState.snapshot();
    private boolean titleSuppressed;

    TpmsDashboardView(Context context) {
        super(context);
        paint.setFilterBitmap(true);
    }

    void setSnapshot(TpmsState.Snapshot value) {
        snapshot = value == null ? TpmsState.snapshot() : value;
        invalidate();
    }

    void setTitleSuppressed(boolean value) {
        if (titleSuppressed == value) return;
        titleSuppressed = value;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        float scale = Math.min(width / 1280f, height / 720f);
        float ox = (width - 1280f * scale) / 2f;
        float oy = (height - 720f * scale) / 2f;

        drawDarkBackground(canvas, ox, oy, scale);
        drawCar(canvas, ox, oy, scale);
        drawTires(canvas, ox, oy, scale);
        if (hasAlert()) postInvalidateDelayed(360);
    }

    private void drawDarkBackground(Canvas canvas, float ox, float oy, float scale) {
        canvas.drawColor(0xff0d0f13);
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(
                0, 0, 0, getHeight(),
                0xff14171d,
                0xff08090c,
                Shader.TileMode.CLAMP));
        dst.set(0, 0, getWidth(), getHeight());
        canvas.drawRect(dst, paint);
        paint.setShader(null);
    }

    private void drawTopBar(Canvas canvas, float ox, float oy, float scale) {
        drawFit(canvas, R.drawable.tpms_title_bar_background_land, ox, oy, 1280f * scale, 72f * scale);
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xdd101722);
        dst.set(ox, oy, ox + 1280f * scale, oy + 92f * scale);
        canvas.drawRect(dst, paint);
    }

    private void drawTitle(Canvas canvas, float ox, float oy, float scale) {
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(34f * scale);
        paint.setColor(0xffffffff);
        paint.setTextAlign(Paint.Align.CENTER);
        setTextShadow(scale);
        drawTextCenterY(canvas, "Давление в шинах", ox + 640f * scale, oy + 29f * scale, paint);

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(18f * scale);
        paint.setColor(isOnline() ? 0xff78f6d0 : 0xffffd166);
        paint.setTextAlign(Paint.Align.CENTER);
        drawTextCenterY(canvas, cleanStatus(), ox + 640f * scale, oy + 56f * scale, paint);
    }

    private void drawTires(Canvas canvas, float ox, float oy, float scale) {
        float panelW = 330f * scale;
        float panelH = 196f * scale;
        float side = 52f * scale;
        float left = ox + side;
        float right = ox + 1280f * scale - side - panelW;
        float top = oy + 120f * scale;
        float bottom = oy + 404f * scale;

        drawTire(canvas, tire(0), 0, "Л.П.(L.F.)", left, top, panelW, panelH, scale);
        drawTire(canvas, tire(1), 1, "П.П.(R.F.)", right, top, panelW, panelH, scale);
        drawTire(canvas, tire(2), 2, "Л.З.(L.R.)", left, bottom, panelW, panelH, scale);
        drawTire(canvas, tire(3), 3, "П.З.(R.R.)", right, bottom, panelW, panelH, scale);
    }

    private void drawTire(Canvas canvas, TpmsState.Tire tire, int index, String title, float x, float y, float w, float h, float scale) {
        boolean alert = tire != null && tire.alert();
        drawTireCardBackground(canvas, x, y, w, h, scale, alert);
        paint.clearShadowLayer();

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setColor(0xff9aa3af);
        paint.setTextSize(17f * scale);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.clearShadowLayer();
        drawTextCenterY(canvas, title, x + 18f * scale, y + 31f * scale, paint);

        if (tire != null && tire.lowBattery) {
            float titleW = paint.measureText(title);
            drawFit(canvas, R.drawable.tpms_low_power_icon, x + 26f * scale + titleW, y + 19f * scale, 40f * scale, 23f * scale);
        }

        drawStatusChip(canvas, tire, alert, x + w - 70f * scale, y + 31f * scale, scale);

        String pressure = pressureText(tire, index);
        String temp = tire == null ? "__" : tire.tempText();

        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(60f * scale);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(0xfff4f1ea);
        setTextShadow(scale);
        drawTextBottom(canvas, pressure, x + 18f * scale, y + h - 40f * scale, paint);

        paint.clearShadowLayer();
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(21f * scale);
        paint.setColor(0xfff4f1ea);
        drawTextBottom(canvas, "Bar", x + 24f * scale, y + h - 18f * scale, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(32f * scale);
        paint.setColor(alert ? 0xffffd166 : 0xfff4f1ea);
        drawTextBottom(canvas, temp + "°C", x + w - 20f * scale, y + h - 74f * scale, paint);

        paint.clearShadowLayer();
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(16f * scale);
        paint.setColor(0xff9aa3af);
        drawTextBottom(canvas, "Температура", x + w - 20f * scale, y + h - 36f * scale, paint);
    }

    private void drawTireCardBackground(Canvas canvas, float x, float y, float w, float h, float scale, boolean alert) {
        paint.clearShadowLayer();
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(alert ? 0xff2a1714 : 0xee191d23);
        dst.set(x, y, x + w, y + h);
        canvas.drawRoundRect(dst, 8f * scale, 8f * scale, paint);

        if (alert) {
            paint.setColor(0xffffba3a);
            dst.set(x, y, x + 6f * scale, y + h);
            canvas.drawRoundRect(dst, 8f * scale, 8f * scale, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f * scale);
        paint.setColor(alert ? 0xffffba3a : 0xff343a44);
        dst.set(x + 0.5f * scale, y + 0.5f * scale, x + w - 0.5f * scale, y + h - 0.5f * scale);
        canvas.drawRoundRect(dst, 8f * scale, 8f * scale, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawStatusChip(Canvas canvas, TpmsState.Tire tire, boolean alert, float cx, float cy, float scale) {
        String text = tire == null || !tire.hasData ? "ОЖИДАНИЕ" : tire.warningText();
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(12f * scale);
        paint.clearShadowLayer();
        float tw = paint.measureText(text);
        float chipW = Math.max(92f * scale, tw + 22f * scale);
        dst.set(cx - chipW / 2f, cy - 12f * scale, cx + chipW / 2f, cy + 12f * scale);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(alert ? 0xfff2b84b : (tire != null && tire.hasData ? 0xff26303a : 0xff22272f));
        canvas.drawRoundRect(dst, 8f * scale, 8f * scale, paint);
        paint.setColor(alert ? 0xff14110b : 0xfff4f1ea);
        paint.setTextAlign(Paint.Align.CENTER);
        drawTextCenterY(canvas, text, cx, cy, paint);
    }

    private void drawCar(Canvas canvas, float ox, float oy, float scale) {
        float carW = 420f * scale;
        float carH = 660f * scale;
        float carCx = ox + 640f * scale;
        float carCy = oy + 360f * scale;
        drawFitAspect(canvas, R.drawable.kia_top_view, carCx, carCy, carW, carH);
    }

    private void drawFooter(Canvas canvas, float ox, float oy, float scale) {
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.FILL);
        dst.set(ox + 420f * scale, oy + 662f * scale, ox + 860f * scale, oy + 704f * scale);
        paint.setColor(0xaa07111c);
        canvas.drawRoundRect(dst, 21f * scale, 21f * scale, paint);

        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(18f * scale);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(isOnline() ? 0xff78f6d0 : 0xffffd166);
        setTextShadow(scale * 0.7f);
        drawTextCenterY(canvas, cleanStatus(), ox + 640f * scale, oy + 683f * scale, paint);
    }

    private TpmsState.Tire tire(int index) {
        if (snapshot == null || snapshot.tires == null || index < 0 || index >= snapshot.tires.length) return null;
        return snapshot.tires[index];
    }

    private String pressureText(TpmsState.Tire tire, int index) {
        if (tire == null || !tire.hasData) return "__";
        return tire.pressureText();
    }

    private String cleanStatus() {
        if (snapshot == null || snapshot.status == null || snapshot.status.length() == 0) return "TPMS: ожидание данных";
        String status = snapshot.status.replace("TPMS:", "").trim();
        if (hasAnyData() && status.contains("не найден")) status = "данные TPMS получены";
        if (status.length() > 46) status = status.substring(0, 43) + "...";
        return status;
    }

    private boolean isOnline() {
        return snapshot != null && (snapshot.connected || hasAnyData());
    }

    private boolean hasAnyData() {
        if (snapshot == null || snapshot.tires == null) return false;
        for (TpmsState.Tire tire : snapshot.tires) {
            if (tire != null && tire.hasData) return true;
        }
        return false;
    }

    private boolean hasAlert() {
        if (snapshot == null || snapshot.tires == null) return false;
        for (TpmsState.Tire tire : snapshot.tires) {
            if (tire != null && tire.alert()) return true;
        }
        return false;
    }

    private void drawTextCenterY(Canvas canvas, String text, float x, float cy, Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(text == null ? "" : text, x, cy - (fm.ascent + fm.descent) / 2f, paint);
    }

    private void drawTextBottom(Canvas canvas, String text, float x, float bottom, Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(text == null ? "" : text, x, bottom - fm.descent, paint);
    }

    private void drawFit(Canvas canvas, int resId, float x, float y, float w, float h) {
        Bitmap bitmap = bitmap(resId);
        if (bitmap == null) return;
        paint.setShader(null);
        paint.setAlpha(255);
        paint.setColorFilter(null);
        paint.clearShadowLayer();
        dst.set(x, y, x + w, y + h);
        canvas.drawBitmap(bitmap, null, dst, paint);
    }

    private void drawFitAspect(Canvas canvas, int resId, float cx, float cy, float maxW, float maxH) {
        Bitmap bitmap = bitmap(resId);
        if (bitmap == null) return;
        float scale = Math.min(maxW / bitmap.getWidth(), maxH / bitmap.getHeight());
        float w = bitmap.getWidth() * scale;
        float h = bitmap.getHeight() * scale;
        drawFit(canvas, resId, cx - w / 2f, cy - h / 2f, w, h);
    }

    private void setTextShadow(float scale) {
        paint.setShadowLayer(3f * scale, 2f * scale, 2f * scale, 0xff000000);
    }

    private void drawFitAlpha(Canvas canvas, int resId, float x, float y, float w, float h, int alpha) {
        int old = paint.getAlpha();
        paint.setAlpha(alpha);
        drawFit(canvas, resId, x, y, w, h);
        paint.setAlpha(old);
    }

    private void drawCover(Canvas canvas, int resId, float x, float y, float w, float h) {
        Bitmap bitmap = bitmap(resId);
        if (bitmap == null) return;
        float scale = Math.max(w / bitmap.getWidth(), h / bitmap.getHeight());
        float bw = bitmap.getWidth() * scale;
        float bh = bitmap.getHeight() * scale;
        float dx = x + (w - bw) / 2f;
        float dy = y + (h - bh) / 2f;
        drawFit(canvas, resId, dx, dy, bw, bh);
    }

    private Bitmap bitmap(int resId) {
        Bitmap cached = cache.get(resId);
        if (cached != null) return cached;
        Bitmap decoded = BitmapFactory.decodeResource(getResources(), resId);
        if (decoded != null) cache.put(resId, decoded);
        return decoded;
    }
}
