package com.aegisfocus.app.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aegisfocus.app.core.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val container = ServiceLocator.getContainer(context)
                    // 1. Restore active focus session if still valid
                    container.focusSessionManager.restoreActiveSession()

                    // 2. Reschedule enabled routines
                    val enabledSchedules = container.scheduleRepository.getEnabledSchedules()
                    // ScheduleAlarmReceiver can trigger checks or WorkManager can sync
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
