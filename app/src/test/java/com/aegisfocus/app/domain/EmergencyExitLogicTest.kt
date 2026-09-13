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
    fun strictSession_rejectsEmergencyExit() = runBlocking {
        val now = System.currentTimeMillis()
        val strictSession = FocusSession(
            id = 20,
            name = "Strict Study",
            startEpochMs = now,
            endEpochMs = now + 15 * 60 * 1000L,
            durationMinutes = 25,
            mode = SessionMode.STRICT,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = listOf("com.instagram.android")
        )
        fakeSessionRepository.activeSession = strictSession

        val result = emergencyExitUseCase(sessionId = 20, reasonNote = "Attempt early exit")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("disabled in Strict Mode") == true)
        assertEquals(SessionStatus.ACTIVE, fakeSessionRepository.activeSession?.status)
    }

    @Test
    fun normalSession_allowsEmergencyExit() = runBlocking {
        val now = System.currentTimeMillis()
        val normalSession = FocusSession(
            id = 21,
            name = "Normal Study",
            startEpochMs = now,
            endEpochMs = now + 15 * 60 * 1000L,
            durationMinutes = 25,
            mode = SessionMode.NORMAL,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = listOf("com.instagram.android")
        )
        fakeSessionRepository.activeSession = normalSession

        val result = emergencyExitUseCase(sessionId = 21, reasonNote = "Urgent family call")

        assertTrue(result.isSuccess)
        val record = result.getOrNull()
        assertEquals(21L, record?.sessionId)
        assertEquals("Urgent family call", record?.reasonNote)
        assertEquals(SessionStatus.EMERGENCY_EXITED, fakeSessionRepository.activeSession?.status)
    }
}
