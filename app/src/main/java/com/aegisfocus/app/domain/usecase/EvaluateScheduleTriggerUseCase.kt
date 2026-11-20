package com.aegisfocus.app.domain.usecase

import com.aegisfocus.app.domain.model.Schedule
import com.aegisfocus.app.domain.repository.ScheduleRepository

class EvaluateScheduleTriggerUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    suspend fun getMatchingSchedulesForNow(
        currentTimeMs: Long = System.currentTimeMillis()
    ): List<Schedule> {
        return scheduleRepository.getEnabledSchedules().filter { it.isActiveAt(currentTimeMs) }
    }
}
