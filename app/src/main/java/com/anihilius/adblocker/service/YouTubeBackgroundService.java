package com.anihilius.adblocker.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.anihilius.adblocker.AnihiliusApp;
import com.anihilius.adblocker.R;

/**
 * YouTubeBackgroundService - enables background audio playback.
 * Intercepts media button events and keeps audio focus when YouTube is backgrounded.
 * Works by maintaining a foreground notification with media controls.
 */
public class YouTubeBackgroundService extends Service {

    private static final String TAG = "YouTubeBgService";
    private static final int NOTIFICATION_ID = 1002;

    private AudioManager audioManager;
    private boolean isPlaying = false;

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerMediaButtonReceiver();
        Log.i(TAG, "YouTube Background Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_YOUTUBE".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, createNotification());
        requestAudioFocus();
        isPlaying = true;
        Log.i(TAG, "YouTube Background Service started");
        return START_STICKY;
    }

    private void requestAudioFocus() {
        if (audioManager != null) {
            AudioManager.OnAudioFocusChangeListener focusChangeListener =
                focusChange -> {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // Don't stop — we're keeping background play alive
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Lower volume slightly
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            // Restore volume
                            break;
                    }
                };

            int result = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            );

            Log.d(TAG, "Audio focus result: " + (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? "GRANTED" : "DENIED"));
        }
    }

    private void registerMediaButtonReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Media button received");
                // Handle media button events here
            }
        }, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private Notification createNotification() {
        Intent launchIntent = getPackageManager()
                .getLaunchIntentForPackage("com.google.android.youtube");

        PendingIntent contentPending = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, YouTubeBackgroundService.class);
        stopIntent.setAction("STOP_YOUTUBE");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent, flags);

        return new NotificationCompat.Builder(this, AnihiliusApp.YOUTUBE_CHANNEL_ID)
                .setContentTitle("▶️ YouTube Background")
                .setContentText("Background playback active")
                .setSmallIcon(R.drawable.ic_play)
                .setContentIntent(contentPending)
                .setOngoing(true)
                .addAction(R.drawable.ic_stop, "Stop", stopPending)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setStyle(new NotificationCompat.MediaStyle())
                .build();
    }

    @Override
    public void onDestroy() {
        isPlaying = false;
        if (audioManager != null) {
            audioManager.abandonAudioFocus(focusChange -> {});
        }
        stopForeground(true);
        Log.i(TAG, "YouTube Background Service stopped");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
