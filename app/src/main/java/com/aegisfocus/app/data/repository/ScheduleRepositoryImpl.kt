package com.aegisfocus.app.data.repository

import com.aegisfocus.app.data.local.dao.ScheduleDao
import com.aegisfocus.app.data.local.entity.ScheduleEntity
import com.aegisfocus.app.domain.model.Schedule
import com.aegisfocus.app.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduleRepositoryImpl(
    private val scheduleDao: ScheduleDao
) : ScheduleRepository {

    override fun getAllSchedulesFlow(): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedulesFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getEnabledSchedules(): List<Schedule> {
        return scheduleDao.getEnabledSchedules().map { it.toDomain() }
    }

    override suspend fun getScheduleById(id: Long): Schedule? {
        return scheduleDao.getScheduleById(id)?.toDomain()
    }

    override suspend fun saveSchedule(schedule: Schedule): Long {
        return scheduleDao.insertSchedule(ScheduleEntity.fromDomain(schedule))
    }

    override suspend fun setScheduleEnabled(id: Long, isEnabled: Boolean) {
        scheduleDao.setScheduleEnabled(id, isEnabled)
    }

    override suspend fun deleteSchedule(schedule: Schedule) {
        scheduleDao.deleteSchedule(ScheduleEntity.fromDomain(schedule))
    }
}
