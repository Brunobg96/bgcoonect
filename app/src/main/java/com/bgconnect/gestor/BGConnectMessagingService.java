package com.bgconnect.gestor;

import android.app.*;
import android.content.*;
import android.media.RingtoneManager;
import android.net.Uri;
import com.google.firebase.messaging.*;
import java.util.Map;

public class BGConnectMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token) {
        super.onNewToken(token);
        getSharedPreferences("bgconnect_push", MODE_PRIVATE).edit().putString("fcm_token", token).apply();
    }

    @Override public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        Map<String,String> d = message.getData();
        String title = value(d.get("title"), "🔔 Novo pedido — BG CONNECT");
        String body = value(d.get("body"), "Você recebeu um novo pedido.");
        String orderId = value(d.get("order_id"), "0");
        showNewOrderNotification(title, body, orderId);
    }

    private String value(String v, String fallback) { return v == null || v.trim().isEmpty() ? fallback : v; }

    private void showNewOrderNotification(String title, String body, String orderId) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("push_order_id", orderId);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
            this,
            (int)(System.currentTimeMillis() & 0x7fffffff),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder b;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, BGConnectApp.CHANNEL_NEW_ORDERS);
        } else {
            b = new Notification.Builder(this);
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            b.setSound(sound).setVibrate(new long[]{0,350,180,350});
        }
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
         .setContentTitle(title)
         .setContentText(body)
         .setStyle(new Notification.BigTextStyle().bigText(body))
         .setContentIntent(pi)
         .setAutoCancel(true)
         .setPriority(Notification.PRIORITY_MAX)
         .setCategory(Notification.CATEGORY_MESSAGE)
         .setVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        int id;
        try { id = Integer.parseInt(orderId); } catch(Exception e) { id = (int)(System.currentTimeMillis() & 0x7fffffff); }
        nm.notify(10000 + Math.abs(id % 100000), b.build());
    }
}
