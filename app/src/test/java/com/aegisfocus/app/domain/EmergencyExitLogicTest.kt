package com.aegisfocus.app.domain

import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.usecase.EmergencyExitUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EmergencyExitLogicTest {

    private lateinit var fakeSessionRepository: FakeFocusSessionRepository
    private lateinit var emergencyExitUseCase: EmergencyExitUseCase

    @Before
    fun setup() {
        fakeSessionRepository = FakeFocusSessionRepository()
        emergencyExitUseCase = EmergencyExitUseCase(fakeSessionRepository)
    }

    @Test
    fun emergencyExit_transitionsSessionToEmergencyExited_andRecordsEvent() = runBlocking {
        val now = System.currentTimeMillis()
        val session = FocusSession(
            id = 20,
            name = "Strict Study",
            startEpochMs = now,
            endEpochMs = now + 15 * 60 * 1000L,
            durationMinutes = 25,
            mode = SessionMode.STRICT,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = listOf("com.instagram.android")
        )
        fakeSessionRepository.activeSession = session

        val result = emergencyExitUseCase(sessionId = 20, reasonNote = "Urgent family call")

        assertTrue(result.isSuccess)
        val record = result.getOrNull()
        assertEquals(20L, record?.sessionId)
        assertEquals("Urgent family call", record?.reasonNote)
        assertEquals(25, record?.sessionDurationMinutes)

        // Verify session status updated
        assertEquals(SessionStatus.EMERGENCY_EXITED, fakeSessionRepository.activeSession?.status)
        assertEquals(1, fakeSessionRepository.emergencyExits.size)
    }
}
