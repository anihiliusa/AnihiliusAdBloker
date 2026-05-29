package com.anihiliusa.xtube

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(11, 11, 15)
        window.navigationBarColor = Color.rgb(11, 11, 15)

        val scroll = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(32, 32, 32, 42)
        root.setBackgroundColor(Color.rgb(11, 11, 15))
        scroll.addView(root)

        title(root, "Xtube Settings")
        note(root, "Clean WebView container with URL filter, cosmetic cleaner and background keep-alive mode.")
        addSwitch(root, "Clean UI / cosmetic filter", AppPreferences.cleanUi(this)) { AppPreferences.setCleanUi(this, it) }
        addSwitch(root, "URL/domain ad blocker", AppPreferences.urlBlock(this)) { AppPreferences.setUrlBlock(this, it) }
        addSwitch(root, "Dark interface CSS", AppPreferences.darkMode(this)) { AppPreferences.setDarkMode(this, it) }
        addSwitch(root, "Keep alive when minimized", AppPreferences.keepService(this)) {
            AppPreferences.setKeepService(this, it)
            val svc = Intent(this, MediaKeepAliveService::class.java)
            if (it) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc) } else stopService(svc)
        }
        addSwitch(root, "Auto-start background mode after boot", AppPreferences.autoStart(this)) { AppPreferences.setAutoStart(this, it) }

        section(root, "DNS / VPN / Proxy")
        addSwitch(root, "DNS/VPN layer toggle", AppPreferences.dnsLayer(this)) {
            AppPreferences.setDnsLayer(this, it)
            Toast.makeText(this, "DNS/VPN engine is saved as next module setting.", Toast.LENGTH_LONG).show()
        }
        addSwitch(root, "HTTPS proxy layer toggle", AppPreferences.proxyLayer(this)) {
            AppPreferences.setProxyLayer(this, it)
            Toast.makeText(this, "HTTPS proxy requires certificate and whitelist flow before activation.", Toast.LENGTH_LONG).show()
        }

        section(root, "Custom block rules")
        val edit = EditText(this)
        edit.setText(AppPreferences.customRules(this).joinToString("\n"))
        edit.setTextColor(Color.WHITE)
        edit.setHintTextColor(Color.GRAY)
        edit.hint = "one domain/keyword per line: doubleclick.net, /ads/, sponsor"
        edit.minLines = 6
        edit.setBackgroundColor(Color.rgb(23, 23, 31))
        root.addView(edit)
        button(root, "Save custom rules") {
            val rules = edit.text.toString().lines().map { it.trim() }.filter { it.isNotBlank() }.toSet()
            AppPreferences.setCustomRules(this, rules)
            Toast.makeText(this, "Saved ${rules.size} rules", Toast.LENGTH_SHORT).show()
        }

        section(root, "Plugin / protected browser mode")
        button(root, "Open Firefox / Mull / Brave") { BrowserLauncher.openProtectedBrowser(this) }
        note(root, "Real browser-extension filtering belongs in a browser/GeckoView layer. Standard Android WebView does not load browser extensions directly.")

        section(root, "System")
        button(root, "Allow battery optimization exemption") { openBatteryOptimizationSettings() }
        button(root, "Minimize Xtube") { moveTaskToBack(true) }
        button(root, "Close settings") { finish() }
        setContentView(scroll)
    }

    private fun title(root: LinearLayout, text: String) {
        val v = TextView(this)
        v.text = text
        v.textSize = 26f
        v.setTextColor(Color.WHITE)
        v.gravity = Gravity.START
        root.addView(v)
    }

    private fun section(root: LinearLayout, text: String) {
        val v = TextView(this)
        v.text = "\n$text"
        v.textSize = 20f
        v.setTextColor(Color.rgb(255, 70, 70))
        root.addView(v)
    }

    private fun note(root: LinearLayout, text: String) {
        val v = TextView(this)
        v.text = text
        v.textSize = 14f
        v.setTextColor(Color.LTGRAY)
        v.setPadding(0, 8, 0, 12)
        root.addView(v)
    }

    private fun addSwitch(root: LinearLayout, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val sw = Switch(this)
        sw.text = label
        sw.textSize = 16f
        sw.setTextColor(Color.WHITE)
        sw.isChecked = checked
        sw.setPadding(0, 8, 0, 8)
        sw.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean -> onChange(isChecked) }
        root.addView(sw)
    }

    private fun button(root: LinearLayout, text: String, action: () -> Unit) {
        val b = Button(this)
        b.text = text
        b.setOnClickListener { action() }
        root.addView(b)
    }

    private fun openBatteryOptimizationSettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }.onFailure {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
