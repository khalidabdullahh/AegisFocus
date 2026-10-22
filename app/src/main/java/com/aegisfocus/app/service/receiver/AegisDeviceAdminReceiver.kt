package com.aegisfocus.app.service.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.domain.model.SessionMode

/**
 * Device Administrator Receiver for Anti-Uninstall & Tamper Protection.
 * When Strict Mode is active, deactivation is blocked/warned to preserve discipline integrity.
 */
class AegisDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "ASR Anti-Uninstall Protection: Activated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "ASR Anti-Uninstall Protection: Deactivated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        val container = ServiceLocator.getContainer(context)
        val activeSession = container.focusSessionManager.activeSession.value

        return if (activeSession != null && activeSession.mode == SessionMode.STRICT && !activeSession.isExpired) {
            "STRICT MODE ACTIVE: ASR is currently protecting your focus session. Deactivating device admin or uninstalling the app is prevented until the session ends."
        } else {
            "Deactivating Device Administrator will disable ASR anti-uninstall protection."
        }
    }
}
