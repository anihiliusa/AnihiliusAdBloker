package com.anihiliusa.xtube;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView webView;
    private ProgressBar progress;
    private TextView status;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(5, 5, 8));
        window.setNavigationBarColor(Color.rgb(5, 5, 8));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(5, 5, 8));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(5, 5, 8));
        webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        progress.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 6));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(18, 10, 18, 10);
        topBar.setBackgroundColor(Color.argb(185, 5, 5, 8));
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 64, Gravity.TOP);
        topBar.setLayoutParams(topParams);

        TextView logo = new TextView(this);
        logo.setText("▶ Xtube");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(18f);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        logo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        topBar.addView(logo, logoParams);

        status = new TextView(this);
        status.setText("Clean mode");
        status.setTextColor(Color.rgb(255, 80, 80));
        status.setTextSize(12f);
        status.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        topBar.addView(status, new LinearLayout.LayoutParams(220, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(webView);
        root.addView(progress);
        root.addView(topBar);
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setLoadsImagesAutomatically(true);
        String ua = settings.getUserAgentString();
        if (ua != null) settings.setUserAgentString(ua.replace("; wv", ""));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                status.setText("Loaded");
                injectCleaner();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                progress.setProgress(newProgress);
                status.setText(newProgress >= 100 ? "Clean mode" : "Loading " + newProgress + "%");
                if (newProgress > 30) injectCleaner();
            }
        });

        startBackgroundHelper();

        if (savedInstanceState == null) {
            webView.loadUrl("https://m.youtube.com/");
        } else {
            webView.restoreState(savedInstanceState);
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

    private void injectCleaner() {
        if (webView == null) return;
        String js = "(function(){" +
                "var css='body{background:#050508!important;} .ad,.ads,.advertisement,.sponsored,.promoted,ytd-ad-slot-renderer,ytd-display-ad-renderer,ytd-in-feed-ad-layout-renderer,ytd-promoted-sparkles-web-renderer,.ytp-ad-module,.video-ads,.ytp-ad-overlay-container,iframe[src*=doubleclick],ins.adsbygoogle{display:none!important;visibility:hidden!important;height:0!important;width:0!important;max-height:0!important;opacity:0!important;}';" +
                "if(!document.getElementById('xtube-style')){var st=document.createElement('style');st.id='xtube-style';st.textContent=css;document.documentElement.appendChild(st);}" +
                "var q=['.ad','.ads','.advertisement','.sponsored','.promoted','ytd-ad-slot-renderer','ytd-display-ad-renderer','ytd-in-feed-ad-layout-renderer','ytd-promoted-sparkles-web-renderer','.ytp-ad-module','.video-ads','.ytp-ad-overlay-container','iframe[src*=doubleclick]','ins.adsbygoogle'];" +
                "function c(){q.forEach(function(s){try{document.querySelectorAll(s).forEach(function(e){e.remove();});}catch(x){}});}" +
                "c();if(!window.__xtube){window.__xtube=1;setInterval(c,900);try{new MutationObserver(c).observe(document.documentElement,{childList:true,subtree:true});}catch(e){}}" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        startBackgroundHelper();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundHelper();
        if (webView != null) {
            webView.onResume();
            injectCleaner();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }
}
