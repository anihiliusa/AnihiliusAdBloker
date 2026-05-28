package com.anihilius.adblocker.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.anihilius.adblocker.R;
import com.anihilius.adblocker.service.AdBlockVpnService;
import com.anihilius.adblocker.service.YouTubeBackgroundService;
import com.anihilius.adblocker.filter.FilterEngine;

public class MainActivity extends AppCompatActivity {

    private Button btnToggle;
    private Switch switchYoutube;
    private Switch switchYtBackground;
    private TextView statusText;
    private TextView filterStatusText;
    private TextView statsText;

    private boolean isVpnActive = false;
    private Handler uiHandler;
    private Runnable statsUpdater;
    private SharedPreferences prefs;
    private FilterEngine filterEngine;

    private final ActivityResultLauncher<Intent> vpnPermissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    startVpnService();
                } else {
                    Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show();
                    btnToggle.setText("▶ START AD BLOCKER");
                    btnToggle.setBackgroundTintList(
                        getColorStateList(R.color.green));
                }
            }
        );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("adblocker_prefs", MODE_PRIVATE);
        uiHandler = new Handler(Looper.getMainLooper());
        filterEngine = new FilterEngine(this);

        initViews();
        setupListeners();
        loadSettings();

        // Load filters in background
        new Thread(() -> {
            filterEngine.loadFilters();
            runOnUiThread(() -> {
                filterStatusText.setText("✅ " + filterEngine.getTotalRules() + " rules loaded");
            });
        }).start();
    }

    private void initViews() {
        btnToggle = findViewById(R.id.btnToggle);
        switchYoutube = findViewById(R.id.switchYoutube);
        switchYtBackground = findViewById(R.id.switchYtBackground);
        statusText = findViewById(R.id.statusText);
        filterStatusText = findViewById(R.id.filterStatusText);
        statsText = findViewById(R.id.statsText);
    }

    private void setupListeners() {
        btnToggle.setOnClickListener(v -> {
            if (isVpnActive) {
                stopVpnService();
            } else {
                requestVpnPermission();
            }
        });

        switchYtBackground.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startYouTubeService();
            } else {
                stopYouTubeService();
            }
            prefs.edit().putBoolean("youtube_background", isChecked).apply();
        });

        switchYoutube.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("youtube_ads_block", isChecked).apply();
            Toast.makeText(this,
                isChecked ? "YouTube ad blocking enabled" : "YouTube ad blocking disabled",
                Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSettings() {
        isVpnActive = prefs.getBoolean("vpn_active", false);
        boolean ytBg = prefs.getBoolean("youtube_background", false);

        switchYtBackground.setChecked(ytBg);
        updateVpnUI();
        startStatsUpdater();
    }

    private void requestVpnPermission() {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent);
        } else {
            startVpnService();
        }
    }

    private void startVpnService() {
        Intent intent = new Intent(this, AdBlockVpnService.class);
        intent.setAction("START_VPN");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        isVpnActive = true;
        prefs.edit().putBoolean("vpn_active", true).apply();
        updateVpnUI();
        Toast.makeText(this, "🛡️ AdBlocker VPN activated", Toast.LENGTH_SHORT).show();
    }

    private void stopVpnService() {
        Intent intent = new Intent(this, AdBlockVpnService.class);
        intent.setAction("STOP_VPN");
        startService(intent);

        isVpnActive = false;
        prefs.edit().putBoolean("vpn_active", false).apply();
        updateVpnUI();
        Toast.makeText(this, "AdBlocker VPN deactivated", Toast.LENGTH_SHORT).show();
    }

    private void startYouTubeService() {
        Intent intent = new Intent(this, YouTubeBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "▶️ YouTube background playback enabled", Toast.LENGTH_SHORT).show();
    }

    private void stopYouTubeService() {
        Intent intent = new Intent(this, YouTubeBackgroundService.class);
        intent.setAction("STOP_YOUTUBE");
        startService(intent);
    }

    private void updateVpnUI() {
        if (isVpnActive) {
            statusText.setText("🟢 Protection Active");
            statusText.setTextColor(getColor(R.color.green));
            btnToggle.setText("■ STOP AD BLOCKER");
            btnToggle.setBackgroundTintList(
                getColorStateList(R.color.red_primary));
        } else {
            statusText.setText("⛔ Not Active");
            statusText.setTextColor(getColor(R.color.red_primary));
            btnToggle.setText("▶ START AD BLOCKER");
            btnToggle.setBackgroundTintList(
                getColorStateList(R.color.green));
        }
    }

    private void startStatsUpdater() {
        statsUpdater = new Runnable() {
            @Override
            public void run() {
                long blocked = AdBlockVpnService.getTotalBlocked();
                long allowed = AdBlockVpnService.getTotalAllowed();
                long bytes = AdBlockVpnService.getTotalBytes();

                statsText.setText(String.format(
                    "Checked: %d\nBlocked: %d\nAllowed: %d\nData: %s",
                    blocked + allowed, blocked, allowed, formatBytes(bytes)));

                uiHandler.postDelayed(this, 1000);
            }
        };
        uiHandler.post(statsUpdater);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVpnActive = prefs.getBoolean("vpn_active", false);
        updateVpnUI();
    }

    @Override
    protected void onDestroy() {
        if (statsUpdater != null) {
            uiHandler.removeCallbacks(statsUpdater);
        }
        super.onDestroy();
    }
}
