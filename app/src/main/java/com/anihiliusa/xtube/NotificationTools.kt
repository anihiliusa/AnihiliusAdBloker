package com.anihiliusa.xtube

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object NotificationTools {
    const val CHANNEL_PLAYBACK = "xtube_playback"
    const val ID_KEEP_ALIVE = 2001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_PLAYBACK, "Xtube background mode", NotificationManager.IMPORTANCE_LOW)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun keepAliveNotification(context: Context): Notification {
        val pending = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, CHANNEL_PLAYBACK) else Notification.Builder(context)
        return builder.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Xtube").setContentText("Background mode is active").setContentIntent(pending).setOngoing(true).build()
    }
}
