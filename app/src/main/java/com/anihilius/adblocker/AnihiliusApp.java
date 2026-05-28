package com.anihilius.adblocker;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class AnihiliusApp extends Application {

    public static final String VPN_CHANNEL_ID = "anihilius_vpn_channel";
    public static final String YOUTUBE_CHANNEL_ID = "anihilius_youtube_channel";
    public static final String UPDATE_CHANNEL_ID = "anihilius_update_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        NotificationChannel vpnChannel = new NotificationChannel(
                VPN_CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
        );
        vpnChannel.setDescription("AdBlocker VPN protection");
        vpnChannel.setShowBadge(false);

        NotificationChannel youtubeChannel = new NotificationChannel(
                YOUTUBE_CHANNEL_ID,
                "YouTube Background",
                NotificationManager.IMPORTANCE_LOW
        );
        youtubeChannel.setDescription("YouTube background playback");
        youtubeChannel.setShowBadge(false);

        NotificationChannel updateChannel = new NotificationChannel(
                UPDATE_CHANNEL_ID,
                "Filter Updates",
                NotificationManager.IMPORTANCE_LOW
        );
        updateChannel.setDescription("Ad filter update notifications");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(vpnChannel);
            manager.createNotificationChannel(youtubeChannel);
            manager.createNotificationChannel(updateChannel);
        }
    }
}
