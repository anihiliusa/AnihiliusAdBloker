package com.anihiliusa.xtube;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.lang.reflect.Method;
import java.net.URLEncoder;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 301;

    private GeckoView geckoView;
    private GeckoRuntime runtime;
    private GeckoSession session;
    private LinearLayout appHeader;
    private EditText searchInput;
    private boolean isFullscreen = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(5, 5, 8));
        window.setNavigationBarColor(Color.rgb(5, 5, 8));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(Color.rgb(5, 5, 8));
        shell.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        appHeader = new LinearLayout(this);
        appHeader.setOrientation(LinearLayout.VERTICAL);
        appHeader.setBackgroundColor(Color.rgb(5, 5, 8));
        appHeader.setPadding(dp(8), dp(4), dp(8), dp(6));
        appHeader.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        TextView title = new TextView(this);
        title.setText("▶ Xtube");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14f);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        topRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView home = new TextView(this);
        home.setText("YouTube");
        home.setTextColor(Color.WHITE);
        home.setTextSize(11f);
        home.setGravity(Gravity.CENTER);
        home.setBackgroundColor(Color.rgb(35, 35, 42));
        home.setOnClickListener(v -> loadUrl("https://www.youtube.com/"));
        topRow.addView(home, new LinearLayout.LayoutParams(dp(76), dp(28)));

        TextView settings = new TextView(this);
        settings.setText("⚙");
        settings.setTextColor(Color.WHITE);
        settings.setTextSize(18f);
        settings.setGravity(Gravity.CENTER);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        topRow.addView(settings, new LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(0, dp(4), 0, 0);
        searchRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("Search YouTube");
        searchInput.setHintTextColor(Color.rgb(165, 165, 170));
        searchInput.setTextColor(Color.WHITE);
        searchInput.setTextSize(14f);
        searchInput.setPadding(dp(12), 0, dp(12), 0);
        searchInput.setBackgroundColor(Color.rgb(32, 32, 38));
        searchInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                runSearch();
                return true;
            }
            return false;
        });
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(0, dp(36), 1f));

        TextView go = new TextView(this);
        go.setText("Go");
        go.setTextColor(Color.WHITE);
        go.setTextSize(12f);
        go.setGravity(Gravity.CENTER);
        go.setBackgroundColor(Color.rgb(44, 44, 52));
        go.setOnClickListener(v -> runSearch());
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(dp(44), dp(36));
        goParams.leftMargin = dp(6);
        searchRow.addView(go, goParams);

        TextView mic = new TextView(this);
        mic.setText("🎙");
        mic.setTextColor(Color.WHITE);
        mic.setTextSize(17f);
        mic.setGravity(Gravity.CENTER);
        mic.setBackgroundColor(Color.rgb(44, 44, 52));
        mic.setOnClickListener(v -> requestMicrophoneThenOpenVoiceSearch());
        LinearLayout.LayoutParams micParams = new LinearLayout.LayoutParams(dp(42), dp(36));
        micParams.leftMargin = dp(6);
        searchRow.addView(mic, micParams);

        appHeader.addView(topRow);
        appHeader.addView(searchRow);

        FrameLayout browserFrame = new FrameLayout(this);
        browserFrame.setBackgroundColor(Color.rgb(5, 5, 8));
        browserFrame.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        geckoView = new GeckoView(this);
        geckoView.setBackgroundColor(Color.rgb(5, 5, 8));
        geckoView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        browserFrame.addView(geckoView);

        shell.addView(appHeader);
        shell.addView(browserFrame);
        setContentView(shell);

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
        session.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
            @Override
            public void onAndroidPermissionsRequest(GeckoSession session, String[] permissions, GeckoSession.PermissionDelegate.Callback callback) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
                    callback.grant();
                } else {
                    callback.grant();
                }
            }

            @Override
            public void onMediaPermissionRequest(GeckoSession session, String uri, GeckoSession.PermissionDelegate.MediaSource[] video, GeckoSession.PermissionDelegate.MediaSource[] audio, GeckoSession.PermissionDelegate.MediaCallback callback) {
                GeckoSession.PermissionDelegate.MediaSource audioSource = audio != null && audio.length > 0 ? audio[0] : null;
                callback.grant(null, audioSource);
            }
        });
        geckoView.setSession(session);

        startBackgroundHelper();
        loadUrl("https://www.youtube.com/");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void runSearch() {
        try {
            String q = searchInput == null ? "" : searchInput.getText().toString().trim();
            if (q.length() == 0) return;
            String encoded = URLEncoder.encode(q, "UTF-8");
            loadUrl("https://www.youtube.com/results?search_query=" + encoded);
        } catch (Exception ignored) {
        }
    }

    private void requestMicrophoneThenOpenVoiceSearch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        }
        loadUrl("https://www.youtube.com/voice_search");
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
            if (appHeader != null) appHeader.setVisibility(View.GONE);
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            if (appHeader != null) appHeader.setVisibility(View.VISIBLE);
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
