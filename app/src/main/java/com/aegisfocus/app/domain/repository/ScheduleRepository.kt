package com.aegisfocus.app.domain.repository

import com.aegisfocus.app.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun getAllSchedulesFlow(): Flow<List<Schedule>>
    suspend fun getEnabledSchedules(): List<Schedule>
    suspend fun getScheduleById(id: Long): Schedule?
    suspend fun saveSchedule(schedule: Schedule): Long
    suspend fun setScheduleEnabled(id: Long, isEnabled: Boolean)
    suspend fun deleteSchedule(schedule: Schedule)
}
