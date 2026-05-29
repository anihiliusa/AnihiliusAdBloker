package com.anihiliusa.xtube

import android.content.Context
import android.net.Uri
import java.util.concurrent.atomic.AtomicLong

object AdBlockRules {
    private val blockedCount = AtomicLong(0)

    private val blockedHostSuffixes = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "pagead2.googlesyndication.com", "adnxs.com",
        "adsystem.com", "adform.net", "advertising.com", "taboola.com",
        "outbrain.com", "criteo.com", "scorecardresearch.com", "quantserve.com",
        "moatads.com", "rubiconproject.com", "pubmatic.com", "openx.net",
        "yieldmo.com", "zedo.com", "smartadserver.com", "media.net", "mgid.com",
        "revcontent.com", "popads.net", "propellerads.com", "adsterra.com",
        "exoclick.com", "trafficjunky.net", "analytics.google.com", "googletagmanager.com",
        "hotjar.com", "facebook.net", "connect.facebook.net", "adsrvr.org", "bidswitch.net",
        "rlcdn.com", "tapad.com", "contextweb.com", "casalemedia.com", "sharethrough.com"
    )

    private val blockedUrlNeedles = setOf(
        "/ads/", "/ad/", "/advert", "/banner", "/sponsor", "/promoted",
        "googleads", "doubleclick", "pagead", "adservice", "adserver", "adunit",
        "preroll", "midroll", "trackingpixel", "utm_source=ad", "ad_type=",
        "adunit=", "ad_id=", "ima3.js", "vast.xml", "vpaid", "prebid", "gpt.js",
        "pubads", "adbreak", "ad_request", "adformat", "adplacement", "adclient"
    )

    private val coreMediaHostSuffixes = setOf(
        "youtube.com", "ytimg.com", "googlevideo.com", "gstatic.com",
        "google.com", "accounts.google.com", "googleusercontent.com"
    )

    fun getBlockedCount(): Long = blockedCount.get()

    fun isBlocked(context: Context, rawUrl: String): Boolean {
        if (!AppPreferences.urlBlock(context)) return false
        val lower = rawUrl.lowercase()
        val uri = runCatching { Uri.parse(lower) }.getOrNull() ?: return false
        val host = uri.host?.removePrefix("www.") ?: return false

        if (coreMediaHostSuffixes.any { host == it || host.endsWith(".$it") }) {
            val block = blockedUrlNeedles.any { lower.contains(it) && !lower.contains("googlevideo.com/videoplayback") }
            if (block) blockedCount.incrementAndGet()
            return block
        }

        val custom = AppPreferences.customRules(context).map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val hostBlocked = blockedHostSuffixes.any { host == it || host.endsWith(".$it") } ||
            custom.any { rule -> host == rule || host.endsWith(".$rule") || lower.contains(rule) }
        val urlBlocked = blockedUrlNeedles.any { lower.contains(it) }
        val result = hostBlocked || urlBlocked
        if (result) blockedCount.incrementAndGet()
        return result
    }
}
