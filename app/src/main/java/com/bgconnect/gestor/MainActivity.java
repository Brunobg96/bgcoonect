package com.bgconnect.gestor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stopOrderAlert(getIntent());

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("BGCONNECT_FCM", "Falha ao obter token FCM", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("BGCONNECT_FCM", "TOKEN=" + token);

                    runOnUiThread(() -> {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Token Firebase")
                                .setMessage(token)
                                .setCancelable(false)
                                .setPositiveButton("Copiar", (dialog, which) -> {
                                    ClipboardManager clipboard =
                                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("FCM Token", token)
                                    );
                                })
                                .setNegativeButton("Fechar", null)
                                .show();
                    });
                    Toast.makeText(MainActivity.this, token, Toast.LENGTH_LONG).show();
                });

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient());

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


}
