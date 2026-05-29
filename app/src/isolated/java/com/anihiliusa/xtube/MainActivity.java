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
    private boolean isFullscreen = false;
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
            if (event.getAction() == MotionEvent.ACTION_DOWN && !isFullscreen) showTopBarTemporarily();
            return false;
        });

        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(10), dp(4), dp(8), dp(4));
        topBar.setBackgroundColor(Color.rgb(5, 5, 8));
        topBar.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(39), Gravity.TOP));

        TextView title = new TextView(this);
        title.setText("▶ Xtube");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14f);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        topBar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView home = new TextView(this);
        home.setText("YouTube");
        home.setTextColor(Color.WHITE);
        home.setTextSize(11f);
        home.setGravity(Gravity.CENTER);
        home.setBackgroundColor(Color.rgb(35, 35, 42));
        home.setOnClickListener(v -> loadUrl("https://www.youtube.com/"));
        topBar.addView(home, new LinearLayout.LayoutParams(dp(76), dp(28)));

        TextView settings = new TextView(this);
        settings.setText("⚙");
        settings.setTextColor(Color.WHITE);
        settings.setTextSize(18f);
        settings.setGravity(Gravity.CENTER);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        topBar.addView(settings, new LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.MATCH_PARENT));

        status = new TextView(this);
        status.setText("Firefox engine");
        status.setVisibility(View.GONE);

        root.addView(geckoView);
        root.addView(topBar);
        root.addView(status);
        setContentView(root);

        runtime = GeckoRuntime.create(this);
        installBuiltInExtension("resource://android/assets/extensions/ublock_origin.xpi", "uBlock0@raymondhill.net");
        session = new GeckoSession();
        session.open(runtime);
        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onFullScreen(GeckoSession session, boolean fullScreen) {
                applyFullscreen(fullScreen);
            }
        });
        geckoView.setSession(session);

        startBackgroundHelper();
        showTopBarTemporarily();
        loadUrl("https://www.youtube.com/");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showTopBarTemporarily() {
        if (topBar == null || isFullscreen) return;
        topBar.setVisibility(View.VISIBLE);
        topBar.animate().alpha(1f).setDuration(120).start();
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler.postDelayed(() -> {
            if (topBar != null && !isFullscreen) {
                topBar.animate().alpha(0f).setDuration(250).withEndAction(() -> topBar.setVisibility(View.GONE)).start();
            }
        }, 1500);
    }

    private void applyFullscreen(boolean fullScreen) {
        isFullscreen = fullScreen;
        View decor = getWindow().getDecorView();
        if (fullScreen) {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            if (topBar != null) topBar.setVisibility(View.GONE);
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            showTopBarTemporarily();
        }
    }

    private void loadUrl(String url) {
        if (session != null) session.loadUri(url);
    }

    private void installBuiltInExtension(String uri, String id) {
        try {
            Object controller = GeckoRuntime.class.getMethod("getWebExtensionController").invoke(runtime);
            Method method = controller.getClass().getMethod("ensureBuiltIn", String.class, String.class);
            method.invoke(controller, uri, id);
        } catch (Exception ignored) {
        }
    }

    private void startBackgroundHelper() {
        try {
            Intent intent = new Intent(this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
            else startService(intent);
        } catch (Exception ignored) {}
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
        if (isFullscreen) {
            applyFullscreen(false);
            return;
        }
        if (session != null) session.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacksAndMessages(null);
        if (session != null) session.close();
        super.onDestroy();
    }
}
