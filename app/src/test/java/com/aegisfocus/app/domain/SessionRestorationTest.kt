package com.aegisfocus.app.domain

import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.usecase.RestoreActiveSessionUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SessionRestorationTest {

    private lateinit var fakeSessionRepository: FakeFocusSessionRepository
    private lateinit var fakeUsageStatsRepository: FakeUsageStatsRepository
    private lateinit var restoreActiveSessionUseCase: RestoreActiveSessionUseCase

    @Before
    fun setup() {
        fakeSessionRepository = FakeFocusSessionRepository()
        fakeUsageStatsRepository = FakeUsageStatsRepository()
        restoreActiveSessionUseCase = RestoreActiveSessionUseCase(fakeSessionRepository, fakeUsageStatsRepository)
    }

    @Test
    fun restoration_returnsSession_ifStillWithinTimeWindow() = runBlocking {
        val now = System.currentTimeMillis()
        val ongoingSession = FocusSession(
            id = 30,
            name = "Restored Session",
            startEpochMs = now - 5 * 60 * 1000L,
            endEpochMs = now + 20 * 60 * 1000L,
            durationMinutes = 25,
            mode = SessionMode.NORMAL,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = listOf("com.twitter.android")
        )
        fakeSessionRepository.activeSession = ongoingSession

        val restored = restoreActiveSessionUseCase()

        assertNotNull(restored)
        assertEquals(30L, restored?.id)
        assertEquals(SessionStatus.ACTIVE, fakeSessionRepository.activeSession?.status)
    }

    @Test
    fun restoration_marksCompletedAndReturnsNull_ifSessionExpiredWhileAppWasClosed() = runBlocking {
        val now = System.currentTimeMillis()
        val expiredSession = FocusSession(
            id = 31,
            name = "Expired While Closed",
            startEpochMs = now - 60 * 60 * 1000L,
            endEpochMs = now - 30 * 60 * 1000L,
            durationMinutes = 30,
            mode = SessionMode.STRICT,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = listOf("com.twitter.android")
        )
        fakeSessionRepository.activeSession = expiredSession

        val restored = restoreActiveSessionUseCase()

        assertNull(restored)
        assertEquals(SessionStatus.COMPLETED, fakeSessionRepository.activeSession?.status)
        assertEquals(30, fakeUsageStatsRepository.completedDurationLogged)
    }
}
