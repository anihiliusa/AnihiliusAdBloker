package com.anihiliusa.xtube

import android.app.Application

class XtubeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationTools.ensureChannels(this)
    }
}
