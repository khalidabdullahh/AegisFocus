package com.aegisfocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aegisfocus.app.data.local.entity.BlockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBlockedApp(app: BlockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(apps: List<BlockedAppEntity>)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteBlockedApp(packageName: String)

    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getBlockedAppsFlow(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAllBlockedApps(): List<BlockedAppEntity>

    @Query("SELECT packageName FROM blocked_apps WHERE isDefaultBlocked = 1")
    suspend fun getDefaultBlockedPackageNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_apps WHERE packageName = :packageName)")
    suspend fun isAppBlocked(packageName: String): Boolean

    @Query("UPDATE blocked_apps SET lastInterventionEpochMs = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastIntervention(packageName: String, timestamp: Long)
}
