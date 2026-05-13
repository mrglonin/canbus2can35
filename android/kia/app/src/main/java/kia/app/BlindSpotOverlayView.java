package kia.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

final class BlindSpotOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private final Runnable animationTick = new Runnable() {
        @Override
        public void run() {
            refreshFromState();
        }
    };

    BlindSpotOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        refreshFromState();
    }

    void refreshFromState() {
        removeCallbacks(animationTick);
        if (BlindSpotState.snapshot().active() && getWindowToken() != null) {
            postInvalidateOnAnimation();
            postDelayed(animationTick, 16L);
        } else {
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshFromState();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(animationTick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        BlindSpotState.Snapshot state = BlindSpotState.snapshot();
        if (!state.active()) return;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        long now = SystemClock.uptimeMillis();
        float phase = (now % 900L) / 900f;
        float pulse = 0.68f + 0.32f * (1f - Math.abs(phase - 0.5f) * 2f);

        boolean left = state.left || state.unknown;
        boolean right = state.right || state.unknown;
        drawDim(canvas, w, h, left, right);
        if (left) drawSide(canvas, w, h, true, phase, pulse);
        if (right) drawSide(canvas, w, h, false, phase, pulse);
        drawCenterWarning(canvas, w, h, state);

        postInvalidateOnAnimation();
    }

    private void drawDim(Canvas canvas, int w, int h, boolean left, boolean right) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        if (left) {
            paint.setColor(0x33000000);
            rect.set(0f, 0f, w * 0.42f, h);
            canvas.drawRect(rect, paint);
        }
        if (right) {
            paint.setColor(0x33000000);
            rect.set(w * 0.58f, 0f, w, h);
            canvas.drawRect(rect, paint);
        }
    }

    private void drawSide(Canvas canvas, int w, int h, boolean left, float phase, float pulse) {
        float min = Math.min(w, h);
        float size = clamp(min * 0.105f, dp(44), dp(118));
        float gap = size * 0.72f;
        float baseY = h * 0.52f;
        float baseX = left ? w * 0.16f : w * 0.84f;
        float drift = size * 0.42f * phase;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(clamp(size * 0.15f, dp(7), dp(18)));
        paint.setShader(null);

        for (int i = 0; i < 4; i++) {
            float order = left ? i : -i;
            float x = baseX + order * gap + (left ? -drift : drift);
            int alpha = Math.round(120 + 135 * pulse * (1f - i * 0.12f));
            paint.setColor((alpha << 24) | 0xffd34a);
            drawChevron(canvas, x, baseY, size, left);
        }
    }

    private void drawCenterWarning(Canvas canvas, int w, int h, BlindSpotState.Snapshot state) {
        float min = Math.min(w, h);
        float cx = w * 0.5f;
        float cy = h * 0.52f;
        float size = clamp(min * 0.18f, dp(92), dp(190));

        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xdd1d1010);
        rect.set(cx - size * 0.92f, cy - size * 0.62f, cx + size * 0.92f, cy + size * 0.62f);
        canvas.drawRoundRect(rect, dp(18), dp(18), paint);

        path.reset();
        path.moveTo(cx, cy - size * 0.72f);
        path.lineTo(cx - size * 0.78f, cy + size * 0.58f);
        path.lineTo(cx + size * 0.78f, cy + size * 0.58f);
        path.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xffffc43b);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(4));
        paint.setColor(0xff241000);
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff241000);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size * 0.82f);
        canvas.drawText("!", cx, cy + size * 0.36f, paint);

        paint.setColor(0xffffffff);
        paint.setTextSize(clamp(min * 0.045f, dp(22), dp(44)));
        String label = state.unknown ? "СЗАДИ"
                : state.left && state.right ? "СЛЕВА И СПРАВА"
                : state.left ? "СЛЕВА" : "СПРАВА";
        canvas.drawText(label, cx, cy + size * 0.95f, paint);
    }

    private void drawChevron(Canvas canvas, float cx, float cy, float size, boolean left) {
        path.reset();
        if (left) {
            path.moveTo(cx + size * 0.32f, cy - size * 0.45f);
            path.lineTo(cx - size * 0.28f, cy);
            path.lineTo(cx + size * 0.32f, cy + size * 0.45f);
        } else {
            path.moveTo(cx - size * 0.32f, cy - size * 0.45f);
            path.lineTo(cx + size * 0.28f, cy);
            path.lineTo(cx - size * 0.32f, cy + size * 0.45f);
        }
        canvas.drawPath(path, paint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
