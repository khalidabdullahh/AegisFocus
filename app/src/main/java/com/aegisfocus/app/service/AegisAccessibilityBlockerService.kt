package com.aegisfocus.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.aegisfocus.app.core.di.ServiceLocator

class AegisAccessibilityBlockerService : AccessibilityService() {

    private var lastGateLaunchMs = 0L
    private var lastShortsCheckMs = 0L
    private var lastBackMs = 0L
    private var lastAdultCheckMs = 0L
    private var nsfwBlur: NsfwBlurController? = null
    private var appLimit: AppLimitController? = null
    private var youtubeIntent: YouTubeIntentController? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        // 0. 18+ visual blur: track which app is in the foreground
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            !(className.contains("InputMethod") || className.contains("VolumeDialog"))
        ) {
            val settings = ServiceLocator.getContainer(applicationContext).settingsRepository
            if (settings.isNsfwBlurEnabled() || nsfwBlur != null) {
                val blur = nsfwBlur ?: NsfwBlurController(this) {
                    ServiceLocator.getContainer(applicationContext).settingsRepository.isNsfwBlurEnabled()
                }.also { nsfwBlur = it }
                blur.onForegroundChanged(packageName)
            }
        }

        // Avoid triggering self or system transient system UI dialogs
        if (packageName == applicationContext.packageName) return
        if (className.contains("InputMethod") || className.contains("VolumeDialog")) return

        val container = com.aegisfocus.app.core.di.ServiceLocator.getContainer(applicationContext)

        // 1. Surgical Shorts & Reels Blocker
        if (com.aegisfocus.app.domain.blocker.ShortsBlockerEngine.isSupportedPackage(packageName) &&
            container.settingsRepository.isShortsBlockerEnabled()
        ) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastShortsCheckMs >= 250L && now - lastBackMs > 900L) {
                lastShortsCheckMs = now
                if (com.aegisfocus.app.domain.blocker.ShortsBlockerEngine
                        .isShortsOrReelsActive(packageName, rootInActiveWindow)
                ) {
                    lastBackMs = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }
            }
        }

        // 1c. YouTube Intent Mode: only videos that match the user's own topics
        if (packageName == "com.google.android.youtube" && container.intentStore.isEnabled()) {
            val intentController = youtubeIntent ?: YouTubeIntentController(this, container.intentStore)
                .also { youtubeIntent = it }
            if (intentController.check(rootInActiveWindow)) return
        }

        // 1b. Adult content second layer (browser address bar), complements the DNS filter
        if (com.aegisfocus.app.domain.blocker.ShortsBlockerEngine.isBrowserPackage(packageName) &&
            container.settingsRepository.isAdultContentBlockEnabled()
        ) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastAdultCheckMs >= 400L && now - lastBackMs > 900L) {
                lastAdultCheckMs = now
                if (com.aegisfocus.app.domain.blocker.AdultContentGuard
                        .isAdultPageActive(packageName, rootInActiveWindow)
                ) {
                    lastBackMs = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }
            }
        }

        // 2. Full App Blocking Detection
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val blockedBySession = container.blockingRuleRepository
                .isBlocked(packageName, applicationContext.packageName)

            // 3. Mindful Gate (a focus-session block always takes priority)
            if (!blockedBySession && container.mindfulGateStore.gateRequiredFor(packageName) != null) {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastGateLaunchMs > 1500L) {
                    lastGateLaunchMs = now
                    val gateIntent = android.content.Intent(
                        applicationContext,
                        com.aegisfocus.app.presentation.mindful.MindfulGateActivity::class.java
                    ).apply {
                        putExtra(
                            com.aegisfocus.app.presentation.mindful.MindfulGateActivity.EXTRA_PACKAGE,
                            packageName
                        )
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(gateIntent)
                }
                return
            }

            // 4. Per-app daily limits (time and number of opens)
            if (!blockedBySession) {
                val limits = appLimit ?: AppLimitController(
                    applicationContext,
                    container.appLimitStore,
                    container.appUsageTracker
                ).also { appLimit = it }
                if (limits.onForeground(packageName)) return
            }

            container.foregroundAppMonitor.onAccessibilityEvent(packageName)
        }
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        postProtectionOffAlert()
        return super.onUnbind(intent)
    }

    /** Tells the user when the accessibility service (all blocking) has been switched off. */
    private fun postProtectionOffAlert() {
        try {
            val manager = getSystemService(android.app.NotificationManager::class.java) ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    android.app.NotificationChannel(
                        ALERT_CHANNEL_ID,
                        "Protection alerts",
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    )
                )
            }
            val open = android.app.PendingIntent.getActivity(
                this,
                0,
                android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = androidx.core.app.NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(com.aegisfocus.app.R.drawable.ic_aegis_shield)
                .setContentTitle("Aegis protection is off")
                .setContentText("The accessibility service was turned off, so blocking and limits stopped. Tap to turn it back on.")
                .setStyle(
                    androidx.core.app.NotificationCompat.BigTextStyle()
                        .bigText("The accessibility service was turned off, so blocking and limits stopped. Tap to turn it back on.")
                )
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .build()
            manager.notify(ALERT_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // notifications may be disabled; nothing else to do
        }
    }

    override fun onDestroy() {
        appLimit?.release()
        appLimit = null
        nsfwBlur?.release()
        nsfwBlur = null
        super.onDestroy()
    }

    override fun onInterrupt() {
        // Required callback for accessibility service
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Refresh permissions state in settings repository
        ServiceLocator.getContainer(applicationContext)
            .settingsRepository
            .refreshPermissionState()
    }

    companion object {
        private const val ALERT_CHANNEL_ID = "aegis_protection_alerts"
        private const val ALERT_NOTIFICATION_ID = 4101
    }
}
