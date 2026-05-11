package com.sorento.navi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

final class MusicOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    MusicOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float pad = Math.max(18f, w / 80f);
        float bandH = Math.max(92f, h / 8.5f);
        float top = h - bandH - Math.max(18f, h / 42f);

        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.clearShadowLayer();
        paint.setColor(0xcc071018);
        rect.set(pad, top, w - pad, top + bandH);
        canvas.drawRoundRect(rect, 18f, 18f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(0xff02f5ce);
        canvas.drawRoundRect(rect, 18f, 18f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(Math.max(20f, w / 58f));
        paint.setColor(0xff02f5ce);
        paint.setShadowLayer(4f, 2f, 2f, 0xff000000);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("МУЗЫКА", pad + 24f, top + 34f, paint);

        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(Math.max(24f, w / 48f));
        paint.setColor(0xffffffff);
        drawSingleLine(canvas, AppLog.media(), pad + 24f, top + bandH - 28f, w - pad * 2f - 48f);

        postInvalidateDelayed(500);
    }

    private void drawSingleLine(Canvas canvas, String value, float x, float y, float maxWidth) {
        if (value == null || value.length() == 0) value = "Мультимедиа: - / - / - / --:--";
        String text = value;
        while (paint.measureText(text) > maxWidth && text.length() > 4) {
            text = text.substring(0, text.length() - 2);
        }
        if (!text.equals(value) && text.length() > 3) {
            text = text.substring(0, text.length() - 3) + "...";
        }
        canvas.drawText(text, x, y, paint);
    }
}
