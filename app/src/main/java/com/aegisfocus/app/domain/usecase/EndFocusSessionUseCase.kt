package com.aegisfocus.app.domain.usecase

import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.repository.FocusSessionRepository
import com.aegisfocus.app.domain.repository.UsageStatsRepository

class EndFocusSessionUseCase(
    private val sessionRepository: FocusSessionRepository,
    private val usageStatsRepository: UsageStatsRepository
) {
    suspend operator fun invoke(sessionId: Long, isNormalCancel: Boolean = false): Result<Unit> {
        val session = sessionRepository.getActiveSession() ?: return Result.failure(IllegalStateException("No active session found"))

        if (session.id != sessionId && sessionId != 0L) {
            return Result.failure(IllegalStateException("Session ID mismatch"))
        }

        if (session.mode == SessionMode.STRICT && !session.isExpired && isNormalCancel) {
            return Result.failure(IllegalStateException("Cannot cancel a Strict Mode session directly. Use Emergency Exit if necessary."))
        }

        val newStatus = if (session.isExpired) SessionStatus.COMPLETED else SessionStatus.CANCELLED
        sessionRepository.updateSessionStatus(session.id, newStatus)

        if (newStatus == SessionStatus.COMPLETED) {
            usageStatsRepository.recordFocusSessionCompleted(session.durationMinutes)
        }

        return Result.success(Unit)
    }
}
