package kia.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

final class BlindSpotOverlayView extends FrameLayout {
    private View warningIcon;
    private View leftArrows;
    private View rightArrows;

    private final Runnable animationTick = new Runnable() {
        @Override
        public void run() {
            refreshFromState();
        }
    };

    BlindSpotOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        LayoutInflater.from(context).inflate(R.layout.view_blind_spot_overlay, this, true);
        warningIcon = findViewById(R.id.rcta_warning_icon);
        leftArrows = findViewById(R.id.rcta_left_arrows);
        rightArrows = findViewById(R.id.rcta_right_arrows);
        refreshFromState();
    }

    void refreshFromState() {
        BlindSpotState.Snapshot state = BlindSpotState.snapshot();
        boolean active = state.active();
        setPartVisible(warningIcon, active);
        setPartVisible(leftArrows, state.left);
        setPartVisible(rightArrows, state.right);
        animateArrows(state.left, state.right);

        removeCallbacks(animationTick);
        if (active && getWindowToken() != null) {
            postDelayed(animationTick, 70L);
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

    private void setPartVisible(View view, boolean visible) {
        if (view != null) view.setVisibility(visible ? VISIBLE : GONE);
    }

    private void animateArrows(boolean left, boolean right) {
        long now = System.currentTimeMillis();
        float phase = (now % 720L) / 720f;
        float shift = dp(54) * phase;
        float alpha = 0.58f + 0.42f * (1f - Math.abs(phase - 0.5f) * 2f);

        if (leftArrows != null) {
            leftArrows.setTranslationX(left ? -shift : 0f);
            leftArrows.setAlpha(left ? alpha : 1f);
        }
        if (rightArrows != null) {
            rightArrows.setTranslationX(right ? shift : 0f);
            rightArrows.setAlpha(right ? alpha : 1f);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
