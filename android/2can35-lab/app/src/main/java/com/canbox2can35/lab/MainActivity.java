package com.canbox2can35.lab;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
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
    }

    @Override
    protected void onDestroy() {
        if (server != null) server.stop();
        if (usb != null) usb.close();
        super.onDestroy();
    }
}
