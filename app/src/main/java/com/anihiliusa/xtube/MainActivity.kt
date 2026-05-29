package com.anihiliusa.xtube

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.io.ByteArrayInputStream

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var gestures: GestureDetector
    private val homeUrl = "https://m.youtube.com/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(11, 11, 15)
        window.navigationBarColor = Color.rgb(11, 11, 15)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.rgb(11, 11, 15))
        webView = WebView(this)
        webView.setBackgroundColor(Color.rgb(11, 11, 15))
        webView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progress.max = 100
        progress.visibility = View.GONE
        progress.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 6)
        val settingsButton = TextView(this)
        settingsButton.text = "⚙"
        settingsButton.textSize = 22f
        settingsButton.gravity = Gravity.CENTER
        settingsButton.setTextColor(Color.WHITE)
        settingsButton.setBackgroundColor(Color.argb(130, 0, 0, 0))
        val p = FrameLayout.LayoutParams(70, 70, Gravity.TOP or Gravity.END)
        p.setMargins(0, 18, 18, 0)
        settingsButton.layoutParams = p
        settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        settingsButton.setOnLongClickListener { moveTaskToBack(true); true }
        root.addView(webView)
        root.addView(progress)
        root.addView(settingsButton)
        setContentView(root)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgentString.replace("; wv", "")
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                return if (AdBlockRules.isBlocked(this@MainActivity, url)) emptyResponse() else null
            }
            override fun onPageFinished(view: WebView?, url: String?) { super.onPageFinished(view, url); injectCleaner() }
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) { handler?.cancel() }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                progress.progress = newProgress
                if (newProgress > 35) injectCleaner()
            }
        }
        gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val startX = e1?.x ?: return false
                val diffX = e2.x - startX
                val diffY = e2.y - e1.y
                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY) && kotlin.math.abs(diffX) > 160) {
                    if (diffX > 0 && webView.canGoBack()) webView.goBack()
                    if (diffX < 0 && webView.canGoForward()) webView.goForward()
                    return true
                }
                return false
            }
        })
        webView.setOnTouchListener { _, ev -> gestures.onTouchEvent(ev); false }
        if (AppPreferences.keepService(this)) startKeepAliveService()
        if (savedInstanceState == null) webView.loadUrl(homeUrl) else webView.restoreState(savedInstanceState)
    }

    override fun onResume() { super.onResume(); injectCleaner() }
    private fun startKeepAliveService() {
        val service = Intent(this, MediaKeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service) else startService(service)
    }
    private fun injectCleaner() {
        if (!AppPreferences.cleanUi(this)) return
        runOnUiThread { runCatching { webView.evaluateJavascript(CleanerScripts.cleanPage(AppPreferences.darkMode(this)), null) } }
    }
    private fun emptyResponse(): WebResourceResponse {
        val headers = mapOf("Access-Control-Allow-Origin" to "*", "Cache-Control" to "no-store")
        return WebResourceResponse("text/plain", "utf-8", 204, "No Content", headers, ByteArrayInputStream(ByteArray(0)))
    }
    override fun onBackPressed() { if (webView.canGoBack()) webView.goBack() else super.onBackPressed() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); webView.saveState(outState) }
    override fun onDestroy() { if (!AppPreferences.keepService(this)) stopService(Intent(this, MediaKeepAliveService::class.java)); webView.destroy(); super.onDestroy() }
}
