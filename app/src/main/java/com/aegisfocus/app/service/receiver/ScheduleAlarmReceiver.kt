package com.aegisfocus.app.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.domain.model.SessionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = ServiceLocator.getContainer(context)
                val schedule = if (scheduleId != -1L) {
                    container.scheduleRepository.getScheduleById(scheduleId)
                } else {
                    val matching = container.evaluateScheduleTriggerUseCase.getMatchingSchedulesForNow()
                    matching.firstOrNull()
                }

                if (schedule != null && schedule.isEnabled) {
                    val activeSession = container.focusSessionRepository.getActiveSession()
                    if (activeSession == null || activeSession.isExpired) {
                        // Automatically start the scheduled session
                        container.focusSessionManager.startSession(
                            name = schedule.name,
                            durationMinutes = schedule.durationMinutes,
                            mode = if (schedule.isStrictMode) SessionMode.STRICT else SessionMode.NORMAL,
                            blockedPackageNames = schedule.blockedPackageNames,
                            profileId = schedule.profileId
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    }
}
