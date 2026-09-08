package com.aegisfocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aegisfocus.app.data.local.entity.UsageSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSnapshot(snapshot: UsageSnapshotEntity)

    @Query("SELECT * FROM usage_snapshots WHERE date = :date LIMIT 1")
    suspend fun getSnapshotByDate(date: String): UsageSnapshotEntity?

    @Query("SELECT * FROM usage_snapshots WHERE date = :date LIMIT 1")
    fun getSnapshotFlowByDate(date: String): Flow<UsageSnapshotEntity?>

    @Query("SELECT * FROM usage_snapshots ORDER BY date DESC LIMIT :limitDays")
    suspend fun getRecentSnapshots(limitDays: Int): List<UsageSnapshotEntity>

    @Query("SELECT * FROM usage_snapshots ORDER BY date DESC LIMIT :limitDays")
    fun getRecentSnapshotsFlow(limitDays: Int): Flow<List<UsageSnapshotEntity>>
}
