package com.aegisfocus.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.aegisfocus.app.core.di.ServiceLocator

class AegisAccessibilityBlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val className = event.className?.toString() ?: ""

            // Avoid triggering self or system transient system UI dialogs
            if (packageName == applicationContext.packageName) return
            if (className.contains("InputMethod") || className.contains("VolumeDialog")) return

            ServiceLocator.getContainer(applicationContext)
                .foregroundAppMonitor
                .onAccessibilityEvent(packageName)
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
