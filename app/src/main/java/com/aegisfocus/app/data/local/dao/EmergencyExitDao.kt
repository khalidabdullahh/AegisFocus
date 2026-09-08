package com.aegisfocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aegisfocus.app.data.local.entity.EmergencyExitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyExitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyExit(exit: EmergencyExitEntity): Long

    @Query("SELECT * FROM emergency_exits ORDER BY timestampEpochMs DESC")
    fun getAllExitsFlow(): Flow<List<EmergencyExitEntity>>

    @Query("SELECT COUNT(*) FROM emergency_exits WHERE timestampEpochMs >= :sinceEpochMs")
    suspend fun getExitCountSince(sinceEpochMs: Long): Int
}
