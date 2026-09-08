package com.aegisfocus.app.domain.repository

import com.aegisfocus.app.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppDiscoveryRepository {
    fun getInstalledAppsFlow(): Flow<List<AppInfo>>
    suspend fun loadInstalledApps(): List<AppInfo>
    suspend fun getBlockedPackageNames(): List<String>
    suspend fun setAppBlocked(packageName: String, isBlocked: Boolean)
    suspend fun setAppsBlocked(packageNames: List<String>, isBlocked: Boolean)
    fun isPackageBlockedFlow(packageName: String): Flow<Boolean>
    suspend fun getAppName(packageName: String): String
}
