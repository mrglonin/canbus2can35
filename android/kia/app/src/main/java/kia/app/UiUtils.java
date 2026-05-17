package kia.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

final class UiUtils {
    private UiUtils() {
    }

    static void enterImmersive(Activity activity) {
        if (activity == null) return;
        Window window = activity.getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decor = window.getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    static void startActivityWithTransition(Activity activity, Intent intent) {
        if (activity == null || intent == null) return;
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.app_screen_enter, R.anim.app_screen_exit);
    }

    static void finishWithTransition(Activity activity) {
        if (activity == null) return;
        activity.finish();
        activity.overridePendingTransition(R.anim.app_screen_pop_enter, R.anim.app_screen_pop_exit);
    }
}
