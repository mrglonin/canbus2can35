package com.sorento.navi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class BlindSpotOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    BlindSpotOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        BlindSpotState.Snapshot state = BlindSpotState.snapshot();
        if (!state.active()) return;

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        long now = System.currentTimeMillis();
        drawWarning(canvas, width, height);
        if (state.left) drawArrows(canvas, true, width, height, now);
        if (state.right) drawArrows(canvas, false, width, height, now);
        postInvalidateDelayed(70L);
    }

    private void drawWarning(Canvas canvas, int width, int height) {
        float cx = width * 0.5f;
        float cy = height * 0.48f;
        float size = Math.min(width, height) * 0.105f;

        path.reset();
        path.moveTo(cx, cy - size);
        path.lineTo(cx - size * 0.95f, cy + size * 0.72f);
        path.lineTo(cx + size * 0.95f, cy + size * 0.72f);
        path.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xeeffd21f);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(4f, size * 0.08f));
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(0xffffffff);
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setColor(Color.WHITE);
        paint.setTextSize(size * 1.24f);
        canvas.drawText("!", cx, cy + size * 0.43f, paint);
    }

    private void drawArrows(Canvas canvas, boolean leftSide, int width, int height, long now) {
        float areaLeft = leftSide ? 0f : width * 0.55f;
        float areaRight = leftSide ? width * 0.45f : width;
        float areaWidth = areaRight - areaLeft;
        float y = height * 0.51f;
        float size = Math.min(width, height) * 0.085f;
        float spacing = size * 1.55f;
        float phase = ((now % 720L) / 720f) * spacing;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(Math.max(8f, size * 0.15f));
        paint.setColor(0xffffd21f);

        for (int i = -1; i < 5; i++) {
            float x;
            if (leftSide) {
                x = areaRight - i * spacing - phase;
                drawChevron(canvas, x, y, size, true);
            } else {
                x = areaLeft + i * spacing + phase;
                drawChevron(canvas, x, y, size, false);
            }
        }
    }

    private void drawChevron(Canvas canvas, float x, float y, float size, boolean pointsLeft) {
        path.reset();
        if (pointsLeft) {
            path.moveTo(x + size * 0.5f, y - size * 0.55f);
            path.lineTo(x - size * 0.5f, y);
            path.lineTo(x + size * 0.5f, y + size * 0.55f);
        } else {
            path.moveTo(x - size * 0.5f, y - size * 0.55f);
            path.lineTo(x + size * 0.5f, y);
            path.lineTo(x - size * 0.5f, y + size * 0.55f);
        }
        canvas.drawPath(path, paint);
    }
}
