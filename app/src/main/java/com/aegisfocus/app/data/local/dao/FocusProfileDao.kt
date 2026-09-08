package com.aegisfocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aegisfocus.app.data.local.entity.FocusProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: FocusProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultProfiles(profiles: List<FocusProfileEntity>)

    @Update
    suspend fun updateProfile(profile: FocusProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: FocusProfileEntity)

    @Query("SELECT * FROM focus_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): FocusProfileEntity?

    @Query("SELECT * FROM focus_profiles ORDER BY isDefault DESC, id ASC")
    fun getAllProfilesFlow(): Flow<List<FocusProfileEntity>>

    @Query("SELECT * FROM focus_profiles ORDER BY isDefault DESC, id ASC")
    suspend fun getAllProfiles(): List<FocusProfileEntity>
}
