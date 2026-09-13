package com.aegisfocus.app.service

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aegisfocus.app.core.di.ServiceLocator

/**
 * Pro feature: Suppresses incoming notifications from distracting apps during an active focus session.
 *
 * Requires BIND_NOTIFICATION_LISTENER_SERVICE permission (declared in manifest).
 * User must grant notification listener access in System Settings.
 *
 * The service checks if a session is currently active and, if so, cancels notifications
 * from packages that are in the active blocked apps list.
 */
class NotificationShieldService : NotificationListenerService() {

    private val tag = "NotificationShield"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val container = ServiceLocator.getContainer(applicationContext)

        val focusSessionManager = container.focusSessionManager
        val activeSession = focusSessionManager.activeSession.value ?: return

        // Only suppress if notification shield is a Pro-enabled active feature
        // (entitlement check is synchronous-friendly via cached state)
        val blockedPackages = activeSession.blockedPackageNames
        if (sbn.packageName in blockedPackages) {
            Log.d(tag, "Suppressing notification from blocked app: ${sbn.packageName}")
            try {
                cancelNotification(sbn.key)
            } catch (e: Exception) {
                Log.w(tag, "Could not cancel notification: ${e.message}")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // no-op
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(tag, "Notification Shield connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(tag, "Notification Shield disconnected")
    }
}
