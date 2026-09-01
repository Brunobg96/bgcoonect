package com.bgconnect.gestor;

import android.app.*;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class BGConnectApp extends Application {
    // Novo ID para aplicar corretamente som, vibração e badge mesmo em aparelhos
    // que já tinham o canal antigo criado.
    public static final String CHANNEL_NEW_ORDERS = "bgconnect_new_orders_v2";
    public static final String GROUP_NEW_ORDERS = "bgconnect_group_new_orders";

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel c = new NotificationChannel(
            CHANNEL_NEW_ORDERS,
            "Novos pedidos",
            NotificationManager.IMPORTANCE_HIGH
        );
        c.setDescription("Avisos prioritários de novos pedidos recebidos pela loja");
        c.enableVibration(true);
        c.setVibrationPattern(new long[]{0, 350, 180, 350});
        c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        c.setShowBadge(true);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        c.setSound(sound, attrs);
        nm.createNotificationChannel(c);
    }
}
