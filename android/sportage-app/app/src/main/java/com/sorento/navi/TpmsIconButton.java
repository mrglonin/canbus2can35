package com.sorento.navi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class TpmsIconButton extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    TpmsIconButton(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float s = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isPressed() ? 0xdd0fc0df : 0xcc061824);
        canvas.drawCircle(cx, cy, s * 0.42f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.032f);
        paint.setColor(0xff18f0ff);
        canvas.drawCircle(cx, cy, s * 0.42f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.075f);
        paint.setColor(0xffffffff);
        rect.set(cx - s * 0.18f, cy - s * 0.24f, cx + s * 0.18f, cy + s * 0.24f);
        canvas.drawRoundRect(rect, s * 0.11f, s * 0.11f, paint);
        paint.setStrokeWidth(s * 0.045f);
        for (int i = -2; i <= 2; i++) {
            float x = cx + i * s * 0.075f;
            canvas.drawLine(x, cy - s * 0.23f, x, cy + s * 0.23f, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff18f0ff);
        canvas.drawCircle(cx + s * 0.26f, cy - s * 0.16f, s * 0.055f, paint);
        canvas.drawCircle(cx + s * 0.29f, cy, s * 0.07f, paint);
        canvas.drawCircle(cx + s * 0.26f, cy + s * 0.17f, s * 0.055f, paint);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
