package kia.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;
import java.util.Locale;

public class YandexNavAccessibilityService extends AccessibilityService {
    private static final String YANDEX_NAVI = "ru.yandex.yandexnavi";
    private static final long POLL_MS = 800L;
    private static final long SAME_ROUTE_REFRESH_MS = 5000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastKey = "";
    private long lastAt;

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            scan();
            handler.postDelayed(this, POLL_MS);
        }
    };

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
        }
        AppService.start(this);
        handler.removeCallbacks(poll);
        handler.post(poll);
        AppLog.line(this, "Навигация Яндекс: accessibility подключён");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!YANDEX_NAVI.contentEquals(event.getPackageName())) return;
        scan();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(poll);
        super.onDestroy();
    }

    private void scan() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        boolean sent = scanRoot(root);
        if (root != null) root.recycle();
        if (sent) return;

        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return;
        for (AccessibilityWindowInfo window : windows) {
            if (window == null) continue;
            AccessibilityNodeInfo windowRoot = window.getRoot();
            sent = scanRoot(windowRoot);
            if (windowRoot != null) windowRoot.recycle();
            if (sent) return;
        }
    }

    private boolean scanRoot(AccessibilityNodeInfo root) {
        try {
            if (root == null) return false;
            String distance = normalize(findText(root, "ru.yandex.yandexnavi:id/text_maneuverballoon_distance"));
            String unit = normalize(findText(root, "ru.yandex.yandexnavi:id/text_maneuverballoon_metrics"));
            String street = firstText(
                    findText(root, "ru.yandex.yandexnavi:id/text_nextstreet"),
                    findText(root, "ru.yandex.yandexnavi:id/statusPanel")
            );
            String etaDistance = normalize(findText(root, "ru.yandex.yandexnavi:id/textview_eta_distance"));
            String speedLimit = normalize(findText(root, "ru.yandex.yandexnavi:id/text_speedlimit"));

            boolean hasRoute = !TextUtils.isEmpty(distance)
                    || !TextUtils.isEmpty(street)
                    || !TextUtils.isEmpty(etaDistance);
            if (!hasRoute) return false;

            String key = join("|", distance, unit, street, etaDistance, speedLimit);
            long now = System.currentTimeMillis();
            if (TextUtils.equals(key, lastKey) && now - lastAt < SAME_ROUTE_REFRESH_MS) return true;
            lastKey = key;
            lastAt = now;

            Intent nav = new Intent(NavProtocol.ACTION_TEYES_NAV_INFO);
            nav.putExtra("app", YANDEX_NAVI);
            nav.putExtra("state", "open");
            nav.putExtra("direction", "forward");
            if (!TextUtils.isEmpty(distance)) nav.putExtra("distance_val_str", distance);
            if (!TextUtils.isEmpty(unit)) nav.putExtra("distance_unit", unit);
            if (!TextUtils.isEmpty(street)) {
                nav.putExtra("position", street);
                nav.putExtra("describe", street);
            }
            if (!TextUtils.isEmpty(etaDistance)) nav.putExtra("total_distance", etaDistance);
            if (!TextUtils.isEmpty(speedLimit)) nav.putExtra("speed_limit", speedLimit);
            NavProtocol.handleTeyesNavInfo(this, nav);
            NavDebugState.teyes(this, "Yandex UI: " + key);
            return true;
        } catch (RuntimeException e) {
            AppLog.line(this, "Навигация Яндекс: ошибка чтения UI: " + e.getClass().getSimpleName());
            return false;
        }
    }

    private static String findText(AccessibilityNodeInfo node, String id) {
        if (node == null) return null;
        String viewId = node.getViewIdResourceName();
        if (TextUtils.equals(viewId, id)) {
            CharSequence text = node.getText();
            if (!TextUtils.isEmpty(text)) return text.toString();
        }
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                String found = findText(child, id);
                if (!TextUtils.isEmpty(found)) return found;
            } finally {
                child.recycle();
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String out = value.replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\u00a0', ' ')
                .replace('\u202f', ' ')
                .trim();
        while (out.contains("  ")) out = out.replace("  ", " ");
        if (out.toLowerCase(Locale.US).endsWith("km")) out = out.substring(0, out.length() - 2).trim() + " км";
        return out.length() == 0 ? null : out;
    }

    private static String firstText(String... values) {
        if (values == null) return null;
        for (String value : values) {
            String clean = normalize(value);
            if (!TextUtils.isEmpty(clean)) return clean;
        }
        return null;
    }

    private static String join(String separator, String... values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                String clean = normalize(value);
                if (TextUtils.isEmpty(clean)) continue;
                if (out.length() > 0) out.append(separator);
                out.append(clean);
            }
        }
        return out.toString();
    }
}
