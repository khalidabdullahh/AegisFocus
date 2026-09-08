package com.aegisfocus.app.domain.repository

import com.aegisfocus.app.domain.model.EmergencyExitRecord
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

interface FocusSessionRepository {
    fun getActiveSessionFlow(): Flow<FocusSession?>
    suspend fun getActiveSession(): FocusSession?
    fun getAllSessionsFlow(): Flow<List<FocusSession>>
    fun getCompletedSessionCountFlow(): Flow<Int>
    suspend fun getTodaySessions(): List<FocusSession>
    suspend fun startSession(session: FocusSession): Long
    suspend fun updateSession(session: FocusSession)
    suspend fun updateSessionStatus(sessionId: Long, status: SessionStatus)
    suspend fun recordIntervention(sessionId: Long)
    suspend fun recordEmergencyExit(record: EmergencyExitRecord): Long
    fun getEmergencyExitsFlow(): Flow<List<EmergencyExitRecord>>
}
