package com.aegisfocus.app.domain.usecase

import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.repository.FocusSessionRepository
import com.aegisfocus.app.domain.repository.UsageStatsRepository

class RestoreActiveSessionUseCase(
    private val sessionRepository: FocusSessionRepository,
    private val usageStatsRepository: UsageStatsRepository
) {
    /**
     * Checks database for active session, compares current epoch time with endEpochMs.
     * If expired, marks completed. If still active, returns the valid active session.
     */
    suspend operator fun invoke(): FocusSession? {
        val session = sessionRepository.getActiveSession() ?: return null

        if (session.status != SessionStatus.ACTIVE) return null

        val now = System.currentTimeMillis()
        return if (now >= session.endEpochMs) {
            // Naturally expired while app was closed / killed
            sessionRepository.updateSessionStatus(session.id, SessionStatus.COMPLETED)
            usageStatsRepository.recordFocusSessionCompleted(session.durationMinutes)
            null
        } else {
            session
        }
    }
}
