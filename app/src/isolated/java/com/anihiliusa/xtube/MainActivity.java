package com.anihiliusa.xtube;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private GeckoView geckoView;
    private GeckoRuntime runtime;
    private GeckoSession session;
    private TextView status;
    private LinearLayout topBar;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

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
        geckoView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) showTopBarTemporarily();
            return false;
        });

        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(10), dp(4), dp(8), dp(4));
        topBar.setBackgroundColor(Color.rgb(5, 5, 8));
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(39), Gravity.TOP);
        topBar.setLayoutParams(topParams);

        TextView logo = new TextView(this);
        logo.setText("▶ Xtube");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(14f);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        logo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        topBar.addView(logo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView login = new TextView(this);
        login.setText("Login");
        login.setTextColor(Color.WHITE);
        login.setTextSize(11f);
        login.setGravity(Gravity.CENTER);
        login.setBackgroundColor(Color.rgb(35, 35, 42));
        login.setOnClickListener(v -> loadUrl("https://accounts.google.com/ServiceLogin?service=youtube"));
        topBar.addView(login, new LinearLayout.LayoutParams(dp(66), dp(28)));

        TextView settings = new TextView(this);
        settings.setText("⚙");
        settings.setTextColor(Color.WHITE);
        settings.setTextSize(18f);
        settings.setGravity(Gravity.CENTER);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        topBar.addView(settings, new LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.MATCH_PARENT));

        status = new TextView(this);
        status.setText("Firefox engine + helper");
        status.setTextColor(Color.rgb(255, 80, 80));
        status.setTextSize(12f);
        status.setGravity(Gravity.CENTER);
        status.setVisibility(View.GONE);

        root.addView(geckoView);
        root.addView(topBar);
        root.addView(status);
        setContentView(root);

        runtime = GeckoRuntime.create(this);
        installBundledExtensions();
        session = new GeckoSession();
        session.open(runtime);
        geckoView.setSession(session);

        startBackgroundHelper();
        showTopBarTemporarily();
        loadUrl("https://m.youtube.com/");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showTopBarTemporarily() {
        if (topBar == null) return;
        topBar.setVisibility(View.VISIBLE);
        topBar.animate().alpha(1f).setDuration(120).start();
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler.postDelayed(() -> {
            if (topBar != null) topBar.animate().alpha(0f).setDuration(250).withEndAction(() -> topBar.setVisibility(View.GONE)).start();
        }, 1500);
    }

    private void loadUrl(String url) {
        if (session != null) session.loadUri(url);
    }

    private void installBundledExtensions() {
        tryInstallExtension("resource://android/assets/extensions/ublock_origin.xpi", "uBlock0@raymondhill.net");
        tryInstallExtension("resource://android/assets/extensions/xtube_helper/", "xtube-helper@anihiliusa");
    }

    private void tryInstallExtension(String uri, String id) {
        try {
            Object controller = GeckoRuntime.class.getMethod("getWebExtensionController").invoke(runtime);
            Method method = controller.getClass().getMethod("ensureBuiltIn", String.class, String.class);
            method.invoke(controller, uri, id);
            if (status != null) status.setText("Firefox engine + helper");
        } catch (Exception ignored) {
            if (status != null) status.setText("Firefox engine");
        }
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
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        startBackgroundHelper();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundHelper();
        showTopBarTemporarily();
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
        uiHandler.removeCallbacksAndMessages(null);
        if (session != null) {
            session.close();
        }
        super.onDestroy();
    }
}
