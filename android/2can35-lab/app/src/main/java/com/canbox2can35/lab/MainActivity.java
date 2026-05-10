package com.canbox2can35.lab;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 35;

    private LabHttpServer server;
    private UsbCdcManager usb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        usb = new UsbCdcManager(this);
        server = new LabHttpServer(this, usb, 8765);
        server.start();

        WebView webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("http://127.0.0.1:8765/");

        requestRuntimePermissions();
        promptSpecialAccessOnce();
    }

    @Override
    protected void onDestroy() {
        if (server != null) server.stop();
        if (usb != null) usb.close();
        super.onDestroy();
    }

    private void requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT < 23) return;
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        }
        requestPermissions(permissions.toArray(new String[0]), REQ_PERMISSIONS);
    }

    private void promptSpecialAccessOnce() {
        SharedPreferences prefs = getSharedPreferences("permissions", MODE_PRIVATE);
        if (prefs.getBoolean("special_prompt_seen", false)) return;
        prefs.edit().putBoolean("special_prompt_seen", true).apply();
        new AlertDialog.Builder(this)
                .setTitle("Доступ к музыке и навигации")
                .setMessage("Для чтения треков, источников и подсказок навигации включи Notification Access для 2CAN35 Lab. Android не дает это обычным popup, поэтому откроется системный экран.")
                .setPositiveButton("Открыть", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    } catch (Exception ignored) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + getPackageName())));
                    }
                })
                .setNegativeButton("Позже", null)
                .show();
    }
}
