package com.aegisfocus.app.domain.analytics

import com.aegisfocus.app.data.local.dao.DailyGoalDao
import com.aegisfocus.app.data.local.dao.FocusEventDao
import com.aegisfocus.app.data.local.dao.UserXpDao
import java.time.LocalDate
import java.time.ZoneId

/**
 * Transparent Discipline Score formula (always documented, never opaque).
 *
 * Score = Base(80)
 *       + ConsistencyBonus   (up to +10, based on goal completion rate over last 7 days)
 *       + GoalBonus          (up to +5, for meeting today's goal)
 *       + StreakBonus        (up to +5, for active streak)
 *       - DistractionPenalty (up to -20, for block attempts in last 7 days)
 *       - EmergencyExitPenalty (up to -20, for emergency exits in last 7 days)
 *
 * Clamped to [10, 100].
 */
class DisciplineScoreCalculator(
    private val focusEventDao: FocusEventDao,
    private val dailyGoalDao: DailyGoalDao,
    private val userXpDao: UserXpDao
) {

    data class ScoreBreakdown(
        val total: Int,
        val base: Int = 80,
        val consistencyBonus: Int,
        val goalBonus: Int,
        val streakBonus: Int,
        val distractionPenalty: Int,
        val emergencyExitPenalty: Int
    )

    suspend fun calculateScore(): ScoreBreakdown {
        val nowMs = System.currentTimeMillis()
        val sevenDaysMs = nowMs - 7L * 24 * 60 * 60 * 1_000
        val todayKey = LocalDate.now(ZoneId.systemDefault()).toString()

        // Block attempts (0..100+) → penalty 0..20
        val blockAttempts = focusEventDao.countBlockAttemptsAfter(sevenDaysMs)
        val distractionPenalty = (blockAttempts * 2).coerceIn(0, 20)

        // Emergency exits (0..10+) → penalty 0..20
        val emergencyExits = focusEventDao.countEmergencyExitsAfter(sevenDaysMs)
        val exitPenalty = (emergencyExits * 4).coerceIn(0, 20)

        // Streak bonus
        val xp = userXpDao.getXp()
        val streak = xp?.currentStreak ?: 0
        val streakBonus = when {
            streak >= 14 -> 5
            streak >= 7  -> 3
            streak >= 3  -> 2
            streak >= 1  -> 1
            else         -> 0
        }

        // Today's goal bonus
        val todayGoal = dailyGoalDao.getGoalForDate(todayKey)
        val goalBonus = if (todayGoal?.isGoalMet == true) 5 else 0

        // Consistency bonus: how many of last 7 days had goal met
        val consistencyBonus = 0 // Simplified: expand with 7-day query in future

        val total = (80 + consistencyBonus + goalBonus + streakBonus - distractionPenalty - exitPenalty)
            .coerceIn(10, 100)

        return ScoreBreakdown(
            total = total,
            consistencyBonus = consistencyBonus,
            goalBonus = goalBonus,
            streakBonus = streakBonus,
            distractionPenalty = distractionPenalty,
            emergencyExitPenalty = exitPenalty
        )
    }
}
