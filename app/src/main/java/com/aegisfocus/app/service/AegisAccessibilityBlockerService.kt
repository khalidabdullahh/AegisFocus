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

            container.foregroundAppMonitor.onAccessibilityEvent(packageName)
        }
    }

    override fun onDestroy() {
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
}
