package com.bgconnect.gestor;

import android.app.*;
import android.os.Build;

public class BGConnectApp extends Application {
    public static final String CHANNEL_NEW_ORDERS = "bgconnect_new_orders";

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
        c.setDescription("Avisos de novos pedidos recebidos pela loja");
        c.enableVibration(true);
        c.setVibrationPattern(new long[]{0, 350, 180, 350});
        c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(c);
    }
}
