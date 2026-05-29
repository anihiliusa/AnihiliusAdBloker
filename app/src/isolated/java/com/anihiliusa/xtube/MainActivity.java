package com.anihiliusa.xtube;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends Activity {
    private GeckoView geckoView;
    private GeckoRuntime runtime;
    private GeckoSession session;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(5, 5, 8));
        window.setNavigationBarColor(Color.rgb(5, 5, 8));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(5, 5, 8));

        geckoView = new GeckoView(this);
        geckoView.setBackgroundColor(Color.rgb(5, 5, 8));
        geckoView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(24, 12, 24, 10);
        topBar.setBackgroundColor(Color.argb(210, 5, 5, 8));
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 82, Gravity.TOP);
        topBar.setLayoutParams(topParams);

        TextView logo = new TextView(this);
        logo.setText("▶ Xtube");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(18f);
        logo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        topBar.addView(logo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView settings = new TextView(this);
        settings.setText("⚙");
        settings.setTextColor(Color.WHITE);
        settings.setTextSize(24f);
        settings.setGravity(Gravity.CENTER);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        topBar.addView(settings, new LinearLayout.LayoutParams(72, ViewGroup.LayoutParams.MATCH_PARENT));

        status = new TextView(this);
        status.setText("Firefox engine");
        status.setTextColor(Color.rgb(255, 80, 80));
        status.setTextSize(12f);
        status.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 42, Gravity.BOTTOM);
        status.setLayoutParams(sp);
        status.setBackgroundColor(Color.argb(150, 5, 5, 8));

        root.addView(geckoView);
        root.addView(topBar);
        root.addView(status);
        setContentView(root);

        runtime = GeckoRuntime.create(this);
        session = new GeckoSession();
        session.open(runtime);
        geckoView.setSession(session);

        startBackgroundHelper();
        session.loadUri("https://m.youtube.com/");
    }

    private void startBackgroundHelper() {
        try {
            Intent intent = new Intent(this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        startBackgroundHelper();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundHelper();
    }

    @Override
    public void onBackPressed() {
        if (session != null) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
        }
        super.onDestroy();
    }
}
