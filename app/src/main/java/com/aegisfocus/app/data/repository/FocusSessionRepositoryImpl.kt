package com.aegisfocus.app.data.repository

import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.data.local.dao.EmergencyExitDao
import com.aegisfocus.app.data.local.dao.FocusSessionDao
import com.aegisfocus.app.data.local.entity.EmergencyExitEntity
import com.aegisfocus.app.data.local.entity.FocusSessionEntity
import com.aegisfocus.app.domain.model.EmergencyExitRecord
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.repository.FocusSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FocusSessionRepositoryImpl(
    private val sessionDao: FocusSessionDao,
    private val emergencyExitDao: EmergencyExitDao
) : FocusSessionRepository {

    override fun getActiveSessionFlow(): Flow<FocusSession?> {
        return sessionDao.getActiveSessionFlow().map { it?.toDomain() }
    }

    override suspend fun getActiveSession(): FocusSession? {
        return sessionDao.getActiveSession()?.toDomain()
    }

    override fun getAllSessionsFlow(): Flow<List<FocusSession>> {
        return sessionDao.getAllSessionsFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getCompletedSessionCountFlow(): Flow<Int> {
        return sessionDao.getCompletedSessionCountFlow()
    }

    override suspend fun getTodaySessions(): List<FocusSession> {
        val startOfDay = TimeUtils.getStartOfDay()
        val endOfDay = TimeUtils.getEndOfDay()
        return sessionDao.getSessionsInRange(startOfDay, endOfDay).map { it.toDomain() }
    }

    override suspend fun startSession(session: FocusSession): Long {
        return sessionDao.insertSession(FocusSessionEntity.fromDomain(session))
    }

    override suspend fun updateSession(session: FocusSession) {
        sessionDao.updateSession(FocusSessionEntity.fromDomain(session))
    }

    override suspend fun updateSessionStatus(sessionId: Long, status: SessionStatus) {
        sessionDao.updateSessionStatus(sessionId, status.name)
    }

    override suspend fun recordIntervention(sessionId: Long) {
        sessionDao.incrementIntervention(sessionId)
    }

    override suspend fun recordEmergencyExit(record: EmergencyExitRecord): Long {
        return emergencyExitDao.insertEmergencyExit(EmergencyExitEntity.fromDomain(record))
    }

    override fun getEmergencyExitsFlow(): Flow<List<EmergencyExitRecord>> {
        return emergencyExitDao.getAllExitsFlow().map { list -> list.map { it.toDomain() } }
    }
}
