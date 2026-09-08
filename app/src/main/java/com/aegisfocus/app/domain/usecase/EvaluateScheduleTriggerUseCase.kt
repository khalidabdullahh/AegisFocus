package com.aegisfocus.app.domain.usecase

import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.domain.model.Schedule
import com.aegisfocus.app.domain.repository.ScheduleRepository
import java.util.Calendar

class EvaluateScheduleTriggerUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    suspend fun getMatchingSchedulesForNow(
        currentTimeMs: Long = System.currentTimeMillis()
    ): List<Schedule> {
        val enabledSchedules = scheduleRepository.getEnabledSchedules()
        if (enabledSchedules.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMs
        }
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        return enabledSchedules.filter { schedule ->
            if (!TimeUtils.isDaySelected(schedule.daysOfWeekBitmask, currentDayOfWeek)) {
                return@filter false
            }
            val startTotalMinutes = schedule.startHour * 60 + schedule.startMinute
            val endTotalMinutes = schedule.endHour * 60 + schedule.endMinute

            if (startTotalMinutes <= endTotalMinutes) {
                currentTotalMinutes in startTotalMinutes until endTotalMinutes
            } else {
                // Crosses midnight, e.g. 23:00 to 07:00
                currentTotalMinutes >= startTotalMinutes || currentTotalMinutes < endTotalMinutes
            }
        }
    }
}
