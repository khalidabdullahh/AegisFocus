package com.aegisfocus.app.domain.motivation

import com.aegisfocus.app.data.local.dao.UserXpDao
import com.aegisfocus.app.data.local.entity.AchievementEntity
import com.aegisfocus.app.data.local.entity.UserXpEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/** Manages XP accrual, level-up detection, streak tracking, and achievement unlocks. */
class XpManager(private val userXpDao: UserXpDao) {

    val currentLevel: Flow<DisciplineLevel> = userXpDao.observeXp().map { entity ->
        DisciplineLevel.fromXp(entity?.totalXp ?: 0)
    }

    val currentXp: Flow<Int> = userXpDao.observeXp().map { it?.totalXp ?: 0 }

    val currentStreak: Flow<Int> = userXpDao.observeXp().map { it?.currentStreak ?: 0 }

    /** Award XP for completing a focus session. */
    suspend fun awardSessionXp(durationMinutes: Int, wasStrictMode: Boolean, blockAttempts: Int) {
        val base = (durationMinutes * 2).coerceAtMost(200)  // 2 XP/min, cap 200
        val strictBonus = if (wasStrictMode) 50 else 0
        val cleanBonus = if (blockAttempts == 0) 25 else 0   // "Laser Focus" bonus
        val total = base + strictBonus + cleanBonus
        userXpDao.addXp(total)
        ensureUserXpInitialized()
        checkAndUnlockAchievements()
    }

    /** Award XP for a daily goal completion. */
    suspend fun awardGoalXp() {
        userXpDao.addXp(50)
        checkAndUnlockAchievements()
    }

    /** Update streak based on today's activity. Call once per day. */
    suspend fun updateStreak(metGoalToday: Boolean) {
        val entity = userXpDao.getXp() ?: UserXpEntity()
        val todayKey = LocalDate.now(ZoneId.systemDefault()).toString()

        if (entity.lastActivityDateKey == todayKey) return // already updated today

        val yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1).toString()
        val wasYesterday = entity.lastActivityDateKey == yesterday

        val newStreak = when {
            metGoalToday && wasYesterday -> entity.currentStreak + 1
            metGoalToday                -> 1
            else                        -> 0
        }

        val newLevel = DisciplineLevel.fromXp(entity.totalXp).name

        userXpDao.upsertXp(
            entity.copy(
                currentStreak = newStreak,
                longestStreak = maxOf(entity.longestStreak, newStreak),
                lastActivityDateKey = if (metGoalToday) todayKey else entity.lastActivityDateKey,
                currentLevel = newLevel,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )

        checkStreakAchievements(newStreak)
    }

    private suspend fun ensureUserXpInitialized() {
        if (userXpDao.getXp() == null) {
            userXpDao.upsertXp(UserXpEntity())
        }
    }

    private suspend fun checkAndUnlockAchievements() {
        val entity = userXpDao.getXp() ?: return
        val level = DisciplineLevel.fromXp(entity.totalXp)
        if (level == DisciplineLevel.IRON_WILL) {
            unlockAchievementIfNew(Achievement.IRON_WILL_REACHED)
        }
    }

    private suspend fun checkStreakAchievements(streak: Int) {
        when {
            streak >= 30 -> unlockAchievementIfNew(Achievement.STREAK_30)
            streak >= 14 -> unlockAchievementIfNew(Achievement.STREAK_14)
            streak >= 7  -> unlockAchievementIfNew(Achievement.STREAK_7)
            streak >= 3  -> unlockAchievementIfNew(Achievement.STREAK_3)
        }
    }

    suspend fun unlockAchievementIfNew(achievement: Achievement) {
        if (userXpDao.hasAchievement(achievement.achievementId) == 0) {
            userXpDao.insertAchievement(
                AchievementEntity(
                    achievementId = achievement.achievementId,
                    name = achievement.title,
                    description = achievement.description,
                    xpReward = achievement.xpReward
                )
            )
            userXpDao.addXp(achievement.xpReward)
        }
    }
}
