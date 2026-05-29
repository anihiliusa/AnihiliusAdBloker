package com.anihiliusa.xtube;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.Gravity;
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

        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(14), dp(8), dp(12), dp(8));
        topBar.setBackgroundColor(Color.rgb(5, 5, 8));
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(78), Gravity.TOP);
        topBar.setLayoutParams(topParams);

        TextView logo = new TextView(this);
        logo.setText("▶ Xtube");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(19f);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        logo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        topBar.addView(logo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView login = new TextView(this);
        login.setText("Login");
        login.setTextColor(Color.WHITE);
        login.setTextSize(14f);
        login.setGravity(Gravity.CENTER);
        login.setBackgroundColor(Color.rgb(35, 35, 42));
        login.setOnClickListener(v -> loadUrl("https://accounts.google.com/ServiceLogin?service=youtube"));
        topBar.addView(login, new LinearLayout.LayoutParams(dp(86), dp(46)));

        TextView settings = new TextView(this);
        settings.setText("⚙");
        settings.setTextColor(Color.WHITE);
        settings.setTextSize(24f);
        settings.setGravity(Gravity.CENTER);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        topBar.addView(settings, new LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.MATCH_PARENT));

        status = new TextView(this);
        status.setText("Firefox engine + uBO");
        status.setTextColor(Color.rgb(255, 80, 80));
        status.setTextSize(12f);
        status.setGravity(Gravity.CENTER);
        status.setVisibility(View.GONE);

        root.addView(geckoView);
        root.addView(topBar);
        root.addView(status);
        setContentView(root);

        runtime = GeckoRuntime.create(this);
        installBundledExtension();
        session = new GeckoSession();
        session.open(runtime);
        geckoView.setSession(session);

        startBackgroundHelper();
        loadUrl("https://m.youtube.com/");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void loadUrl(String url) {
        if (session != null) session.loadUri(url);
    }

    private void installBundledExtension() {
        try {
            Object controller = GeckoRuntime.class.getMethod("getWebExtensionController").invoke(runtime);
            Method method = controller.getClass().getMethod("ensureBuiltIn", String.class, String.class);
            method.invoke(controller, "resource://android/assets/extensions/ublock_origin.xpi", "uBlock0@raymondhill.net");
            if (status != null) status.setText("Firefox engine + uBO");
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

    private void enterPipIfPossible() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return;
        try {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build();
            enterPictureInPictureMode(params);
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
        enterPipIfPossible();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundHelper();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (topBar != null) topBar.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
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
