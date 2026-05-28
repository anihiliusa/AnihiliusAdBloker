package com.anihilius.adblocker.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.anihilius.adblocker.AnihiliusApp;
import com.anihilius.adblocker.R;
import com.anihilius.adblocker.filter.FilterEngine;
import com.anihilius.adblocker.ui.MainActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AdBlockVpnService — Local VPN that intercepts DNS traffic
 * and blocks ad/tracking domains using FilterEngine.
 */
public class AdBlockVpnService extends VpnService {

    private static final String TAG = "AdBlockVpnService";
    private static final String VPN_ADDRESS = "10.0.0.2";
    private static final String VPN_ROUTE = "0.0.0.0";
    private static final int VPN_MTU = 1500;
    private static final int NOTIFICATION_ID = 1001;

    private ParcelFileDescriptor vpnInterface;
    private FilterEngine filterEngine;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread vpnThread;
    private PendingIntent stopPendingIntent;

    // Stats
    private static final AtomicLong totalBlocked = new AtomicLong(0);
    private static final AtomicLong totalAllowed = new AtomicLong(0);
    private static final AtomicLong totalBytes = new AtomicLong(0);

    @Override
    public void onCreate() {
        super.onCreate();
        filterEngine = new FilterEngine(this);
        filterEngine.loadFilters();

        Intent stopIntent = new Intent(this, AdBlockVpnService.class);
        stopIntent.setAction("STOP_VPN");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, flags);

