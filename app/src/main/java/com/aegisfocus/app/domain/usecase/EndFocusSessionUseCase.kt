package com.aegisfocus.app.domain.usecase

import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.data.local.dao.DailyGoalDao
import com.aegisfocus.app.data.local.entity.DailyGoalEntity
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.repository.FocusSessionRepository
import com.aegisfocus.app.domain.repository.SettingsRepository
import com.aegisfocus.app.domain.repository.UsageStatsRepository

class EndFocusSessionUseCase(
    private val sessionRepository: FocusSessionRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val dailyGoalDao: DailyGoalDao? = null,
    private val settingsRepository: SettingsRepository? = null
) {
    suspend operator fun invoke(sessionId: Long, isNormalCancel: Boolean = false): Result<Unit> {
        val session = sessionRepository.getActiveSession() ?: return Result.failure(IllegalStateException("No active session found"))

        if (session.id != sessionId && sessionId != 0L) {
            return Result.failure(IllegalStateException("Session ID mismatch"))
        }

        if (session.mode == SessionMode.STRICT && !session.isExpired && isNormalCancel) {
            return Result.failure(IllegalStateException("Strict Mode sessions are unbreakable and cannot be ended early."))
        }

        val newStatus = if (session.isExpired) SessionStatus.COMPLETED else SessionStatus.CANCELLED
        sessionRepository.updateSessionStatus(session.id, newStatus)

        if (newStatus == SessionStatus.COMPLETED) {
            usageStatsRepository.recordFocusSessionCompleted(session.durationMinutes)
            if (dailyGoalDao != null) {
                try {
                    val todayKey = TimeUtils.getTodayDateString()
                    val targetMinutes = settingsRepository?.getDailyFocusGoalMinutes() ?: 120
                    val existing = dailyGoalDao.getGoalForDate(todayKey)
                    if (existing == null) {
                        dailyGoalDao.upsertGoal(
                            DailyGoalEntity(
                                dateKey = todayKey,
                                targetFocusMinutes = targetMinutes,
                                achievedFocusMinutes = session.durationMinutes,
                                achievedSessions = 1,
                                isGoalMet = session.durationMinutes >= targetMinutes
                            )
                        )
                    } else {
                        val newAchieved = existing.achievedFocusMinutes + session.durationMinutes
                        val newSessions = existing.achievedSessions + 1
                        val isMet = newAchieved >= existing.targetFocusMinutes
                        dailyGoalDao.upsertGoal(
                            existing.copy(
                                achievedFocusMinutes = newAchieved,
                                achievedSessions = newSessions,
                                isGoalMet = isMet,
                                updatedAtEpochMs = System.currentTimeMillis()
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        return Result.success(Unit)
    }
}
