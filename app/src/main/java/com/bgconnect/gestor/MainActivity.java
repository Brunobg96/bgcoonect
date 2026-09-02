package com.bgconnect.gestor;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.*;
import android.widget.*;
import android.view.*;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String START_URL = "https://bgconnect.kinghost.net/admin.php";
    private static final String UPDATE_URL = "https://bgconnect.kinghost.net/api/app_distribution.php?action=latest";
    private static final long UPDATE_RECHECK_MS = 5 * 60 * 1000L;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER = 501;
    private static final int NOTIFICATION_PERMISSION = 601;
    private long lastUpdateCheck = 0L;
    private boolean updateCheckRunning = false;
    private AlertDialog updateDialog;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        stopPersistentOrderAlert();
        getWindow().setStatusBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        requestNotificationPermission();

        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " BGConnectAndroid/1.8 Production PersistentOrderVoice AutoUpdate");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.addJavascriptInterface(new NativeBridge(), "BGConnectNative");

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String scheme = u.getScheme() == null ? "" : u.getScheme();
                if (scheme.equals("http") || scheme.equals("https")) {
                    String host = u.getHost() == null ? "" : u.getHost();
                    if (host.endsWith("bgconnect.kinghost.net")) return false;
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch(Exception ignored) {}
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                registerPushTokenInPanel(view);
                installNativePendingSync(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;
                try { startActivityForResult(p.createIntent(), FILE_CHOOSER); }
                catch(Exception e) { filePathCallback = null; return false; }
                return true;
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        if (state == null) webView.loadUrl(urlFromIntent(getIntent())); else webView.restoreState(state);
        checkAppUpdate(true);
    }

    @Override protected void onResume() {
        super.onResume();
        if (System.currentTimeMillis() - lastUpdateCheck >= UPDATE_RECHECK_MS) checkAppUpdate(false);
    }

    private void checkAppUpdate(boolean immediate) {
        if (updateCheckRunning) return;
        if (!immediate && System.currentTimeMillis() - lastUpdateCheck < UPDATE_RECHECK_MS) return;
        updateCheckRunning = true;
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(UPDATE_URL + "&_=" + System.currentTimeMillis());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setUseCaches(false);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "BGConnectGestor/1.8 Android");
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) return;
                String body = readAll(conn.getInputStream());
                JSONObject root = new JSONObject(body);
                JSONObject app = root.optJSONObject("app");
                if (app == null || !app.optBoolean("has_apk", false)) return;
                int latestCode = app.optInt("version_code", 0);
                int currentCode = getCurrentVersionCode();
                if (latestCode <= currentCode) {
                    runOnUiThread(this::dismissUpdateDialogIfAny);
                    return;
                }
                String version = app.optString("version", "");
                String notes = app.optString("notes", "");
                String downloadUrl = app.optString("download_url", "https://bgconnect.kinghost.net/baixar-app.php");
                boolean force = app.optBoolean("force_update", false);
                runOnUiThread(() -> showUpdateDialog(version, notes, downloadUrl, force));
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
                lastUpdateCheck = System.currentTimeMillis();
                updateCheckRunning = false;
            }
        }, "bg-app-update-check").start();
    }

    private String readAll(InputStream input) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private int getCurrentVersionCode() {
        try {
            if (Build.VERSION.SDK_INT >= 28) return (int)getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    private void showUpdateDialog(String version, String notes, String downloadUrl, boolean force) {
        if (isFinishing() || (Build.VERSION.SDK_INT >= 17 && isDestroyed())) return;
        dismissUpdateDialogIfAny();
        StringBuilder message = new StringBuilder();
        if (force) message.append("Esta atualização é obrigatória para continuar usando o BG CONNECT Gestor.\n\n");
        else message.append("Uma nova versão do BG CONNECT Gestor está disponível.\n\n");
        if (version != null && !version.trim().isEmpty()) message.append("Versão: ").append(version.trim()).append("\n");
        if (notes != null && !notes.trim().isEmpty()) message.append("\nNovidades:\n").append(notes.trim());

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle("Nova atualização disponível")
            .setMessage(message.toString())
            .setPositiveButton("Atualizar agora", (dialog, which) -> openUpdateDownload(downloadUrl));
        if (!force) builder.setNegativeButton("Atualizar depois", null);
        updateDialog = builder.create();
        updateDialog.setCancelable(!force);
        updateDialog.setCanceledOnTouchOutside(!force);
        if (force) {
            updateDialog.setOnShowListener(d -> {
                updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> openUpdateDownload(downloadUrl));
            });
        }
        updateDialog.show();
    }

    private void dismissUpdateDialogIfAny() {
        try {
            if (updateDialog != null && updateDialog.isShowing()) updateDialog.dismiss();
        } catch (Exception ignored) {}
        updateDialog = null;
    }

    private void openUpdateDownload(String downloadUrl) {
        try {
            Uri uri = Uri.parse((downloadUrl == null || downloadUrl.trim().isEmpty())
                ? "https://bgconnect.kinghost.net/baixar-app.php" : downloadUrl.trim());
            Intent browser = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(browser);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir a página de atualização.", Toast.LENGTH_LONG).show();
        }
    }

    private String urlFromIntent(Intent intent) {
        String id = intent == null ? null : intent.getStringExtra("push_order_id");
        if ((id == null || id.isEmpty()) && intent != null) id = intent.getStringExtra("order_id");
        if (id != null && id.matches("\\d+")) return START_URL + "?push_order=" + Uri.encode(id);
        return START_URL;
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        stopPersistentOrderAlert();
        if (webView != null) webView.loadUrl(urlFromIntent(intent));
        checkAppUpdate(true);
    }

    private void stopPersistentOrderAlert() {
        try {
            Intent stop = new Intent(this, NewOrderAlertService.class);
            stop.setAction(NewOrderAlertService.ACTION_STOP);
            startService(stop);
        } catch (Exception ignored) {
            try { stopService(new Intent(this, NewOrderAlertService.class)); } catch(Exception ignored2) {}
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION);
        }
    }

    private void registerPushTokenInPanel(WebView view) {
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) return;
                String token = task.getResult();
                getSharedPreferences("bgconnect_push", MODE_PRIVATE).edit().putString("fcm_token", token).apply();
                String q = JSONObject.quote(token);
                String device = JSONObject.quote(Build.MANUFACTURER + " " + Build.MODEL);
                String js = "(function(){try{window.BG_ANDROID_PUSH_TOKEN="+q+";if(!window.USER||!window.CSRF)return;fetch('api/push.php?action=register',{method:'POST',credentials:'same-origin',cache:'no-store',headers:{'Content-Type':'application/json','X-CSRF-Token':window.CSRF},body:JSON.stringify({token:"+q+",device_name:"+device+"})}).catch(function(){});}catch(e){}})();";
                runOnUiThread(() -> view.evaluateJavascript(js, null));
            });
        } catch (Exception ignored) {}
    }

    private void installNativePendingSync(WebView view) {
        String js = "(function(){try{"+
            "function bgNativeSync(){fetch('api/orders.php?status=new',{credentials:'same-origin',cache:'no-store'}).then(function(r){if(!r.ok)throw 0;return r.json();}).then(function(j){if(window.BGConnectNative&&j&&Array.isArray(j.orders))window.BGConnectNative.updatePendingCount(j.orders.length);}).catch(function(){});}"+
            "bgNativeSync();"+
            "if(window.__BG_NATIVE_PENDING_TIMER)clearInterval(window.__BG_NATIVE_PENDING_TIMER);"+
            "window.__BG_NATIVE_PENDING_TIMER=setInterval(bgNativeSync,15000);"+
            "}catch(e){}})();";
        runOnUiThread(() -> view.evaluateJavascript(js, null));
    }

    private final class NativeBridge {
        @JavascriptInterface public void updatePendingCount(int count) {
            PendingOrderState.syncCount(MainActivity.this, count);
            NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            if (count <= 0) { stopPersistentOrderAlert(); if (nm != null) nm.cancelAll(); }
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    results = new Uri[n];
                    for (int i=0;i<n;i++) results[i]=data.getClipData().getItemAt(i).getUri();
                } else if (data.getData()!=null) results = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        CookieManager.getInstance().flush();
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        CookieManager.getInstance().flush();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        CookieManager.getInstance().flush();
        super.onDestroy();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        webView.saveState(out); super.onSaveInstanceState(out);
    }
}
