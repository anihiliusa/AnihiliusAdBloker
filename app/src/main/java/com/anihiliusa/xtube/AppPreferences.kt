package com.anihiliusa.xtube

import android.content.Context

object AppPreferences {
    private const val FILE = "xtube_settings"
    private const val KEY_CLEAN_UI = "clean_ui"
    private const val KEY_URL_BLOCK = "url_block"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_KEEP_SERVICE = "keep_service"
    private const val KEY_AUTO_START = "auto_start"
    private const val KEY_DNS_LAYER = "dns_layer"
    private const val KEY_PROXY_LAYER = "proxy_layer"
    private const val KEY_CUSTOM_RULES = "custom_rules"

    fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun cleanUi(context: Context) = prefs(context).getBoolean(KEY_CLEAN_UI, true)
    fun setCleanUi(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_CLEAN_UI, enabled).apply()

    fun urlBlock(context: Context) = prefs(context).getBoolean(KEY_URL_BLOCK, true)
    fun setUrlBlock(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_URL_BLOCK, enabled).apply()

    fun darkMode(context: Context) = prefs(context).getBoolean(KEY_DARK_MODE, true)
    fun setDarkMode(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()

    fun keepService(context: Context) = prefs(context).getBoolean(KEY_KEEP_SERVICE, true)
    fun setKeepService(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_KEEP_SERVICE, enabled).apply()

    fun autoStart(context: Context) = prefs(context).getBoolean(KEY_AUTO_START, false)
    fun setAutoStart(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_START, enabled).apply()

    fun dnsLayer(context: Context) = prefs(context).getBoolean(KEY_DNS_LAYER, false)
    fun setDnsLayer(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_DNS_LAYER, enabled).apply()

    fun proxyLayer(context: Context) = prefs(context).getBoolean(KEY_PROXY_LAYER, false)
    fun setProxyLayer(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_PROXY_LAYER, enabled).apply()

    fun customRules(context: Context): Set<String> = prefs(context).getStringSet(KEY_CUSTOM_RULES, emptySet()) ?: emptySet()
    fun setCustomRules(context: Context, rules: Set<String>) = prefs(context).edit().putStringSet(KEY_CUSTOM_RULES, rules).apply()
}
