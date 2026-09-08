package com.aegisfocus.app.data.repository

import com.aegisfocus.app.data.local.dao.FocusProfileDao
import com.aegisfocus.app.data.local.entity.FocusProfileEntity
import com.aegisfocus.app.domain.model.FocusProfile
import com.aegisfocus.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val profileDao: FocusProfileDao
) : ProfileRepository {

    override fun getAllProfilesFlow(): Flow<List<FocusProfile>> {
        return profileDao.getAllProfilesFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllProfiles(): List<FocusProfile> {
        return profileDao.getAllProfiles().map { it.toDomain() }
    }

    override suspend fun getProfileById(id: Long): FocusProfile? {
        return profileDao.getProfileById(id)?.toDomain()
    }

    override suspend fun saveProfile(profile: FocusProfile): Long {
        return profileDao.insertProfile(FocusProfileEntity.fromDomain(profile))
    }

    override suspend fun deleteProfile(profile: FocusProfile) {
        profileDao.deleteProfile(FocusProfileEntity.fromDomain(profile))
    }
}
