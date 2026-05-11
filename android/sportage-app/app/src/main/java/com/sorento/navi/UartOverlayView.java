package com.sorento.navi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

final class UartOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    UartOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        SidebandDebugState.Snapshot s = SidebandDebugState.snapshot();
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.clearShadowLayer();
        paint.setColor(0x4d14b87a);
        rect.set(0, 0, w / 2f, h);
        canvas.drawRect(rect, paint);
        paint.setColor(0x4dd83a3a);
        rect.set(w / 2f, 0, w, h);
        canvas.drawRect(rect, paint);

        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(Math.max(22f, w / 44f));
        paint.setAlpha(255);
        paint.setColor(0xffffffff);
        paint.setShadowLayer(4f, 2f, 2f, 0xff000000);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("UART TX", 28f, 48f, paint);
        canvas.drawText("UART RX", w / 2f + 28f, 48f, paint);
        drawLines(canvas, s.lastUartTx, 28f, 92f, w / 2f - 46f);
        drawLines(canvas, s.lastUartRx, w / 2f + 28f, 92f, w / 2f - 46f);
        postInvalidateDelayed(500);
    }

    private void drawLines(Canvas canvas, String text, float x, float y, float maxWidth) {
        if (text == null || text.length() == 0) text = "нет данных";
        int chars = Math.max(12, (int) (maxWidth / Math.max(10f, paint.getTextSize() * 0.56f)));
        String[] parts = text.split(" ");
        String line = "";
        for (String part : parts) {
            String next = line.length() == 0 ? part : line + " " + part;
            if (next.length() > chars) {
                canvas.drawText(line, x, y, paint);
                y += paint.getTextSize() + 8f;
                line = part;
            } else {
                line = next;
            }
        }
        if (line.length() > 0) canvas.drawText(line, x, y, paint);
    }
}
