package com.aegisfocus.app.domain

import com.aegisfocus.app.domain.model.EmergencyExitRecord
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.model.UsageStatsSummary
import com.aegisfocus.app.domain.repository.FocusSessionRepository
import com.aegisfocus.app.domain.repository.UsageStatsRepository
import com.aegisfocus.app.domain.usecase.EndFocusSessionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StrictModeStateMachineTest {

    private lateinit var fakeSessionRepository: FakeFocusSessionRepository
    private lateinit var fakeUsageStatsRepository: FakeUsageStatsRepository
    private lateinit var endFocusSessionUseCase: EndFocusSessionUseCase

    @Before
    fun setup() {
        fakeSessionRepository = FakeFocusSessionRepository()
        fakeUsageStatsRepository = FakeUsageStatsRepository()
        endFocusSessionUseCase = EndFocusSessionUseCase(fakeSessionRepository, fakeUsageStatsRepository)
    }

    @Test
    fun strictModeSession_rejectsNormalCancellation_whenActive() = runBlocking {
        val now = System.currentTimeMillis()
        val strictSession = FocusSession(
            id = 10,
            name = "Strict Focus",
            startEpochMs = now,
            endEpochMs = now + 1_000_000L,
            durationMinutes = 50,
            mode = SessionMode.STRICT,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = listOf("com.youtube")
        )
        fakeSessionRepository.activeSession = strictSession

        val result = endFocusSessionUseCase(sessionId = 10, isNormalCancel = true)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Strict Mode") == true)
        // Ensure session remains active
        assertTrue(fakeSessionRepository.activeSession?.status == SessionStatus.ACTIVE)
    }

    @Test
    fun normalModeSession_allowsNormalCancellation_whenActive() = runBlocking {
        val now = System.currentTimeMillis()
        val normalSession = FocusSession(
            id = 11,
            name = "Normal Focus",
            startEpochMs = now,
            endEpochMs = now + 1_000_000L,
            durationMinutes = 25,
            mode = SessionMode.NORMAL,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = listOf("com.youtube")
        )
        fakeSessionRepository.activeSession = normalSession

        val result = endFocusSessionUseCase(sessionId = 11, isNormalCancel = true)

        assertTrue(result.isSuccess)
        assertTrue(fakeSessionRepository.activeSession?.status == SessionStatus.CANCELLED)
    }

    @Test
    fun strictModeSession_allowsNaturalCompletion_whenExpired() = runBlocking {
        val now = System.currentTimeMillis()
        val expiredStrictSession = FocusSession(
            id = 12,
            name = "Strict Focus Expired",
            startEpochMs = now - 60_000L,
            endEpochMs = now - 10L,
            durationMinutes = 25,
            mode = SessionMode.STRICT,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = listOf("com.youtube")
        )
        fakeSessionRepository.activeSession = expiredStrictSession

        val result = endFocusSessionUseCase(sessionId = 12, isNormalCancel = false)

        assertTrue(result.isSuccess)
        assertTrue(fakeSessionRepository.activeSession?.status == SessionStatus.COMPLETED)
    }
}

// Minimal In-Memory Repository Fakes for Domain Testing
class FakeFocusSessionRepository : FocusSessionRepository {
    var activeSession: FocusSession? = null
    val emergencyExits = mutableListOf<EmergencyExitRecord>()

    override fun getActiveSessionFlow(): Flow<FocusSession?> = flowOf(activeSession)
    override suspend fun getActiveSession(): FocusSession? = activeSession
    override fun getAllSessionsFlow(): Flow<List<FocusSession>> = flowOf(emptyList())
    override fun getCompletedSessionCountFlow(): Flow<Int> = flowOf(0)
    override suspend fun getTodaySessions(): List<FocusSession> = emptyList()
    override suspend fun startSession(session: FocusSession): Long {
        activeSession = session
        return session.id
    }
    override suspend fun updateSession(session: FocusSession) {
        activeSession = session
    }
    override suspend fun updateSessionStatus(sessionId: Long, status: SessionStatus) {
        activeSession = activeSession?.copy(status = status)
    }
    override suspend fun recordIntervention(sessionId: Long) {
        activeSession = activeSession?.copy(totalInterventions = (activeSession?.totalInterventions ?: 0) + 1)
    }
    override suspend fun recordEmergencyExit(record: EmergencyExitRecord): Long {
        emergencyExits.add(record)
        return record.id
    }
    override fun getEmergencyExitsFlow(): Flow<List<EmergencyExitRecord>> = flowOf(emergencyExits)
}

class FakeUsageStatsRepository : UsageStatsRepository {
    var completedDurationLogged = 0
    override suspend fun getTodayUsageSummary(): UsageStatsSummary = UsageStatsSummary(date = "2026-09-08")
    override suspend fun getWeeklyUsageHistory(): List<UsageStatsSummary> = emptyList()
    override fun getDailyUsageFlow(): Flow<UsageStatsSummary> = flowOf(UsageStatsSummary(date = "2026-09-08"))
    override suspend fun recordFocusSessionCompleted(durationMinutes: Int) {
        completedDurationLogged += durationMinutes
    }
}
