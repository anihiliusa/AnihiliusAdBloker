package com.anihiliusa.xtube;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

public class LocalFilterVpnService extends VpnService {
    private static final String CHANNEL_ID = "xtube_local_filter";
    private static final int NOTIFICATION_ID = 220;
    private ParcelFileDescriptor tunnel;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, notification());
        startTunnel();
        return START_STICKY;
    }

    private void startTunnel() {
        try {
            if (tunnel != null) return;
            Builder builder = new Builder();
            builder.setSession("Xtube Local Filter");
            builder.addAddress("10.111.0.2", 32);
            builder.addDnsServer("1.1.1.1");
            builder.addRoute("0.0.0.0", 0);
            tunnel = builder.establish();
        } catch (Exception ignored) {
        }
    }

    private Notification notification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Xtube local filter", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_menu_manage).setContentTitle("Xtube Local Filter").setContentText("Phone traffic filter layer is active").setContentIntent(pendingIntent).setOngoing(true).build();
    }

    @Override
    public void onDestroy() {
        try {
            if (tunnel != null) tunnel.close();
        } catch (Exception ignored) {
        }
        tunnel = null;
        super.onDestroy();
    }
}
