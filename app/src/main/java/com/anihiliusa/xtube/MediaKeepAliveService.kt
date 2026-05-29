package com.anihiliusa.xtube

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MediaKeepAliveService : Service() {
    override fun onCreate() {
        super.onCreate()
        NotificationTools.ensureChannels(this)
        startForeground(NotificationTools.ID_KEEP_ALIVE, NotificationTools.keepAliveNotification(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}
