package com.aegisfocus.app.domain.repository

import com.aegisfocus.app.domain.model.FocusProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getAllProfilesFlow(): Flow<List<FocusProfile>>
    suspend fun getAllProfiles(): List<FocusProfile>
    suspend fun getProfileById(id: Long): FocusProfile?
    suspend fun saveProfile(profile: FocusProfile): Long
    suspend fun deleteProfile(profile: FocusProfile)
}
