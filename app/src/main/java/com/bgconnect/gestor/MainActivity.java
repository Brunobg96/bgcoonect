package com.bgconnect.gestor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stopOrderAlert(getIntent());

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new NativeBridge(), "BGConnectNative");

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                registerPushToken(view);
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl("https://bgconnect.kinghost.net/admin.php");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onPause() {
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onStop() {
        CookieManager.getInstance().flush();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void stopOrderAlert(Intent intent) {
        if (intent != null &&
                intent.getBooleanExtra("stop_order_alert", false)) {

            Intent stopIntent =
                    new Intent(this, PedidoAlertService.class);

            stopIntent.setAction(PedidoAlertService.ACTION_STOP);

            startService(stopIntent);

            intent.removeExtra("stop_order_alert");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        stopOrderAlert(intent);
    }



    private void registerPushToken(WebView view) {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {

                        if (!task.isSuccessful()) return;

                        String token = task.getResult();
                        if (token == null || token.trim().isEmpty()) return;

                        String tokenJs = JSONObject.quote(token);
                        String deviceJs = JSONObject.quote(
                                Build.MANUFACTURER + " " + Build.MODEL
                        );

                        String js =
                                "(function(){try{" +
                                "if(!window.USER||!window.CSRF)return;" +
                                "fetch('api/push.php?action=register',{" +
                                "method:'POST'," +
                                "credentials:'same-origin'," +
                                "cache:'no-store'," +
                                "headers:{" +
                                "'Content-Type':'application/json'," +
                                "'X-CSRF-Token':window.CSRF" +
                                "}," +
                                "body:JSON.stringify({" +
                                "token:" + tokenJs + "," +
                                "device_name:" + deviceJs +
                                "})" +
                                "}).catch(function(){});" +
                                "}catch(e){}})();";

                        runOnUiThread(() ->
                                view.evaluateJavascript(js, null)
                        );
                    });

        } catch (Exception ignored) {}
    }



    private class NativeBridge {

        @JavascriptInterface
        public void stopOrderAlert() {
            runOnUiThread(() -> {
                try {
                    Intent stopIntent =
                            new Intent(MainActivity.this, PedidoAlertService.class);

                    stopIntent.setAction(PedidoAlertService.ACTION_STOP);
                    startService(stopIntent);

                } catch (Exception ignored) {}
            });
        }
    }


}
