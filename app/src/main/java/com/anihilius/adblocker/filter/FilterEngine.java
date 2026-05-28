package com.anihilius.adblocker.filter;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FilterEngine - core ad-blocking engine.
 * Loads and applies filter lists (hosts-based + domain-based).
 * Uses ConcurrentHashMap for thread-safe lookups during packet processing.
 */
public class FilterEngine {

    private static final String TAG = "FilterEngine";

    // Blocked domains (exact match)
    private final Set<String> blockedDomains = ConcurrentHashMap.newKeySet();

    // Blocked hostnames from /etc/hosts style lists
    private final Set<String> blockedHosts = ConcurrentHashMap.newKeySet();

    // Blocked URL patterns (substring match)
    private final Set<String> blockedUrlPatterns = ConcurrentHashMap.newKeySet();

    // Whitelisted domains
    private final Set<String> whitelistedDomains = ConcurrentHashMap.newKeySet();

    private volatile boolean isLoaded = false;
    private int totalRules = 0;

    private final Context context;

    public FilterEngine(Context context) {
        this.context = context;
    }

    /**
     * Load all built-in filter lists from assets
     */
    public void loadFilters() {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "Loading ad filters...");

        // Load hosts-style blocklists
        loadHostsFile("blocked_hosts.txt");
        loadHostsFile("adtrackers.txt");
        loadHostsFile("malware.txt");

        // Load domain blocklists (EasyList style)
        loadDomainList("easylist.txt");
        loadDomainList("easyprivacy.txt");
        loadDomainList("annoyances.txt");

        // Load custom user filters
        loadCustomFilters();

