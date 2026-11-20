package com.aegisfocus.app.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aegisfocus.app.core.di.ServiceLocator
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
                    container.evaluateScheduleTriggerUseCase.getMatchingSchedulesForNow().firstOrNull()
                }

                if (schedule != null && schedule.isEnabled) {
                    // Starts the routine for the time left in its window (skips if something is running)
                    container.scheduleCoordinator.startIfInsideWindow(schedule)
                    // Schedule next alarm for this routine (looping across days)
                    ScheduleAlarmScheduler.scheduleNextAlarm(context, schedule)
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
