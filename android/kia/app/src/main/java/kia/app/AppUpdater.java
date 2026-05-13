package kia.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AppUpdater {
    static final String ACTION_STATE = "kia.app.APP_UPDATE_STATE";

    private static final String LATEST_MANIFEST_URL = "https://raw.githubusercontent.com/mrglonin/canbus2can35/main/updates/latest.json";
    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/mrglonin/canbus2can35/releases/latest";
    private static final String APK_MIME = "application/vnd.android.package-archive";
    private static final Pattern RELEASE_NUMBER = Pattern.compile("(?:kia[_-])?(\\d{3,})");
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static Snapshot snapshot = new Snapshot("Обновление: ожидание", "", "", "",
            "", "", 0, 0, 0, 0, false, false, false);
    private static boolean busy;

    private AppUpdater() {
    }

    static synchronized Snapshot snapshot() {
        return snapshot;
    }

    static void checkOnLaunch(Context context) {
        if (context == null || !AppPrefs.updateCheckOnLaunch(context)) return;
        check(context.getApplicationContext(), false);
    }

    static void checkNow(Context context) {
        if (context == null) return;
        check(context.getApplicationContext(), true);
    }

    static void downloadAndInstall(Activity activity) {
        if (activity == null) return;
        Snapshot current = snapshot();
        if (TextUtils.isEmpty(current.downloadUrl)) {
            Toast.makeText(activity, "Сначала проверьте обновление", Toast.LENGTH_SHORT).show();
            checkNow(activity);
            return;
        }
        if (!current.updateAvailable && current.latestRelease <= current.currentRelease) {
            Toast.makeText(activity, "Уже актуальная версия", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canInstallPackages(activity)) {
            set(activity, current.copy("Разрешите установку APK из этого источника", false, false, current.downloaded));
            openInstallPermission(activity);
            return;
        }
        File existing = downloadedFile(activity, current.assetName);
        if (existing.exists() && existing.length() > 0) {
            install(activity, existing);
            return;
        }
        download(activity, current);
    }

    static void installDownloaded(Activity activity) {
        if (activity == null) return;
        Snapshot current = snapshot();
        File file = downloadedFile(activity, current.assetName);
        if (!file.exists()) {
            Toast.makeText(activity, "APK ещё не скачан", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canInstallPackages(activity)) {
            openInstallPermission(activity);
            return;
        }
        install(activity, file);
    }

    private static void check(Context context, boolean interactive) {
        synchronized (AppUpdater.class) {
            if (busy) {
                if (interactive) Toast.makeText(context, "Проверка уже идёт", Toast.LENGTH_SHORT).show();
                return;
            }
            busy = true;
        }
        int currentRelease = currentRelease(context);
        set(context, new Snapshot("Обновление: проверка GitHub", currentVersion(context), "", "",
                "", "", currentRelease, 0, 0, 0, false, true, false));
        EXEC.execute(() -> {
            try {
                ReleaseInfo info = loadLatestRelease();
                int latestRelease = info.releaseNumber > 0 ? info.releaseNumber : releaseNumber(info.assetName + " " + info.tagName);
                boolean available = latestRelease > currentRelease;
                String status;
                if (TextUtils.isEmpty(info.downloadUrl)) {
                    status = "Обновление: git manifest/release APK не найден";
                } else if (available) {
                    status = "Обновление доступно: " + info.assetName;
                } else if (latestRelease > 0) {
                    status = "Обновление: актуальная версия";
                } else {
                    status = "Обновление: release найден, номер версии не распознан";
                }
                File file = downloadedFile(context, info.assetName);
                set(context, new Snapshot(status, currentVersion(context), info.tagName, info.releaseName,
                        info.assetName, info.downloadUrl, currentRelease, latestRelease, 0, info.assetSize,
                        available, false, false, file.exists() && file.length() > 0));
            } catch (Exception e) {
                set(context, new Snapshot("Обновление: ошибка " + e.getClass().getSimpleName(), currentVersion(context),
                        "", "", "", "", currentRelease, 0, 0, 0, false, false, false));
                AppLog.line(context, "Обновление APK: ошибка " + e.getClass().getSimpleName() + " " + e.getMessage());
            } finally {
                synchronized (AppUpdater.class) {
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
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "KiaCanbusUpdater");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String json = readText(stream);
        ReleaseInfo info = new ReleaseInfo();
        if (code == 404) return info;
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("GitHub manifest HTTP " + code + " " + json);
        }
        JSONObject root = new JSONObject(json);
        JSONObject app = root.optJSONObject("app");
        if (app == null) return info;
        info.tagName = root.optString("updated_at", "");
        info.releaseName = "git manifest";
        info.assetName = app.optString("asset_name", "");
        info.downloadUrl = app.optString("download_url", "");
        info.assetSize = app.optLong("size", 0);
        info.releaseNumber = app.optInt("release_number", releaseNumber(info.assetName + " " + app.optString("version_name", "")));
        return info;
    }

    private static ReleaseInfo loadGithubRelease() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "KiaCanbusUpdater");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String json = readText(stream);
        ReleaseInfo info = new ReleaseInfo();
        if (code == 404) return info;
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("GitHub HTTP " + code + " " + json);
        }
        JSONObject root = new JSONObject(json);
        info.tagName = root.optString("tag_name", "");
        info.releaseName = root.optString("name", "");
        JSONArray assets = root.optJSONArray("assets");
        if (assets == null) return info;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset == null) continue;
            String name = asset.optString("name", "");
            if (!name.toLowerCase(Locale.US).endsWith(".apk")) continue;
            if (!TextUtils.isEmpty(info.assetName) && !name.toLowerCase(Locale.US).contains("kia")) continue;
            info.assetName = name;
            info.downloadUrl = asset.optString("browser_download_url", "");
            info.assetSize = asset.optLong("size", 0);
            info.releaseNumber = releaseNumber(name + " " + info.tagName + " " + info.releaseName);
            if (name.toLowerCase(Locale.US).contains("kia")) break;
        }
        return info;
    }

    private static void download(Activity activity, Snapshot source) {
        synchronized (AppUpdater.class) {
            if (busy) {
                Toast.makeText(activity, "Загрузка уже идёт", Toast.LENGTH_SHORT).show();
                return;
            }
            busy = true;
        }
        Context app = activity.getApplicationContext();
        set(app, source.copy("Обновление: загрузка " + source.assetName, false, true, false));
        EXEC.execute(() -> {
            File out = downloadedFile(app, source.assetName);
            File tmp = new File(out.getParentFile(), out.getName() + ".part");
            try {
                if (!out.getParentFile().exists()) out.getParentFile().mkdirs();
                HttpURLConnection connection = (HttpURLConnection) new URL(source.downloadUrl).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", "KiaCanbusUpdater");
                long total = connection.getContentLengthLong();
                if (total <= 0) total = source.totalBytes;
                try (InputStream in = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream file = new FileOutputStream(tmp)) {
                    byte[] buffer = new byte[32768];
                    long done = 0;
                    long lastReport = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        file.write(buffer, 0, read);
                        done += read;
                        long now = System.currentTimeMillis();
                        if (now - lastReport > 600L) {
                            lastReport = now;
                            set(app, source.progress("Обновление: загрузка " + percent(done, total), done, total));
                        }
                    }
                }
                if (out.exists()) out.delete();
                if (!tmp.renameTo(out)) throw new IllegalStateException("rename failed");
                set(app, source.progress("Обновление: APK скачан", out.length(), total).downloaded(true));
                MAIN.post(() -> install(activity, out));
            } catch (Exception e) {
                tmp.delete();
                set(app, source.copy("Обновление: ошибка загрузки " + e.getClass().getSimpleName(), false, false, false));
                AppLog.line(app, "Обновление APK: ошибка загрузки " + e.getClass().getSimpleName() + " " + e.getMessage());
            } finally {
                synchronized (AppUpdater.class) {
                    busy = false;
                }
            }
        });
    }

    private static void install(Activity activity, File file) {
        try {
            Uri uri = Uri.parse("content://" + activity.getPackageName() + ".updateprovider/" + file.getName());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, APK_MIME);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
            set(activity, snapshot().copy("Обновление: открыт установщик Android", false, false, true));
        } catch (Exception e) {
            set(activity, snapshot().copy("Обновление: установщик не открылся", false, false, true));
            Toast.makeText(activity, "Не удалось открыть установщик: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static boolean canInstallPackages(Activity activity) {
        return Build.VERSION.SDK_INT < 26 || activity.getPackageManager().canRequestPackageInstalls();
    }

    private static void openInstallPermission(Activity activity) {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        } catch (Exception e) {
            activity.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        }
    }

    private static File downloadedFile(Context context, String assetName) {
        String safe = TextUtils.isEmpty(assetName) ? "kia_update.apk" : assetName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!safe.endsWith(".apk")) safe += ".apk";
        File root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (root == null) root = context.getFilesDir();
        return new File(new File(root, UpdateApkProvider.DIR), safe);
    }

    private static int currentRelease(Context context) {
        return releaseNumber(currentVersion(context));
    }

    private static String currentVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private static int releaseNumber(String value) {
        if (TextUtils.isEmpty(value)) return 0;
        Matcher matcher = RELEASE_NUMBER.matcher(value.replaceAll("[^0-9A-Za-z._-]", " "));
        int best = 0;
        while (matcher.find()) {
            try {
                best = Math.max(best, Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
        if (best > 0) return best;
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() >= 2 && digits.length() <= 4) {
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
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
        synchronized (AppUpdater.class) {
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
        int releaseNumber;
        long assetSize;
    }

    static final class Snapshot {
        final String status;
        final String currentVersion;
        final String tagName;
        final String releaseName;
        final String assetName;
        final String downloadUrl;
        final int currentRelease;
        final int latestRelease;
        final long downloadedBytes;
        final long totalBytes;
        final boolean updateAvailable;
        final boolean checking;
        final boolean downloading;
        final boolean downloaded;

        Snapshot(String status, String currentVersion, String tagName, String releaseName,
                 String assetName, String downloadUrl, int currentRelease, int latestRelease,
                 long downloadedBytes, long totalBytes, boolean updateAvailable, boolean checking,
                 boolean downloading) {
            this(status, currentVersion, tagName, releaseName, assetName, downloadUrl, currentRelease,
                    latestRelease, downloadedBytes, totalBytes, updateAvailable, checking, downloading, false);
        }

        Snapshot(String status, String currentVersion, String tagName, String releaseName,
                 String assetName, String downloadUrl, int currentRelease, int latestRelease,
                 long downloadedBytes, long totalBytes, boolean updateAvailable, boolean checking,
                 boolean downloading, boolean downloaded) {
            this.status = status;
            this.currentVersion = currentVersion;
            this.tagName = tagName;
            this.releaseName = releaseName;
            this.assetName = assetName;
            this.downloadUrl = downloadUrl;
            this.currentRelease = currentRelease;
            this.latestRelease = latestRelease;
            this.downloadedBytes = downloadedBytes;
            this.totalBytes = totalBytes;
            this.updateAvailable = updateAvailable;
            this.checking = checking;
            this.downloading = downloading;
            this.downloaded = downloaded;
        }

        Snapshot copy(String status, boolean checking, boolean downloading, boolean downloaded) {
            return new Snapshot(status, currentVersion, tagName, releaseName, assetName, downloadUrl,
                    currentRelease, latestRelease, downloadedBytes, totalBytes, updateAvailable,
                    checking, downloading, downloaded);
        }

        Snapshot progress(String status, long downloadedBytes, long totalBytes) {
            return new Snapshot(status, currentVersion, tagName, releaseName, assetName, downloadUrl,
                    currentRelease, latestRelease, downloadedBytes, totalBytes, updateAvailable,
                    false, true, downloaded);
        }

        Snapshot downloaded(boolean downloaded) {
            return new Snapshot(status, currentVersion, tagName, releaseName, assetName, downloadUrl,
                    currentRelease, latestRelease, downloadedBytes, totalBytes, updateAvailable,
                    false, false, downloaded);
        }
    }
}
