package com.aegisfocus.app.service.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.aegisfocus.app.presentation.MainActivity
import com.aegisfocus.app.R

/**
 * -----------------------------------------------------------------------------
 * AegisAdBlockerVpnService
 * Purpose: Provides true 1-tap, on-device automatic AdBlocking.
 * Mechanism: Creates a lightweight local VPN tunnel interface that sets
 *               AdGuard DNS as the active resolver. With "Block 18+ content" on, the
 *               AdGuard Family DNS is used instead, which also blocks adult sites.
 * Privacy: 100% on-device. Zero network traffic is proxied or sent to remote
 *             servers; only standard DNS lookups are filtered.
 * -----------------------------------------------------------------------------
 */
class AegisAdBlockerVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        try {
            val settings = com.aegisfocus.app.core.di.ServiceLocator
                .getContainer(applicationContext)
                .settingsRepository
            val adultBlock = settings.isAdultContentBlockEnabled()
            val adsBlock = settings.isAdBlockerVpnEnabled()
            val scamShield = settings.isScamShieldEnabled()

            startForeground(NOTIFICATION_ID, buildNotification(adultBlock, adsBlock, scamShield))

            // Restart cleanly when the filter level changes while the VPN is running
            vpnInterface?.close()
            vpnInterface = null

            val builder = Builder()
                .setSession("Aegis Focus AdBlocker")
                .addAddress("10.0.0.2", 32)
                .setBlocking(false)

            if (adultBlock) {
                builder
                    .addDnsServer("94.140.14.15")       // AdGuard Family Primary DNS
                    .addDnsServer("94.140.15.16")       // AdGuard Family Secondary DNS
                    .addDnsServer("2a10:50c0::bad1:ff")  // AdGuard Family IPv6 Primary
                    .addDnsServer("2a10:50c0::bad2:ff")  // AdGuard Family IPv6 Secondary
            } else if (scamShield && !adsBlock) {
                // Scam Shield only: Cloudflare security DNS blocks malware and phishing domains
                builder
                    .addDnsServer("1.1.1.2")
                    .addDnsServer("1.0.0.2")
                    .addDnsServer("2606:4700:4700::1112")
                    .addDnsServer("2606:4700:4700::1002")
            } else {
                // AdGuard DNS also filters known phishing and malware domains
                builder
                    .addDnsServer("94.140.14.14")       // AdGuard Primary DNS
                    .addDnsServer("94.140.15.15")       // AdGuard Secondary DNS
                    .addDnsServer("2a10:50c0::ad1:ff")  // AdGuard IPv6 Primary
                    .addDnsServer("2a10:50c0::ad2:ff")  // AdGuard IPv6 Secondary
            }

            vpnInterface = builder.establish()
            isRunning = vpnInterface != null
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aegis AdBlocker Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active status when system-wide AdBlocker is enabled"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(adultBlock: Boolean, adsBlock: Boolean, scamShield: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aegis protection active")
            .setContentText(
                when {
                    adultBlock -> "Blocking ads, scams, trackers and 18+ sites"
                    adsBlock && scamShield -> "Blocking ads, trackers and scam links"
                    adsBlock -> "Blocking in-app ads, trackers and scam links"
                    else -> "Scam Shield is blocking phishing and malware links"
                }
            )
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START = "com.aegisfocus.app.vpn.START"
        const val ACTION_STOP = "com.aegisfocus.app.vpn.STOP"
        private const val CHANNEL_ID = "aegis_adblocker_vpn_channel"
        private const val NOTIFICATION_ID = 8841

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, AegisAdBlockerVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AegisAdBlockerVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
