package com.bgconnect.gestor;

import android.content.Intent;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BGConnectMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "Novo pedido - BG CONNECT";
        String body = "Você recebeu um novo pedido.";

        // Prioridade para mensagens DATA
        if (message.getData() != null) {

            if (message.getData().containsKey("title")) {
                title = message.getData().get("title");
            }

            if (message.getData().containsKey("body")) {
                body = message.getData().get("body");
            }
        }

        // Compatibilidade caso chegue notification + data
        if (message.getNotification() != null) {

            if (!message.getData().containsKey("title")
                    && message.getNotification().getTitle() != null) {
                title = message.getNotification().getTitle();
            }

            if (!message.getData().containsKey("body")
                    && message.getNotification().getBody() != null) {
                body = message.getNotification().getBody();
            }
        }

        Intent serviceIntent =
                new Intent(this, PedidoAlertService.class);

        serviceIntent.setAction(PedidoAlertService.ACTION_START);
        serviceIntent.putExtra("title", title);
        serviceIntent.putExtra("body", body);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
