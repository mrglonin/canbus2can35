package kia.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class SettingsIconButton extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    SettingsIconButton(Context context) {
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
        float cx = w * 0.50f;
        float cy = h * 0.50f;
        float outer = s * 0.42f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isPressed() ? 0xdd0fc0df : 0xcc061824);
        canvas.drawCircle(cx, cy, outer, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.032f);
        paint.setColor(0xff18f0ff);
        canvas.drawCircle(cx, cy, outer, paint);

        float r = s * 0.17f;
        paint.setStrokeWidth(s * 0.045f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xffffffff);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2d * i / 8d;
            float x1 = cx + (float) Math.cos(a) * r;
            float y1 = cy + (float) Math.sin(a) * r;
            float x2 = cx + (float) Math.cos(a) * (r + s * 0.075f);
            float y2 = cy + (float) Math.sin(a) * (r + s * 0.075f);
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.06f);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff18f0ff);
        canvas.drawCircle(cx, cy, s * 0.055f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.04f);
        paint.setColor(0xff18f0ff);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        rect.set(cx - s * 0.13f, cy + s * 0.28f, cx + s * 0.13f, cy + s * 0.38f);
        canvas.drawRoundRect(rect, s * 0.025f, s * 0.025f, paint);
        canvas.drawLine(cx - s * 0.05f, cy + s * 0.25f, cx - s * 0.05f, cy + s * 0.18f, paint);
        canvas.drawLine(cx + s * 0.05f, cy + s * 0.25f, cx + s * 0.05f, cy + s * 0.18f, paint);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
