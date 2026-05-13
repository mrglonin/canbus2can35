package kia.app;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

final class TpmsOverlayView extends FrameLayout {
    private final TpmsWarningBannerView banner;

    TpmsOverlayView(Context context) {
        super(context);
        setClipChildren(false);
        setClipToPadding(false);
        banner = new TpmsWarningBannerView(context);
        banner.setVisibility(View.GONE);
        banner.setOnCloseClickListener(v -> {
            TpmsAlertManager.suppressAfterUserClose(getContext());
            banner.setVisibility(View.GONE);
        });
        LayoutParams lp = new LayoutParams(-1, banner.preferredHeight(), Gravity.TOP);
        addView(banner, lp);
    }

    static boolean hasVisibleAlert(Context context) {
        if (context == null || !AppPrefs.tpmsEnabled(context) || !AppPrefs.tpmsAlertOverlay(context)) return false;
        if (TpmsAlertManager.isSuppressed(context)) return false;
        return TpmsAlertManager.alertMessage(TpmsState.snapshot()).length() > 0;
    }

    int windowHeight() {
        return banner.preferredHeight();
    }

    void refreshFromState() {
        String message = TpmsAlertManager.alertMessage(TpmsState.snapshot());
        boolean visible = message.length() > 0 && !TpmsAlertManager.isSuppressed(getContext());
        if (visible) {
            banner.setMessage(message);
            banner.setVisibility(View.VISIBLE);
            banner.setAlpha(1f);
        } else {
            banner.setVisibility(View.GONE);
            banner.setAlpha(1f);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshFromState();
    }

    @Override
    protected void onDetachedFromWindow() {
        banner.setAlpha(1f);
        super.onDetachedFromWindow();
    }
}
