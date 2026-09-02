package com.bgconnect.gestor;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;

public class NewOrderAlertService extends Service {
    public static final String ACTION_START = "com.bgconnect.gestor.START_ORDER_ALERT";
    public static final String ACTION_STOP = "com.bgconnect.gestor.STOP_ORDER_ALERT";
    private static final String CHANNEL = "bgconnect_pending_order_alert_voice_v2";
    private static final int NOTIFICATION_ID = 9917;
    private static final long REPEAT_DELAY_MS = 5000L;
    private static final String PREF = "bgconnect_active_alert";

    private Handler handler;
    private MediaPlayer player;
    private PowerManager.WakeLock wakeLock;
    private boolean active = false;

    @Override public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAlertAndSelf();
            return START_NOT_STICKY;
        }
        String orderId = read(intent, "order_id", getSaved("order_id", "0"));
        String title = read(intent, "title", getSaved("title", "Novo pedido recebido"));
        String body = read(intent, "body", getSaved("body", "Toque para abrir o pedido."));
        save(orderId, title, body);
        startForeground(NOTIFICATION_ID, buildNotification(orderId, title, body));
        if (!active) {
            active = true;
            acquireWakeLock();
            playVoice();
        }
        return START_STICKY;
    }

    private String read(Intent i, String key, String fallback) {
        if (i == null) return fallback;
        String v = i.getStringExtra(key);
        return v == null || v.trim().isEmpty() ? fallback : v;
    }

    private void save(String orderId, String title, String body) {
        getSharedPreferences(PREF, MODE_PRIVATE).edit()
            .putString("order_id", orderId).putString("title", title).putString("body", body).apply();
    }

    private String getSaved(String key, String fallback) {
        return getSharedPreferences(PREF, MODE_PRIVATE).getString(key, fallback);
    }

    private Notification buildNotification(String orderId, String title, String body) {
        Intent open = new Intent(this, MainActivity.class);
        open.putExtra("push_order_id", orderId);
        open.putExtra("stop_order_alert", true);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 9918, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_stat_order)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new Notification.BigTextStyle().bigText(body + "\nAlerta repetirá até você abrir o pedido."))
            .setContentIntent(pi)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setPriority(Notification.PRIORITY_MAX);
        return b.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel c = new NotificationChannel(CHANNEL, "Pedido aguardando atendimento", NotificationManager.IMPORTANCE_HIGH);
        c.setDescription("Mantém o aviso ativo enquanto houver pedido novo aguardando atendimento");
        Uri sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.novo_pedido);
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        c.setSound(sound, attrs);
        c.enableVibration(false);
        c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        c.setShowBadge(true);
        nm.createNotificationChannel(c);
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BGConnect:NewOrderAlert");
            wakeLock.acquire();
        } catch (Exception ignored) {}
    }

    private void playVoice() {
        if (!active) return;

        releasePlayer();

        try {
            player = new MediaPlayer();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                player.setAudioAttributes(
                    new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                );
            } else {
                player.setAudioStreamType(android.media.AudioManager.STREAM_ALARM);
            }

            android.content.res.AssetFileDescriptor afd =
                getResources().openRawResourceFd(R.raw.novo_pedido);

            if (afd == null) {
                releasePlayer();
                scheduleNext();
                return;
            }

            player.setDataSource(
                afd.getFileDescriptor(),
                afd.getStartOffset(),
                afd.getLength()
            );
            afd.close();

            player.setVolume(1.0f, 1.0f);

            player.setOnCompletionListener(mp -> {
                releasePlayer();
                scheduleNext();
            });

            player.setOnErrorListener((mp, what, extra) -> {
                releasePlayer();
                scheduleNext();
                return true;
            });

            player.prepare();
            player.start();

        } catch (Exception e) {
            releasePlayer();
            scheduleNext();
        }
    }

    private void scheduleNext() {
        if (!active) return;
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::playVoice, REPEAT_DELAY_MS);
    }

    private void releasePlayer() {
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch(Exception ignored) {}
            try { player.release(); } catch(Exception ignored) {}
            player = null;
        }
    }

    private void stopAlertAndSelf() {
        active = false;
        if (handler != null) handler.removeCallbacksAndMessages(null);
        releasePlayer();
        if (wakeLock != null) {
            try { if (wakeLock.isHeld()) wakeLock.release(); } catch(Exception ignored) {}
            wakeLock = null;
        }
        getSharedPreferences(PREF, MODE_PRIVATE).edit().clear().apply();
        try { stopForeground(true); } catch(Exception ignored) {}
        stopSelf();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (active) {
            try {
                Intent restart = new Intent(this, NewOrderAlertService.class);
                restart.setAction(ACTION_START);
                restart.putExtra("order_id", getSaved("order_id", "0"));
                restart.putExtra("title", getSaved("title", "🔔 Novo pedido - BG CONNECT"));
                restart.putExtra("body", getSaved("body", "Toque para abrir o pedido."));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(restart);
                } else {
                    startService(restart);
                }
            } catch (Exception ignored) {}
        }

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        try {
            if (handler != null) handler.removeCallbacksAndMessages(null);
        } catch (Exception ignored) {}

        releasePlayer();

        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {}

        // NÃO limpar o pedido salvo aqui.
        // Se o Android destruir o serviço, START_STICKY poderá restaurá-lo.
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
