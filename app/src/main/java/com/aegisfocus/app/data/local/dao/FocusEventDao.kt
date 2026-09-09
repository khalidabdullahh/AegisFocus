package com.aegisfocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aegisfocus.app.data.local.entity.FocusEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: FocusEventEntity)

    @Query("SELECT * FROM focus_events WHERE sessionId = :sessionId ORDER BY epochMs ASC")
    suspend fun getEventsForSession(sessionId: Long): List<FocusEventEntity>

    @Query("SELECT * FROM focus_events WHERE epochMs >= :sinceEpochMs ORDER BY epochMs DESC")
    fun getRecentEvents(sinceEpochMs: Long): Flow<List<FocusEventEntity>>

    @Query("SELECT COUNT(*) FROM focus_events WHERE eventType = 'BLOCK_ATTEMPT' AND epochMs >= :sinceEpochMs")
    suspend fun countBlockAttemptsAfter(sinceEpochMs: Long): Int

    @Query("SELECT COUNT(*) FROM focus_events WHERE eventType = 'EMERGENCY_EXIT' AND epochMs >= :sinceEpochMs")
    suspend fun countEmergencyExitsAfter(sinceEpochMs: Long): Int

    @Query("DELETE FROM focus_events WHERE epochMs < :beforeEpochMs")
    suspend fun pruneOldEvents(beforeEpochMs: Long)
}
