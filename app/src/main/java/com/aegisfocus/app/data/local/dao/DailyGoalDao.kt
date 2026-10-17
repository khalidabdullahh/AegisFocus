package com.aegisfocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aegisfocus.app.data.local.entity.DailyGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: DailyGoalEntity)

    @Query("SELECT * FROM daily_goals WHERE dateKey = :dateKey")
    suspend fun getGoalForDate(dateKey: String): DailyGoalEntity?

    @Query("SELECT * FROM daily_goals ORDER BY dateKey DESC LIMIT 30")
    fun getRecentGoals(): Flow<List<DailyGoalEntity>>

    @Query("SELECT * FROM daily_goals WHERE dateKey LIKE :yearMonthPrefix || '%' ORDER BY dateKey ASC")
    fun getGoalsForMonth(yearMonthPrefix: String): Flow<List<DailyGoalEntity>>

    @Query("SELECT * FROM daily_goals WHERE dateKey LIKE :yearMonthPrefix || '%' ORDER BY dateKey ASC")
    suspend fun getGoalsListForMonth(yearMonthPrefix: String): List<DailyGoalEntity>

    @Query("UPDATE daily_goals SET achievedFocusMinutes = achievedFocusMinutes + :minutes, updatedAtEpochMs = :nowMs WHERE dateKey = :dateKey")
    suspend fun addFocusMinutes(dateKey: String, minutes: Int, nowMs: Long = System.currentTimeMillis())

    @Query("UPDATE daily_goals SET achievedSessions = achievedSessions + 1, updatedAtEpochMs = :nowMs WHERE dateKey = :dateKey")
    suspend fun incrementSessions(dateKey: String, nowMs: Long = System.currentTimeMillis())

    @Query("UPDATE daily_goals SET isGoalMet = 1, updatedAtEpochMs = :nowMs WHERE dateKey = :dateKey")
    suspend fun markGoalMet(dateKey: String, nowMs: Long = System.currentTimeMillis())
}
