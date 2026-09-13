package com.aegisfocus.app.domain.usecase

import com.aegisfocus.app.domain.model.EmergencyExitRecord
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.repository.FocusSessionRepository

class EmergencyExitUseCase(
    private val sessionRepository: FocusSessionRepository
) {
    suspend operator fun invoke(sessionId: Long, reasonNote: String? = null): Result<EmergencyExitRecord> {
        val session = sessionRepository.getActiveSession()
            ?: return Result.failure(IllegalStateException("No active focus session found"))

        if (session.mode == SessionMode.STRICT && !session.isExpired) {
            return Result.failure(IllegalStateException("Emergency Exit is completely disabled in Strict Mode."))
        }

        val now = System.currentTimeMillis()
        val remainingMinutes = ((session.endEpochMs - now) / 60000).toInt().coerceAtLeast(0)

        val record = EmergencyExitRecord(
            sessionId = session.id,
            timestampEpochMs = now,
            sessionDurationMinutes = session.durationMinutes,
            remainingMinutesAtExit = remainingMinutes,
            reasonNote = reasonNote
        )

        sessionRepository.recordEmergencyExit(record)
        sessionRepository.updateSessionStatus(session.id, SessionStatus.EMERGENCY_EXITED)

        return Result.success(record)
    }
}
