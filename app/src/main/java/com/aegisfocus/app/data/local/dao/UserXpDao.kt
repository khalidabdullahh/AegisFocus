package com.aegisfocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aegisfocus.app.data.local.entity.AchievementEntity
import com.aegisfocus.app.data.local.entity.UserXpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserXpDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertXp(xp: UserXpEntity)

    @Query("SELECT * FROM user_xp WHERE id = 1")
    fun observeXp(): Flow<UserXpEntity?>

    @Query("SELECT * FROM user_xp WHERE id = 1")
    suspend fun getXp(): UserXpEntity?

    @Query("UPDATE user_xp SET totalXp = totalXp + :xp, updatedAtEpochMs = :nowMs WHERE id = 1")
    suspend fun addXp(xp: Int, nowMs: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements ORDER BY earnedAtEpochMs DESC")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT COUNT(*) FROM achievements WHERE achievementId = :id")
    suspend fun hasAchievement(id: String): Int
}
