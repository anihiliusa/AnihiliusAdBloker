package com.anihiliusa.xtube;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 301;

    private GeckoView geckoView;
    private GeckoRuntime runtime;
    private GeckoSession session;
    private boolean isFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(5, 5, 8));
        window.setNavigationBarColor(Color.rgb(5, 5, 8));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(5, 5, 8));
        root.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        geckoView = new GeckoView(this);
        geckoView.setBackgroundColor(Color.rgb(5, 5, 8));
        geckoView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(geckoView);
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
        session.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
            @Override
            public void onAndroidPermissionsRequest(GeckoSession session, String[] permissions, GeckoSession.PermissionDelegate.Callback callback) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
                }
                callback.grant();
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
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
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
        if (session != null) session.close();
        super.onDestroy();
    }
}
