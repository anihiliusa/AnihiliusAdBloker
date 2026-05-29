package com.anihiliusa.xtube;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView webView;
    private ProgressBar progress;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 11, 15));
        getWindow().setNavigationBarColor(Color.rgb(11, 11, 15));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(11, 11, 15));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11, 11, 15));
        webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(ProgressBar.GONE);
        progress.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 7));

        TextView badge = new TextView(this);
        badge.setText("Xtube");
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(14f);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(Color.argb(130, 0, 0, 0));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(150, 58, Gravity.TOP | Gravity.END);
        bp.setMargins(0, 18, 18, 0);
        badge.setLayoutParams(bp);

        root.addView(webView);
        root.addView(progress);
        root.addView(badge);
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

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectCleaner();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setVisibility(newProgress >= 100 ? ProgressBar.GONE : ProgressBar.VISIBLE);
                progress.setProgress(newProgress);
                if (newProgress > 30) injectCleaner();
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl("https://m.youtube.com/");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void injectCleaner() {
        if (webView == null) return;
        String js = "(function(){" +
                "var css='.ad,.ads,.advertisement,.sponsored,.promoted,ytd-ad-slot-renderer,ytd-display-ad-renderer,.ytp-ad-module,.video-ads,iframe[src*=doubleclick],ins.adsbygoogle{display:none!important;visibility:hidden!important;height:0!important;width:0!important;} body{background:#0b0b0f!important;}';" +
                "if(!document.getElementById('xtube-style')){var st=document.createElement('style');st.id='xtube-style';st.textContent=css;document.documentElement.appendChild(st);}" +
                "var q=['.ad','.ads','.advertisement','.sponsored','.promoted','ytd-ad-slot-renderer','ytd-display-ad-renderer','.ytp-ad-module','.video-ads','iframe[src*=doubleclick]','ins.adsbygoogle'];" +
                "function c(){q.forEach(function(s){try{document.querySelectorAll(s).forEach(function(e){e.remove();});}catch(x){}});}" +
                "c();if(!window.__xtube){window.__xtube=1;setInterval(c,1200);try{new MutationObserver(c).observe(document.documentElement,{childList:true,subtree:true});}catch(e){}}" +
                "})();";
        webView.evaluateJavascript(js, null);
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
