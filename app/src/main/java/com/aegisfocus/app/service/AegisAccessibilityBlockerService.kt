package com.aegisfocus.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.aegisfocus.app.core.di.ServiceLocator

class AegisAccessibilityBlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        // Avoid triggering self or system transient system UI dialogs
        if (packageName == applicationContext.packageName) return
        if (className.contains("InputMethod") || className.contains("VolumeDialog")) return

        val container = com.aegisfocus.app.core.di.ServiceLocator.getContainer(applicationContext)

        // 1. Surgical Shorts & Reels Blocker
        if (container.settingsRepository.isShortsBlockerEnabled()) {
            if (packageName.contains("youtube") || packageName.contains("instagram") || packageName.contains("facebook")) {
                val root = rootInActiveWindow
                if (root != null && com.aegisfocus.app.domain.blocker.ShortsBlockerEngine.isShortsOrReelsActive(packageName, root)) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }
            }
        }

        // 2. Full App Blocking Detection
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            container.foregroundAppMonitor.onAccessibilityEvent(packageName)
        }
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