        isLoaded = true;
        long elapsed = System.currentTimeMillis() - startTime;
        Log.i(TAG, "Filters loaded: " + totalRules + " rules in " + elapsed + "ms");
    }

    /**
     * Load a hosts-format file (one domain per line)
     * Lines starting with # are comments
     */
    private void loadHostsFile(String filename) {
        try {
            InputStream is = context.getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Hosts format: "0.0.0.0 domain.com" or just "domain.com"
                String domain = line;
                if (line.startsWith("0.0.0.0 ") || line.startsWith("127.0.0.1 ")) {
                    domain = line.substring(line.indexOf(' ') + 1).trim();
                }

                // Remove leading dot if present
                if (domain.startsWith(".")) {
                    domain = domain.substring(1);
                }

                if (!domain.isEmpty() && !domain.contains(" ")) {
                    blockedHosts.add(domain.toLowerCase());
                    count++;
                }
            }
            reader.close();
            totalRules += count;
            Log.d(TAG, "Loaded " + count + " hosts from " + filename);
        } catch (IOException e) {
            Log.d(TAG, "Hosts file not found: " + filename + " (using built-in defaults)");
            loadDefaultHosts();
        }
    }

    /**
     * Load a domain-based filter list (EasyList format)
     */
    private void loadDomainList(String filename) {
        try {
            InputStream is = context.getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")
                        || line.startsWith("[")) continue;

                // Skip CSS/element hiding rules
                if (line.contains("##") || line.contains("#@#")) continue;

                // Handle domain-based rules: domain1,domain2||ads.example.com^
                if (line.startsWith("||")) {
                    String domain = line.substring(2).replace("^", "").replace("*", "");
                    if (!domain.isEmpty()) {
                        blockedDomains.add(domain.toLowerCase());
                        count++;
                    }
                }
                // Handle keyword rules: -ad-banner-
                else if (line.startsWith("-") && line.endsWith("-") && !line.contains("||")) {
                    blockedUrlPatterns.add(line.substring(1, line.length() - 1).toLowerCase());
                    count++;
                }
                // Handle URL pattern rules
                else if (line.contains("||") && line.contains("^")) {
                    int start = line.indexOf("||");
                    int end = line.indexOf("^", start);
                    String domain = line.substring(start + 2, end);
                    if (!domain.isEmpty()) {
                        blockedDomains.add(domain.toLowerCase());
                        count++;
                    }
                }
            }
            reader.close();
            totalRules += count;
            Log.d(TAG, "Loaded " + count + " rules from " + filename);
        } catch (IOException e) {
            Log.d(TAG, "Filter list not found: " + filename + " (skipping)");
        }
    }

    /**
     * Load custom user filters from app storage
     */
    private void loadCustomFilters() {
        File customFile = new File(context.getFilesDir(), "custom_filters.txt");
        if (!customFile.exists()) return;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(customFile));
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("!")) continue;

                if (line.startsWith("||")) {
                    String domain = line.substring(2).replace("^", "").replace("*", "");
                    blockedDomains.add(domain.toLowerCase());
                    count++;
                } else if (!line.startsWith("##") && !line.startsWith("@@")) {
                    blockedDomains.add(line.toLowerCase());
                    count++;
                }
            }
            reader.close();
            totalRules += count;
            Log.d(TAG, "Loaded " + count + " custom filters");
        } catch (IOException e) {
            Log.e(TAG, "Error loading custom filters", e);
        }
    }

    /**
     * Load default built-in ad domains when no asset files exist
     */
    private void loadDefaultHosts() {
        String[] defaultBlocked = {
            // Major ad networks
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "google-analytics.com", "googletagmanager.com", "googletagservices.com",
            "ad.doubleclick.net", "adsserver.google.com",
            "pagead2.googlesyndication.com", "adservice.google.com",

            // Facebook tracking
            "facebook.com/tr", "facebook.net/tr", "fbcdn.net",
            "graph.facebook.com", "connect.facebook.net",

            // Common ad servers
            "ads.yahoo.com", "ad.yieldmanager.com", "advertising.com",
            "adnxs.com", "adsrvr.org", "adtechus.com", "adtech.com",
            "amazon-adsystem.com", "moatads.com",
            "pubmatic.com", "rubiconproject.com", "openx.net",
            "criteo.com", "criteo.net", "taboola.com", "outbrain.com",
            "spotxchange.com", "spotx.tv",
            "media.net", "bidswitch.net", "demdex.net",

            // Analytics & tracking
            "mixpanel.com", "amplitude.com", "segment.io", "segment.com",
            "hotjar.com", "mouseflow.com", "crazyegg.com",
            "optimizely.com", "convert.com", "vwo.com",
            "branch.io", "adjust.com", "appsflyer.com",
            "instabug.com", "bugsnag.com", "crashlytics.com",

            // YouTube specific
            "youtubekids.com/ads", "youtube.com/ads",
            "s.youtube.com", "i.ytimg.com/vi/*/hqdefault.jpg",

            // Social media tracking pixels
            "pixel.facebook.com", "pixel.twitter.com",
            "bat.bing.com", "snap.licdn.com",

            // Malware / scam ad servers
            "malvertising.org", "scamads.com",
            "adk2x.com", "adk2.com",

            // Chinese ad networks
            "adsage.cn", "adsmogo.com", "adwhirl.com",
            "gads.mobi", "mopub.com"
        };

        for (String domain : defaultBlocked) {
            blockedHosts.add(domain.toLowerCase());
        }
        totalRules += defaultBlocked.length;
        Log.i(TAG, "Loaded " + defaultBlocked.length + " default blocked domains");
    }

    /**
     * Check if a hostname should be blocked
     */
    public boolean shouldBlock(String hostname) {
        if (hostname == null || hostname.isEmpty()) return false;

        hostname = hostname.toLowerCase();

        // Whitelist check first
        if (whitelistedDomains.contains(hostname)) return false;
        for (String wl : whitelistedDomains) {
            if (hostname.endsWith("." + wl)) return false;
        }

        // Exact match in hosts
        if (blockedHosts.contains(hostname)) return true;

        // Subdomain match in hosts
        String checkHost = hostname;
        while (checkHost.contains(".")) {
            if (blockedHosts.contains(checkHost)) return true;
            int dotIndex = checkHost.indexOf('.');
            checkHost = checkHost.substring(dotIndex + 1);
        }

        // Domain match
        for (String blocked : blockedDomains) {
            if (hostname.equals(blocked) || hostname.endsWith("." + blocked)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a URL should be blocked
     */
    public boolean shouldBlockUrl(String url) {
        if (url == null || url.isEmpty()) return false;

        String lowerUrl = url.toLowerCase();

        // Extract hostname from URL
        try {
            if (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) {
                String withoutProtocol = lowerUrl.substring(lowerUrl.indexOf("://") + 3);
                String hostname = withoutProtocol.split("[/?#]")[0];
                if (shouldBlock(hostname)) return true;
            }
        } catch (Exception e) {
            // Fall through to pattern matching
        }

        // Pattern matching
        for (String pattern : blockedUrlPatterns) {
            if (lowerUrl.contains(pattern)) return true;
        }

        return false;
    }

    /**
     * Check if ad-blocker is ready
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    public int getTotalRules() {
        return totalRules;
    }

    /**
     * Add a domain to whitelist
     */
    public void whitelistDomain(String domain) {
        whitelistedDomains.add(domain.toLowerCase());
    }

    /**
     * Remove a domain from whitelist
     */
    public void unwhitelistDomain(String domain) {
        whitelistedDomains.remove(domain.toLowerCase());
    }

    /**
     * Add a custom block rule
     */
    public void addCustomBlock(String domain) {
        blockedDomains.add(domain.toLowerCase());
        totalRules++;
    }

    /**
     * Get stats for display
     */
    public String getStats() {
        return "Domains: " + blockedDomains.size()
             + " | Hosts: " + blockedHosts.size()
             + " | Patterns: " + blockedUrlPatterns.size()
             + " | Total: " + totalRules;
    }
}
