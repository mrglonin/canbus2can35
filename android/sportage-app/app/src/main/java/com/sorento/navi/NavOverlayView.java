package com.sorento.navi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

final class NavOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    NavOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        float pad = Math.max(18f, w / 80f);
        float top = Math.max(18f, getHeight() / 36f);
        float panelH = Math.max(150f, getHeight() / 5.5f);

        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.clearShadowLayer();
        paint.setColor(0xcc070b12);
        rect.set(pad, top, w - pad, top + panelH);
        canvas.drawRoundRect(rect, 18f, 18f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(0xff7dfcff);
        canvas.drawRoundRect(rect, 18f, 18f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(Math.max(20f, w / 60f));
        paint.setColor(0xff7dfcff);
        paint.setShadowLayer(4f, 2f, 2f, 0xff000000);
        canvas.drawText("НАВИГАЦИЯ DEBUG", pad + 24f, top + 34f, paint);

        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(Math.max(17f, w / 76f));
        paint.setColor(0xffffffff);
        float y = top + 68f;
        y = drawLine(canvas, "state: " + AppLog.nav(), pad + 24f, y, w - pad * 2f - 48f);
        NavDebugState.Snapshot s = NavDebugState.snapshot();
        y = drawLine(canvas, "event: " + s.lastEvent, pad + 24f, y, w - pad * 2f - 48f);
        y = drawLine(canvas, "frame: " + s.lastFrame, pad + 24f, y, w - pad * 2f - 48f);
        drawLine(canvas, "teyes: " + s.lastTeyes, pad + 24f, y, w - pad * 2f - 48f);

        postInvalidateDelayed(500);
    }

    private float drawLine(Canvas canvas, String value, float x, float y, float maxWidth) {
        if (value == null) value = "";
        String text = value;
        while (paint.measureText(text) > maxWidth && text.length() > 4) {
            text = text.substring(0, text.length() - 2);
        }
        if (!text.equals(value) && text.length() > 3) {
            text = text.substring(0, text.length() - 3) + "...";
        }
        canvas.drawText(text, x, y, paint);
        return y + paint.getTextSize() + 9f;
    }
}
