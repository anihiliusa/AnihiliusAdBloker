package com.anihiliusa.xtube

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object BrowserLauncher {
    private val protectedBrowsers = listOf(
        "org.mozilla.firefox",
        "org.mozilla.fenix",
        "us.spotco.fennec_dos",
        "org.mozilla.firefox_beta",
        "org.mozilla.fennec_fdroid",
        "com.brave.browser"
    )

    fun openProtectedBrowser(context: Context) {
        val pm = context.packageManager
        for (pkg in protectedBrowsers) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                context.startActivity(launch)
                return
            }
        }
        Toast.makeText(context, "Install Firefox or Brave for extension-based browsing", Toast.LENGTH_LONG).show()
        val uri = Uri.parse("market://details?id=org.mozilla.firefox")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.mozilla.firefox"))) }
    }
}
