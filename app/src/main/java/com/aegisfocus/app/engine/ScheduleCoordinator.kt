package com.aegisfocus.app.engine

import android.content.Context
import android.util.Log
import com.aegisfocus.app.domain.model.Schedule
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.repository.FocusSessionRepository
import com.aegisfocus.app.domain.repository.ScheduleRepository
import com.aegisfocus.app.service.receiver.ScheduleAlarmScheduler

/**
 * Keeps Focus Routines running. A routine must (1) have its next start alarm set whenever it is
 * created, edited, switched on, or the app/phone restarts, and (2) start right away if the user
 * saves it while its time window is already open. The session only lasts for the time left in the
 * window, so it ends when the routine ends.
 */
class ScheduleCoordinator(
    private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val focusSessionManager: FocusSessionManager
) {

    /** Call after a routine is saved or switched on/off. */
    suspend fun onRoutineChanged(schedule: Schedule) {
        if (!schedule.isEnabled) {
            ScheduleAlarmScheduler.cancelAlarm(context, schedule.id)
            return
        }
        ScheduleAlarmScheduler.scheduleNextAlarm(context, schedule)
        startIfInsideWindow(schedule)
    }

    fun onRoutineDeleted(schedule: Schedule) {
        ScheduleAlarmScheduler.cancelAlarm(context, schedule.id)
    }

    /** Re-arms every enabled routine and starts the one that should be running now. */
    suspend fun syncAll(nowMs: Long = System.currentTimeMillis()) {
        trimRoutineSessionToWindow()
        val enabled = scheduleRepository.getEnabledSchedules()
        enabled.forEach { ScheduleAlarmScheduler.scheduleNextAlarm(context, it) }
        // If two routines overlap, the strict one wins.
        val due = enabled
            .filter { it.isActiveAt(nowMs) }
            .sortedByDescending { it.isStrictMode }
            .firstOrNull() ?: return
        startIfInsideWindow(due, nowMs)
    }

    /**
     * A routine's session must end when its window ends. Older versions started routine sessions with
     * the routine's full length, so a routine begun mid-window could run for hours past its end time
     * (Strict Mode cannot be cancelled). If the running session belongs to a routine and outlasts that
     * routine's window, cut it back to the window end. Returns true when it was changed.
     */
    suspend fun trimRoutineSessionToWindow(): Boolean {
        val active = focusSessionRepository.getActiveSession() ?: return false
        if (active.status != SessionStatus.ACTIVE) return false

        val windowEnd = scheduleRepository.getEnabledSchedules()
            .filter { it.name.trim() == active.name && it.isStrictMode == active.isStrictMode }
            .mapNotNull { it.windowEndFor(active.startEpochMs) }
            .minOrNull() ?: return false

        // one minute of slack for rounding
        if (active.endEpochMs <= windowEnd + 60_000L) return false

        val minutes = ((windowEnd - active.startEpochMs) / 60_000L).toInt().coerceAtLeast(1)
        focusSessionRepository.updateSession(active.copy(endEpochMs = windowEnd, durationMinutes = minutes))
        // Reloads the session: completes it right away if the window is already over, else restarts the timer
        focusSessionManager.restoreActiveSession()
        Log.i("ScheduleCoordinator", "Trimmed routine session \"${active.name}\" to its window end")
        return true
    }

    /**
     * Starts [schedule] as a focus session if its window is open and nothing else is running.
     * Returns true when a session was started.
     */
    suspend fun startIfInsideWindow(
        schedule: Schedule,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val minutesLeft = schedule.remainingMinutesAt(nowMs)
        if (minutesLeft <= 0) return false

        val active = focusSessionRepository.getActiveSession()
        if (active != null && !active.isExpired && active.status == SessionStatus.ACTIVE) return false

        val result = focusSessionManager.startSession(
            name = schedule.name,
            durationMinutes = minutesLeft,
            mode = if (schedule.isStrictMode) SessionMode.STRICT else SessionMode.NORMAL,
            blockedPackageNames = schedule.blockedPackageNames,
            profileId = schedule.profileId
        )
        result.exceptionOrNull()?.let { Log.w("ScheduleCoordinator", "Routine \"${schedule.name}\" did not start", it) }
        return result.isSuccess
    }
}
