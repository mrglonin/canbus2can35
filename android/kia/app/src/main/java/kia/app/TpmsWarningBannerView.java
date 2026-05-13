package kia.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

final class TpmsWarningBannerView extends FrameLayout {
    private final PressureIconView logo;
    private final TextView warningText;
    private final ImageButton closeButton;
    private String message = "";

    TpmsWarningBannerView(Context context) {
        super(context);
        setBackground(warningBackground());
        setElevation(dp(40));

        logo = new PressureIconView(context);
        addView(logo, new LayoutParams(dp(54), dp(54), Gravity.LEFT | Gravity.CENTER_VERTICAL));

        warningText = new TextView(context);
        warningText.setTextColor(0xffffffff);
        warningText.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(27));
        warningText.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        warningText.setGravity(Gravity.CENTER);
        warningText.setSingleLine(true);
        warningText.setEllipsize(TextUtils.TruncateAt.END);
        warningText.setIncludeFontPadding(false);
        warningText.setShadowLayer(dp(2), dp(1), dp(1), 0xaa000000);
        addView(warningText, new LayoutParams(-1, -1, Gravity.CENTER));

        closeButton = new ImageButton(context);
        closeButton.setBackgroundResource(R.drawable.tpms_warning_close_style);
        closeButton.setPadding(0, 0, 0, 0);
        closeButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(closeButton, new LayoutParams(dp(43), dp(43), Gravity.RIGHT | Gravity.CENTER_VERTICAL));
    }

    void setMessage(String message) {
        this.message = message == null ? "" : message;
        warningText.setText(this.message);
        applyAdaptiveLayout(getWidth(), getHeight());
    }

    void setOnCloseClickListener(View.OnClickListener listener) {
        closeButton.setOnClickListener(listener);
    }

    int preferredHeight() {
        return dp(75);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyAdaptiveLayout(w, h);
    }

    private void applyAdaptiveLayout(int width, int height) {
        if (width <= 0 || height <= 0) return;
        int icon = Math.round(clamp(height * 0.90f, dp(54), dp(88)));
        int close = Math.round(clamp(height * 0.50f, dp(30), dp(43)));
        int sidePad = Math.round(clamp(width * 0.035f, dp(14), dp(34)));
        int gap = Math.round(clamp(width * 0.024f, dp(10), dp(26)));

        LayoutParams logoLp = new LayoutParams(icon, icon, Gravity.LEFT | Gravity.CENTER_VERTICAL);
        logoLp.leftMargin = sidePad;
        logo.setLayoutParams(logoLp);

        LayoutParams closeLp = new LayoutParams(close, close, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        closeLp.rightMargin = Math.max(dp(14), Math.round(sidePad * 0.75f));
        closeButton.setLayoutParams(closeLp);

        int leftGuard = sidePad + icon + gap;
        int rightGuard = closeLp.rightMargin + close + gap;
        int guard = Math.max(leftGuard, rightGuard);
        LayoutParams textLp = new LayoutParams(-1, -1, Gravity.CENTER);
        textLp.leftMargin = guard;
        textLp.rightMargin = guard;
        warningText.setLayoutParams(textLp);

        fitText(width - textLp.leftMargin - textLp.rightMargin, height);
    }

    private void fitText(int availableWidth, int height) {
        float maxSize = clamp(height * 0.36f, dp(20), dp(30));
        float minSize = dp(17);
        warningText.setTextSize(TypedValue.COMPLEX_UNIT_PX, maxSize);
        float size = maxSize;
        while (size > minSize && warningText.getPaint().measureText(message) > availableWidth) {
            size -= 1f;
            warningText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private GradientDrawable warningBackground() {
        return new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xffff6674, 0xffff245d}
        );
    }

    private static final class PressureIconView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arc = new RectF();

        PressureIconView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float s = Math.min(w, h);
            if (s <= 0f) return;

            float cx = w / 2f;
            float cy = h / 2f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffffffff);
            paint.setShadowLayer(s * 0.055f, 0f, s * 0.03f, 0x66000000);
            canvas.drawCircle(cx, cy, s * 0.43f, paint);

            paint.clearShadowLayer();
            paint.setColor(0xffff245d);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(s * 0.075f);

            arc.set(cx - s * 0.24f, cy - s * 0.14f, cx + s * 0.24f, cy + s * 0.35f);
            canvas.drawArc(arc, 155f, 230f, false, paint);
            canvas.drawLine(cx - s * 0.27f, cy - s * 0.01f, cx - s * 0.34f, cy + s * 0.26f, paint);
            canvas.drawLine(cx + s * 0.27f, cy - s * 0.01f, cx + s * 0.34f, cy + s * 0.26f, paint);

            paint.setStrokeWidth(s * 0.07f);
            canvas.drawLine(cx, cy - s * 0.20f, cx, cy + s * 0.08f, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy + s * 0.24f, s * 0.05f, paint);
        }
    }
}
