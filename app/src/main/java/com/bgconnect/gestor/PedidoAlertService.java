package com.bgconnect.gestor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class PedidoAlertService extends Service {

    public static final String ACTION_START = "com.bgconnect.gestor.START_ALERT";
    public static final String ACTION_STOP = "com.bgconnect.gestor.STOP_ALERT";

    private static final String CHANNEL_ID = "bgconnect_alerta_pedido_v1";
    private static final int NOTIFICATION_ID = 9090;

    private MediaPlayer player;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAlert();
            stopSelf();
            return START_NOT_STICKY;
        }

        createChannel();

        String title = intent != null
                ? intent.getStringExtra("title")
                : "Novo pedido - BG CONNECT";

        String body = intent != null
                ? intent.getStringExtra("body")
                : "Você recebeu um novo pedido.";

        startForeground(
                NOTIFICATION_ID,
                buildNotification(
                        title != null ? title : "Novo pedido - BG CONNECT",
                        body != null ? body : "Você recebeu um novo pedido."
                )
        );

        startAlert();

        return START_STICKY;
    }

    private void startAlert() {

        try {
            if (player != null) {
                try {
                    player.stop();
                } catch (Exception ignored) {}

                try {
                    player.release();
                } catch (Exception ignored) {}

                player = null;
            }

            player = new MediaPlayer();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                player.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                );
            } else {
                player.setAudioStreamType(AudioManager.STREAM_ALARM);
            }

            AssetFileDescriptor afd =
                    getResources().openRawResourceFd(R.raw.novo_pedido);

            if (afd == null) {
                return;
            }

            player.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength()
            );

            afd.close();

            player.setLooping(true);
            player.setVolume(1.0f, 1.0f);

            player.prepare();
            player.start();

        } catch (Exception e) {
            android.util.Log.e(
                    "BGCONNECT_ALERT",
                    "Erro ao reproduzir alerta",
                    e
            );
        }
    }

    private void stopAlert() {

        try {
            if (player != null) {
                if (player.isPlaying()) {
                    player.stop();
                }

                player.release();
                player = null;
            }
        } catch (Exception ignored) {
        }

        stopForeground(true);
    }

    private void createChannel() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Alerta de novos pedidos",
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setSound(null, null);
        channel.setDescription("Mantém o alerta de novo pedido ativo.");

        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification(String title, String body) {

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.putExtra("stop_order_alert", true);
        openIntent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        9091,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
                );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText(body)
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        stopAlert();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
