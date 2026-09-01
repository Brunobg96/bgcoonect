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

public class MainActivity extends Activity {
    private static final String START_URL = "https://bgconnect.kinghost.net/admin.php";
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER = 501;
    private static final int NOTIFICATION_PERMISSION = 601;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
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
        s.setUserAgentString(s.getUserAgentString() + " BGConnectAndroid/1.1 PushFCM");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

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
    }

    private String urlFromIntent(Intent intent) {
        String id = intent == null ? null : intent.getStringExtra("push_order_id");
        if (id != null && id.matches("\\d+")) return START_URL + "?push_order=" + Uri.encode(id);
        return START_URL;
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (webView != null) webView.loadUrl(urlFromIntent(intent));
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

    @Override protected void onSaveInstanceState(Bundle out) {
        webView.saveState(out); super.onSaveInstanceState(out);
    }
}
