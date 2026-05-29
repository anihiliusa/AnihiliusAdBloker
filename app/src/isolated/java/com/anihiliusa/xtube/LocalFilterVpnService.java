package com.anihiliusa.xtube;

import android.net.VpnService;

public class LocalFilterVpnService extends VpnService {
    @Override
    public int onStartCommand(android.content.Intent intent, int flags, int startId) {
        stopSelf();
        return START_NOT_STICKY;
    }
}
