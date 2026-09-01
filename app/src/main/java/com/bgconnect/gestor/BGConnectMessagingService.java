package com.bgconnect.gestor;

import android.app.*;
import android.content.*;
import android.media.RingtoneManager;
import android.net.Uri;
import com.google.firebase.messaging.*;
import java.util.Map;

public class BGConnectMessagingService extends FirebaseMessagingService {
    private static final int SUMMARY_ID = 9090;

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
        int pendingCount = PendingOrderState.registerIncoming(this, orderId);
        showNewOrderNotification(title, body, orderId, pendingCount);
    }

    private String value(String v, String fallback) { return v == null || v.trim().isEmpty() ? fallback : v; }

    private PendingIntent orderIntent(String orderId, int requestCode) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("push_order_id", orderId);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent panelIntent(int requestCode) {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification.Builder baseBuilder() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return new Notification.Builder(this, BGConnectApp.CHANNEL_NEW_ORDERS);
        }
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return new Notification.Builder(this)
            .setSound(sound)
            .setVibrate(new long[]{0,350,180,350});
    }

    private void showNewOrderNotification(String title, String body, String orderId, int pendingCount) {
        int orderNumber;
        try { orderNumber = Integer.parseInt(orderId); }
        catch(Exception e) { orderNumber = (int)(System.currentTimeMillis() & 0x7fffffff); }
        int notificationId = 10000 + Math.abs(orderNumber % 100000);

        Notification.Builder b = baseBuilder();
        b.setSmallIcon(com.bgconnect.gestor.R.drawable.ic_stat_order)
         .setContentTitle(title)
         .setContentText(body)
         .setStyle(new Notification.BigTextStyle().bigText(body))
         .setContentIntent(orderIntent(orderId, notificationId))
         .addAction(new Notification.Action.Builder(null, "Ver pedido", orderIntent(orderId, notificationId + 300000)).build())
         .addAction(new Notification.Action.Builder(null, "Abrir painel", panelIntent(notificationId + 600000)).build())
         .setAutoCancel(true)
         .setPriority(Notification.PRIORITY_MAX)
         .setCategory(Notification.CATEGORY_MESSAGE)
         .setVisibility(Notification.VISIBILITY_PUBLIC)
         .setGroup(BGConnectApp.GROUP_NEW_ORDERS)
         .setNumber(Math.max(1, pendingCount));

        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(notificationId, b.build());
        updateSummary(nm, pendingCount);
    }

    private void updateSummary(NotificationManager nm, int pendingCount) {
        if (pendingCount <= 0) {
            nm.cancel(SUMMARY_ID);
            return;
        }
        String text = pendingCount == 1 ? "1 pedido novo aguardando decisão" : pendingCount + " pedidos novos aguardando decisão";
        Notification.Builder summary = baseBuilder();
        summary.setSmallIcon(com.bgconnect.gestor.R.drawable.ic_stat_order)
            .setContentTitle("BG CONNECT Gestor")
            .setContentText(text)
            .setStyle(new Notification.BigTextStyle().bigText(text))
            .setContentIntent(panelIntent(SUMMARY_ID + 1))
            .setGroup(BGConnectApp.GROUP_NEW_ORDERS)
            .setGroupSummary(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setNumber(pendingCount)
            .setVisibility(Notification.VISIBILITY_PUBLIC);
        nm.notify(SUMMARY_ID, summary.build());
    }
}
