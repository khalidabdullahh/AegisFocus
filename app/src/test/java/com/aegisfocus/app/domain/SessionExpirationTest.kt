package com.aegisfocus.app.domain

import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExpirationTest {

    @Test
    fun session_isNotExpired_whenCurrentTimeIsBeforeEndEpoch() {
        val now = System.currentTimeMillis()
        val session = FocusSession(
            id = 1,
            name = "Deep Work",
            startEpochMs = now - 10_000L,
            endEpochMs = now + 50_000L,
            durationMinutes = 25,
            mode = SessionMode.NORMAL,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = listOf("com.instagram.android")
        )

        assertFalse(session.isExpired)
        assertTrue(session.remainingMillis > 0)
    }

    @Test
    fun session_isExpired_whenCurrentTimeIsPastEndEpoch() {
        val now = System.currentTimeMillis()
        val session = FocusSession(
            id = 1,
            name = "Deep Work",
            startEpochMs = now - 60_000L,
            endEpochMs = now - 1_000L,
            durationMinutes = 25,
            mode = SessionMode.NORMAL,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = listOf("com.instagram.android")
        )

        assertTrue(session.isExpired)
        assertEquals(0L, session.remainingMillis)
    }

    @Test
    fun progressFraction_isBoundedBetweenZeroAndOne() {
        val now = System.currentTimeMillis()

        // Just started
        val sessionStart = FocusSession(
            id = 1,
            name = "Test",
            startEpochMs = now,
            endEpochMs = now + 60_000L,
            durationMinutes = 1,
            mode = SessionMode.NORMAL,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = emptyList()
        )
        assertTrue(sessionStart.progressFraction in 0f..0.1f)

        // Expired
        val sessionPast = FocusSession(
            id = 2,
            name = "Test",
            startEpochMs = now - 120_000L,
            endEpochMs = now - 60_000L,
            durationMinutes = 1,
            mode = SessionMode.NORMAL,
            status = SessionStatus.ACTIVE,
            blockedPackageNames = emptyList()
        )
        assertEquals(1f, sessionPast.progressFraction, 0.01f)
    }
}
