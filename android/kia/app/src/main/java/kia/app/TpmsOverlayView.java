package kia.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

final class TpmsOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    TpmsOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
    }

    static boolean hasVisibleAlert(Context context) {
        if (context == null || !AppPrefs.tpmsEnabled(context) || !AppPrefs.tpmsAutoOpen(context)) return false;
        if (TpmsAlertManager.isSuppressed(context)) return false;
        return TpmsAlertManager.alertMessage(TpmsState.snapshot()).length() > 0;
    }

    void refreshFromState() {
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        String message = TpmsAlertManager.alertMessage(TpmsState.snapshot());
        if (message.length() == 0 || TpmsAlertManager.isSuppressed(getContext())) return;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float pad = Math.max(dp(18), w * 0.028f);
        float top = Math.max(dp(16), h * 0.035f);
        float height = clamp(h * 0.155f, dp(108), dp(168));
        float radius = dp(18);
        long now = SystemClock.uptimeMillis();
        float pulse = 0.72f + 0.28f * (1f - Math.abs((now % 900L) / 900f - 0.5f) * 2f);

        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xdd120b0b);
        rect.set(pad, top, w - pad, top + height);
        canvas.drawRoundRect(rect, radius, radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(0xffff4545);
        canvas.drawRoundRect(rect, radius, radius, paint);

        drawWarningIcon(canvas, pad + height * 0.52f, top + height * 0.5f, height * 0.34f, pulse);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(0xffffffff);
        paint.setTextSize(clamp(w * 0.032f, dp(24), dp(42)));
        paint.clearShadowLayer();
        canvas.drawText("TPMS", pad + height * 0.96f, top + height * 0.42f, paint);

        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(clamp(w * 0.026f, dp(20), dp(34)));
        paint.setColor(0xffffe1e1);
        drawSingleLine(canvas, message, pad + height * 0.96f, top + height * 0.73f, w - pad * 2f - height * 1.1f);

        postInvalidateDelayed(500L);
    }

    private void drawWarningIcon(Canvas canvas, float cx, float cy, float size, float pulse) {
        path.reset();
        path.moveTo(cx, cy - size);
        path.lineTo(cx - size * 1.05f, cy + size * 0.82f);
        path.lineTo(cx + size * 1.05f, cy + size * 0.82f);
        path.close();

        paint.setStyle(Paint.Style.FILL);
        int alpha = Math.round(210 + 45 * pulse);
        paint.setColor((alpha << 24) | 0xffd34a);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(0xff240c00);
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size * 1.05f);
        paint.setColor(0xff240c00);
        canvas.drawText("!", cx, cy + size * 0.47f, paint);
    }

    private void drawSingleLine(Canvas canvas, String value, float x, float y, float maxWidth) {
        String text = value == null ? "" : value;
        float originalSize = paint.getTextSize();
        while (paint.measureText(text) > maxWidth && paint.getTextSize() > dp(17)) {
            paint.setTextSize(paint.getTextSize() - 1f);
        }
        while (paint.measureText(text) > maxWidth && text.length() > 4) {
            text = text.substring(0, text.length() - 2);
        }
        if (!text.equals(value) && text.length() > 3) {
            text = text.substring(0, text.length() - 3) + "...";
        }
        canvas.drawText(text, x, y, paint);
        paint.setTextSize(originalSize);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
