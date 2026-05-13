package kia.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class FirmwareReleaseUpdater {
    static final String ACTION_STATE = "kia.app.FIRMWARE_RELEASE_STATE";

    private static final String LATEST_MANIFEST_URL = "https://api.github.com/repos/mrglonin/canbus2can35/contents/updates/latest.json?ref=main";
    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/mrglonin/canbus2can35/releases/latest";
    private static final int MAX_FIRMWARE_SIZE = 114688;
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static Snapshot snapshot = new Snapshot("Прошивка адаптера: ожидание", "", "",
            "", "", 0, 0, 0, false, false, false, false);
    private static boolean busy;

    private FirmwareReleaseUpdater() {
    }

    static synchronized Snapshot snapshot() {
        return snapshot;
    }

    static void checkNow(Context context) {
        if (context == null) return;
        check(context.getApplicationContext(), true);
    }

    static void downloadAndFlash(Activity activity) {
        if (activity == null) return;
        Snapshot current = snapshot();
        if (TextUtils.isEmpty(current.downloadUrl)) {
            Toast.makeText(activity, "Проверяю BIN и сразу прошиваю", Toast.LENGTH_SHORT).show();
            checkThenDownload(activity);
            return;
        }
        if (current.assetSize > MAX_FIRMWARE_SIZE) {
            Toast.makeText(activity, "BIN больше 112 КБ, адаптер его не примет", Toast.LENGTH_LONG).show();
            return;
        }
        download(activity, current);
    }

    private static void check(Context context, boolean interactive) {
        synchronized (FirmwareReleaseUpdater.class) {
            if (busy) {
                if (interactive) Toast.makeText(context, "Проверка уже идёт", Toast.LENGTH_SHORT).show();
                return;
            }
            busy = true;
        }
        set(context, new Snapshot("Прошивка адаптера: проверка GitHub", "", "",
                "", "", 0, 0, 0, true, false, false, false));
        EXEC.execute(() -> {
            try {
                ReleaseInfo info = loadLatestRelease();
                String status;
                if (TextUtils.isEmpty(info.downloadUrl)) {
                    status = "Прошивка адаптера: git manifest/release BIN не найден";
                } else if (info.assetSize > MAX_FIRMWARE_SIZE) {
                    status = "Прошивка адаптера: BIN больше 112 КБ";
                } else {
                    status = "Прошивка адаптера: найден " + info.assetName;
                }
                set(context, new Snapshot(status, info.tagName, info.releaseName, info.assetName,
                        info.downloadUrl, 0, info.assetSize, info.assetSize, false, false, false, false));
            } catch (Exception e) {
                set(context, new Snapshot("Прошивка адаптера: ошибка " + e.getClass().getSimpleName(),
                        "", "", "", "", 0, 0, 0, false, false, false, false));
                AppLog.line(context, "Прошивка адаптера OTA: ошибка " + e.getClass().getSimpleName() + " " + e.getMessage());
            } finally {
                synchronized (FirmwareReleaseUpdater.class) {
                    busy = false;
                }
            }
        });
    }

    private static void checkThenDownload(Activity activity) {
        synchronized (FirmwareReleaseUpdater.class) {
            if (busy) {
                Toast.makeText(activity, "Проверка уже идёт", Toast.LENGTH_SHORT).show();
                return;
            }
            busy = true;
        }
        Context app = activity.getApplicationContext();
        set(app, new Snapshot("Прошивка адаптера: проверка GitHub", "", "",
                "", "", 0, 0, 0, true, false, false, false));
        EXEC.execute(() -> {
            try {
                ReleaseInfo info = loadLatestRelease();
                Snapshot next = new Snapshot(
                        TextUtils.isEmpty(info.downloadUrl)
                                ? "Прошивка адаптера: git manifest/release BIN не найден"
                                : "Прошивка адаптера: найден " + info.assetName,
                        info.tagName, info.releaseName, info.assetName, info.downloadUrl,
                        0, info.assetSize, info.assetSize, false, false, false, false);
                set(app, next);
                synchronized (FirmwareReleaseUpdater.class) {
                    busy = false;
                }
                if (TextUtils.isEmpty(info.downloadUrl)) return;
                if (info.assetSize > MAX_FIRMWARE_SIZE) {
                    MAIN.post(() -> Toast.makeText(activity, "BIN больше 112 КБ, адаптер его не примет", Toast.LENGTH_LONG).show());
                    return;
                }
                MAIN.post(() -> download(activity, snapshot()));
            } catch (Exception e) {
                set(app, new Snapshot("Прошивка адаптера: ошибка " + e.getClass().getSimpleName(),
                        "", "", "", "", 0, 0, 0, false, false, false, false));
                AppLog.line(app, "Прошивка адаптера OTA: ошибка " + e.getClass().getSimpleName() + " " + e.getMessage());
                synchronized (FirmwareReleaseUpdater.class) {
                    busy = false;
                }
            }
        });
    }

    private static ReleaseInfo loadLatestRelease() throws Exception {
        try {
            ReleaseInfo manifest = loadManifestRelease();
            if (!TextUtils.isEmpty(manifest.downloadUrl)) return manifest;
        } catch (Exception ignored) {
        }
        return loadGithubRelease();
    }

    private static ReleaseInfo loadManifestRelease() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_MANIFEST_URL).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/vnd.github.raw");
        connection.setRequestProperty("User-Agent", "KiaCanbusFirmwareUpdater");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String json = readText(stream);
        ReleaseInfo info = new ReleaseInfo();
        if (code == 404) return info;
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("GitHub manifest HTTP " + code + " " + json);
        }
        JSONObject root = new JSONObject(json);
        JSONObject firmware = root.optJSONObject("firmware");
        if (firmware == null) return info;
        info.tagName = firmware.optString("version", root.optString("updated_at", ""));
        info.releaseName = "git manifest";
        info.assetName = firmware.optString("asset_name", "");
        info.downloadUrl = firmware.optString("download_url", "");
        info.assetSize = firmware.optLong("size", 0);
        return info;
    }

    private static ReleaseInfo loadGithubRelease() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "KiaCanbusFirmwareUpdater");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String json = readText(stream);
        ReleaseInfo best = new ReleaseInfo();
        if (code == 404) return best;
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("GitHub HTTP " + code + " " + json);
        }
        JSONObject root = new JSONObject(json);
        best.tagName = root.optString("tag_name", "");
        best.releaseName = root.optString("name", "");
        JSONArray assets = root.optJSONArray("assets");
        if (assets == null) return best;
        int bestScore = -1;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset == null) continue;
            String name = asset.optString("name", "");
            String lower = name.toLowerCase(Locale.US);
            if (!lower.endsWith(".bin")) continue;
            int score = firmwareAssetScore(lower);
            if (score < bestScore) continue;
            bestScore = score;
            best.assetName = name;
            best.downloadUrl = asset.optString("browser_download_url", "");
            best.assetSize = asset.optLong("size", 0);
        }
        return best;
    }

    private static int firmwareAssetScore(String lowerName) {
        int score = 0;
        if (lowerName.contains("v20")) score += 60;
        if (lowerName.contains("2can35")) score += 40;
        if (lowerName.contains("smt35")) score += 30;
        if (lowerName.contains("kia")) score += 20;
        if (lowerName.contains("mode1")) score += 10;
        if (lowerName.contains("base")) score -= 20;
        return score;
    }

    private static void download(Activity activity, Snapshot source) {
        synchronized (FirmwareReleaseUpdater.class) {
            if (busy) {
                Toast.makeText(activity, "Загрузка или прошивка уже идёт", Toast.LENGTH_SHORT).show();
                return;
            }
            busy = true;
        }
        Context app = activity.getApplicationContext();
        set(app, source.copy("Прошивка адаптера: загрузка " + source.assetName, false, true, false, false));
        EXEC.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(source.downloadUrl).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", "KiaCanbusFirmwareUpdater");
                long total = connection.getContentLengthLong();
                if (total <= 0) total = source.totalBytes;
                ByteArrayOutputStream out = new ByteArrayOutputStream(total > 0 && total < Integer.MAX_VALUE ? (int) total : 8192);
                try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                    byte[] buffer = new byte[8192];
                    long done = 0;
                    long lastReport = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        done += read;
                        if (done > MAX_FIRMWARE_SIZE) {
                            throw new IllegalStateException("firmware too large");
                        }
                        long now = System.currentTimeMillis();
                        if (now - lastReport > 500L) {
                            lastReport = now;
                            set(app, source.progress("Прошивка адаптера: загрузка " + percent(done, total), done, total, true, false));
                        }
                    }
                }
                byte[] data = out.toByteArray();
                set(app, source.progress("Прошивка адаптера: BIN скачан", data.length, total, false, false).downloaded(true));
                MAIN.post(() -> flash(activity, data));
            } catch (Exception e) {
                set(app, source.copy("Прошивка адаптера: ошибка загрузки " + e.getClass().getSimpleName(),
                        false, false, false, false));
                AppLog.line(app, "Прошивка адаптера OTA: ошибка загрузки " + e.getClass().getSimpleName() + " " + e.getMessage());
                synchronized (FirmwareReleaseUpdater.class) {
                    busy = false;
                }
            }
        });
    }

    private static void flash(Activity activity, byte[] data) {
        Context app = activity.getApplicationContext();
        Snapshot source = snapshot();
        set(app, source.progress("Прошивка адаптера: старт USB update", 0, 100, false, true));
        new CanbusFirmwareUpdater(app, data, (text, percent, done) -> {
            Snapshot current = snapshot();
            set(app, current.progress("Прошивка адаптера: " + text, percent, 100, false, !done));
            if (done) {
                AppLog.line(app, "Прошивка адаптера OTA: " + text + " " + percent + "%");
                synchronized (FirmwareReleaseUpdater.class) {
                    busy = false;
                }
            }
        }).start();
    }

    private static String readText(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, read, java.nio.charset.StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static String percent(long done, long total) {
        if (total <= 0) return done / 1024 + " KB";
        return Math.min(100, Math.round(done * 100f / total)) + "%";
    }

    private static void set(Context context, Snapshot next) {
        synchronized (FirmwareReleaseUpdater.class) {
            snapshot = next;
        }
        if (context != null) {
            Intent intent = new Intent(ACTION_STATE);
            intent.setPackage(context.getPackageName());
            context.sendBroadcast(intent);
        }
    }

    private static final class ReleaseInfo {
        String tagName = "";
        String releaseName = "";
        String assetName = "";
        String downloadUrl = "";
        long assetSize;
    }

    static final class Snapshot {
        final String status;
        final String tagName;
        final String releaseName;
        final String assetName;
        final String downloadUrl;
        final long downloadedBytes;
        final long totalBytes;
        final long assetSize;
        final boolean checking;
        final boolean downloading;
        final boolean downloaded;
        final boolean flashing;

        Snapshot(String status, String tagName, String releaseName, String assetName,
                 String downloadUrl, long downloadedBytes, long totalBytes, long assetSize,
                 boolean checking, boolean downloading, boolean downloaded, boolean flashing) {
            this.status = status;
            this.tagName = tagName;
            this.releaseName = releaseName;
            this.assetName = assetName;
            this.downloadUrl = downloadUrl;
            this.downloadedBytes = downloadedBytes;
            this.totalBytes = totalBytes;
            this.assetSize = assetSize;
            this.checking = checking;
            this.downloading = downloading;
            this.downloaded = downloaded;
            this.flashing = flashing;
        }

        Snapshot copy(String status, boolean checking, boolean downloading, boolean downloaded, boolean flashing) {
            return new Snapshot(status, tagName, releaseName, assetName, downloadUrl, downloadedBytes,
                    totalBytes, assetSize, checking, downloading, downloaded, flashing);
        }

        Snapshot progress(String status, long downloadedBytes, long totalBytes, boolean downloading, boolean flashing) {
            return new Snapshot(status, tagName, releaseName, assetName, downloadUrl, downloadedBytes,
                    totalBytes, assetSize, false, downloading, downloaded, flashing);
        }

        Snapshot downloaded(boolean downloaded) {
            return new Snapshot(status, tagName, releaseName, assetName, downloadUrl, downloadedBytes,
                    totalBytes, assetSize, false, false, downloaded, flashing);
        }
    }
}