        Log.i(TAG, "VPN Service created. Rules loaded: " + filterEngine.getTotalRules());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_VPN".equals(intent.getAction())) {
            stopVpn();
            return START_NOT_STICKY;
        }

        if (isRunning.get()) {
            Log.d(TAG, "VPN already running");
            return START_STICKY;
        }

        startVpn();
        return START_STICKY;
    }

    private void startVpn() {
        try {
            Builder builder = new Builder();
            builder.setSession("AnihiliusAdBlocker")
                   .setMtu(VPN_MTU)
                   .addAddress(VPN_ADDRESS, 32)
                   .addRoute(VPN_ROUTE, 0)
                   .addDnsServer("9.9.9.9")      // Quad9
                   .addDnsServer("1.1.1.1")      // Cloudflare
                   .addDnsServer("8.8.8.8");     // Google

            // Allow our own app to bypass VPN
            try {
                builder.addDisallowedApplication(getPackageName());
            } catch (Exception e) {
                Log.w(TAG, "Could not exclude own package");
            }

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "VPN interface is null - no permission?");
                stopSelf();
                return;
            }

            isRunning.set(true);
            totalBlocked.set(0);
            totalAllowed.set(0);
            totalBytes.set(0);

            startForeground(NOTIFICATION_ID, createNotification());

            vpnThread = new Thread(this::runVpnLoop, "VPN-Loop");
            vpnThread.start();

            Log.i(TAG, "VPN started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start VPN", e);
            stopSelf();
        }
    }

    /**
     * Main VPN packet processing loop.
     * Reads raw IP packets from the TUN interface, parses DNS queries,
     * and blocks requests to ad domains.
     */
    private void runVpnLoop() {
        FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
        FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());
        ByteBuffer packet = ByteBuffer.allocate(VPN_MTU);

        while (isRunning.get()) {
            try {
                packet.clear();
                int length = in.read(packet.array());
                if (length <= 0) {
                    Thread.sleep(10);
                    continue;
                }

                totalBytes.addAndGet(length);
                packet.limit(length);

                // Parse IP header
                int version = (packet.get(0) >> 4) & 0x0F;
                if (version != 4) {
                    // Forward non-IPv4 packets as-is
                    out.write(packet.array(), 0, length);
                    continue;
                }

                int protocol = packet.get(9) & 0xFF;
                int headerLength = (packet.get(0) & 0x0F) * 4;

                // Check if this is a DNS packet (UDP port 53)
                if (protocol == 17) { // UDP
                    int udpHeader = headerLength + 8; // IP header + UDP header
                    if (length > udpHeader + 12) {
                        int srcPort = ((packet.get(headerLength) & 0xFF) << 8)
                                    | (packet.get(headerLength + 1) & 0xFF);
                        int dstPort = ((packet.get(headerLength + 2) & 0xFF) << 8)
                                    | (packet.get(headerLength + 3) & 0xFF);

                        if (dstPort == 53 || srcPort == 53) {
                            // This is a DNS packet — extract query
                            String queriedDomain = extractDnsQuery(packet, udpHeader);
                            if (queriedDomain != null) {
                                if (filterEngine.shouldBlock(queriedDomain)) {
                                    totalBlocked.incrementAndGet();
                                    Log.d(TAG, "BLOCKED: " + queriedDomain);
                                    // Drop this packet (don't write to output)
                                    // Send back NXDOMAIN response
                                    sendDnsBlockResponse(packet, length, out);
                                    continue;
                                } else {
                                    totalAllowed.incrementAndGet();
                                }
                            }
                        }
                    }
                }

                // Forward allowed packets
                out.write(packet.array(), 0, length);

            } catch (IOException e) {
                if (isRunning.get()) {
                    Log.e(TAG, "I/O error in VPN loop", e);
                }
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in VPN loop", e);
            }
        }

        Log.i(TAG, "VPN loop ended. Blocked: " + totalBlocked.get()
              + ", Allowed: " + totalAllowed.get());
    }

    /**
     * Extract the queried domain name from a DNS query packet.
     */
    private String extractDnsQuery(ByteBuffer packet, int dnsOffset) {
        try {
            StringBuilder domain = new StringBuilder();
            int pos = dnsOffset + 12; // Skip DNS header (ID, flags, counts)
            int maxLen = packet.limit();

            while (pos < maxLen) {
                int labelLen = packet.get(pos) & 0xFF;
                if (labelLen == 0) {
                    pos++;
                    break;
                }
                // Compression pointer check
                if ((labelLen & 0xC0) == 0xC0) {
                    break;
                }
                pos++;
                if (pos + labelLen > maxLen) break;

                if (domain.length() > 0) domain.append(".");
                byte[] label = new byte[labelLen];
                packet.get(label, 0, labelLen);
                domain.append(new String(label, "US-ASCII"));
                pos += labelLen;
            }

            return domain.length() > 0 ? domain.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Send a DNS NXDOMAIN response to block the ad domain.
     */
    private void sendDnsBlockResponse(ByteBuffer originalPacket, int length, FileOutputStream out) {
        try {
            byte[] response = new byte[length];
            originalPacket.rewind();
            originalPacket.get(response, 0, length);

            // Set QR bit (response) and RCODE=3 (NXDOMAIN)
            // DNS header byte 2 (flags high): set QR=1
            int dnsOffset = (response[0] & 0x0F) * 4;
            if (dnsOffset + 12 <= length) {
                response[dnsOffset + 2] = (byte) 0x81; // QR=1, Opcode=0, AA=0, TC=0, RD=1
                response[dnsOffset + 3] = (byte) 0x83; // RA=1, RCODE=3 (NXDOMAIN)

                // Zero out answer/auth/additional counts
                response[dnsOffset + 6] = 0;
                response[dnsOffset + 7] = 0;
                response[dnsOffset + 8] = 0;
                response[dnsOffset + 9] = 0;
                response[dnsOffset + 10] = 0;
                response[dnsOffset + 11] = 0;

                // Swap source and destination ports
                int srcPortOffset = dnsOffset - 2; // UDP src port in IP packet
                int dstPortOffset = dnsOffset;     // UDP dst port in IP packet

                byte tmp = response[srcPortOffset];
                response[srcPortOffset] = response[dstPortOffset];
                response[dstPortOffset] = tmp;
                tmp = response[srcPortOffset + 1];
                response[srcPortOffset + 1] = response[dstPortOffset + 1];
                response[dstPortOffset + 1] = tmp;

                // Swap IP src and dst addresses
                int ipSrcOffset = 12;
                int ipDstOffset = 16;
                for (int i = 0; i < 4; i++) {
                    tmp = response[ipSrcOffset + i];
                    response[ipSrcOffset + i] = response[ipDstOffset + i];
                    response[ipDstOffset + i] = tmp;
                }

                out.write(response, 0, length);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send block response", e);
        }
    }

    private Notification createNotification() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent mainPending = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String statsText = "Blocked: " + totalBlocked.get()
                         + " | Allowed: " + totalAllowed.get()
                         + " | Rules: " + filterEngine.getTotalRules();

        return new NotificationCompat.Builder(this, AnihiliusApp.VPN_CHANNEL_ID)
                .setContentTitle("🛡️ AnihiliusAdBlocker Active")
                .setContentText(statsText)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentIntent(mainPending)
                .setOngoing(true)
                .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    public void stopVpn() {
        isRunning.set(false);
        if (vpnThread != null) {
            vpnThread.interrupt();
        }
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing VPN interface", e);
        }
        stopForeground(true);
        stopSelf();
        Log.i(TAG, "VPN stopped");
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    @Override
    public void onRevoke() {
        stopVpn();
        super.onRevoke();
    }

    // Public getters for stats
    public static long getTotalBlocked() { return totalBlocked.get(); }
    public static long getTotalAllowed() { return totalAllowed.get(); }
    public static long getTotalBytes() { return totalBytes.get(); }
    public boolean isActive() { return isRunning.get(); }
    public FilterEngine getFilterEngine() { return filterEngine; }
}
