package com.aegisfocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aegisfocus.app.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY startEpochMs DESC LIMIT 1")
    suspend fun getActiveSession(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY startEpochMs DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions ORDER BY startEpochMs DESC")
    suspend fun getAllSessions(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions ORDER BY startEpochMs DESC")
    fun getAllSessionsFlow(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE startEpochMs >= :startEpochMs AND startEpochMs <= :endEpochMs ORDER BY startEpochMs DESC")
    suspend fun getSessionsInRange(startEpochMs: Long, endEpochMs: Long): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE startEpochMs >= :startEpochMs AND startEpochMs <= :endEpochMs ORDER BY startEpochMs DESC")
    fun getSessionsInRangeFlow(startEpochMs: Long, endEpochMs: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE status = 'COMPLETED' AND startEpochMs >= :sinceEpochMs")
    suspend fun getCompletedSessionsSince(sinceEpochMs: Long): List<FocusSessionEntity>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED'")
    fun getCompletedSessionCountFlow(): Flow<Int>

    @Query("UPDATE focus_sessions SET status = :status WHERE id = :sessionId")
    suspend fun updateSessionStatus(sessionId: Long, status: String)

    @Query("UPDATE focus_sessions SET totalInterventions = totalInterventions + 1 WHERE id = :sessionId")
    suspend fun incrementIntervention(sessionId: Long)
}
