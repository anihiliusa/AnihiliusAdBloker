package com.anihilius.adblocker.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BootReceiver - auto-starts VPN after device reboot
 * if the user had it enabled.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.i(TAG, "Boot completed, checking auto-start preference");

            boolean autoStart = context.getSharedPreferences("adblocker_prefs", Context.MODE_PRIVATE)
                    .getBoolean("auto_start_on_boot", false);

            if (autoStart) {
                Intent vpnIntent = new Intent(context, AdBlockVpnService.class);
                vpnIntent.setAction("START_VPN");
                context.startForegroundService(vpnIntent);
                Log.i(TAG, "Auto-starting VPN after boot");
            }
        }
    }
}
