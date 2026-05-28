package com.anihilius.adblocker.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.anihilius.adblocker.filter.FilterEngine;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * FilterUpdateService - periodically checks for filter list updates.
 * Downloads latest filter lists from online sources and updates FilterEngine.
 */
public class FilterUpdateService extends Service {

    private static final String TAG = "FilterUpdateService";
    private static final long UPDATE_INTERVAL_HOURS = 6; // Update every 6 hours

    private ScheduledExecutorService scheduler;

    @Override
    public void onCreate() {
        super.onCreate();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scheduler.scheduleAtFixedRate(this::checkForUpdates, 0, UPDATE_INTERVAL_HOURS, TimeUnit.HOURS);
        Log.i(TAG, "Filter update service started (interval: " + UPDATE_INTERVAL_HOURS + "h)");
        return START_STICKY;
    }

    private void checkForUpdates() {
        try {
            Log.d(TAG, "Checking for filter updates...");

            // Filter update sources
            String[] filterUrls = {
                "https://easylist.to/easylist/easylist.txt",
                "https://easylist.to/easylist/easyprivacy.txt",
                "https://easylist-downloads.adblockplus.org/easylistadplus.txt",
                "https://secure.fanboy.co.nz/fanboy-annoyance.txt"
            };

            // In a real implementation, download and save each filter list
            // For now, we just log the check
            for (String url : filterUrls) {
                Log.d(TAG, "Would update from: " + url);
                // TODO: Implement actual HTTP download + save to assets/files
            }

            // Update the last check time
            getSharedPreferences("adblocker_prefs", MODE_PRIVATE)
                .edit()
                .putLong("last_filter_update", System.currentTimeMillis())
                .apply();

            Log.i(TAG, "Filter update check complete");

        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates", e);
        }
    }

    @Override
    public void onDestroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
