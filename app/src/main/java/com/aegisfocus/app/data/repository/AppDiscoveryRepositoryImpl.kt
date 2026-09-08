package com.aegisfocus.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.aegisfocus.app.data.local.dao.BlockedAppDao
import com.aegisfocus.app.data.local.entity.BlockedAppEntity
import com.aegisfocus.app.domain.model.AppCategory
import com.aegisfocus.app.domain.model.AppInfo
import com.aegisfocus.app.domain.repository.AppDiscoveryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppDiscoveryRepositoryImpl(
    private val context: Context,
    private val blockedAppDao: BlockedAppDao
) : AppDiscoveryRepository {

    private val packageManager: PackageManager = context.packageManager

    override fun getInstalledAppsFlow(): Flow<List<AppInfo>> {
        return blockedAppDao.getBlockedAppsFlow().map { blockedEntities ->
            val blockedMap = blockedEntities.associateBy { it.packageName }
            val installed = loadInstalledApps()
            installed.map { app ->
                if (blockedMap.containsKey(app.packageName)) {
                    app.copy(isBlocked = true)
                } else {
                    app
                }
            }
        }
    }

    override suspend fun loadInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val myPackageName = context.packageName
        val blockedList = blockedAppDao.getAllBlockedApps().associateBy { it.packageName }

        val apps = resolveInfos
            .filter { it.activityInfo.packageName != myPackageName }
            .map { resolveInfo ->
                val pkgName = resolveInfo.activityInfo.packageName
                val appLabel = resolveInfo.loadLabel(packageManager).toString()
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val category = categorizeApp(pkgName, resolveInfo.activityInfo.applicationInfo)
                val isBlocked = blockedList.containsKey(pkgName)

                AppInfo(
                    packageName = pkgName,
                    appName = if (appLabel.isNotBlank()) appLabel else pkgName,
                    category = category,
                    isBlocked = isBlocked,
                    isSystemApp = isSystem
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }

        // If running on a minimal emulator with few launchable apps, ensure rich mockable list if empty
        if (apps.isEmpty()) {
            getSampleApps()
        } else {
            apps
        }
    }

    override suspend fun getBlockedPackageNames(): List<String> {
        return blockedAppDao.getAllBlockedApps().map { it.packageName }
    }

    override suspend fun setAppBlocked(packageName: String, isBlocked: Boolean) {
        if (isBlocked) {
            val appName = getAppName(packageName)
            blockedAppDao.insertOrUpdateBlockedApp(
                BlockedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    category = "OTHER",
                    isDefaultBlocked = true
                )
            )
        } else {
            blockedAppDao.deleteBlockedApp(packageName)
        }
    }

    override suspend fun setAppsBlocked(packageNames: List<String>, isBlocked: Boolean) {
        if (isBlocked) {
            val entities = packageNames.map { pkg ->
                BlockedAppEntity(
                    packageName = pkg,
                    appName = getAppName(pkg),
                    category = "OTHER",
                    isDefaultBlocked = true
                )
            }
            blockedAppDao.insertOrUpdateAll(entities)
        } else {
            packageNames.forEach { pkg ->
                blockedAppDao.deleteBlockedApp(pkg)
            }
        }
    }

    override fun isPackageBlockedFlow(packageName: String): Flow<Boolean> {
        return blockedAppDao.getBlockedAppsFlow().map { list ->
            list.any { it.packageName == packageName }
        }
    }

    override suspend fun getAppName(packageName: String): String = withContext(Dispatchers.IO) {
        try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }

    private fun categorizeApp(packageName: String, appInfo: ApplicationInfo): AppCategory {
        val lowerPkg = packageName.lowercase()
        return when {
            lowerPkg.contains("instagram") || lowerPkg.contains("twitter") || lowerPkg.contains("facebook") ||
            lowerPkg.contains("tiktok") || lowerPkg.contains("musically") || lowerPkg.contains("reddit") ||
            lowerPkg.contains("threads") || lowerPkg.contains("snapchat") -> AppCategory.SOCIAL

            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("spotify") ||
            lowerPkg.contains("twitch") || lowerPkg.contains("hulu") || lowerPkg.contains("disney") ||
            lowerPkg.contains("primevideo") -> AppCategory.ENTERTAINMENT

            lowerPkg.contains("game") || lowerPkg.contains("play") && lowerPkg.contains("games") ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo.category == ApplicationInfo.CATEGORY_GAME) -> AppCategory.GAMES

            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("discord") ||
            lowerPkg.contains("messenger") || lowerPkg.contains("signal") -> AppCategory.COMMUNICATION

            lowerPkg.contains("docs") || lowerPkg.contains("sheets") || lowerPkg.contains("notion") ||
            lowerPkg.contains("slack") || lowerPkg.contains("trello") || lowerPkg.contains("jira") -> AppCategory.PRODUCTIVITY

            else -> AppCategory.OTHER
        }
    }

    private fun getSampleApps(): List<AppInfo> {
        return listOf(
            AppInfo("com.google.android.youtube", "YouTube", AppCategory.ENTERTAINMENT),
            AppInfo("com.instagram.android", "Instagram", AppCategory.SOCIAL),
            AppInfo("com.twitter.android", "X (Twitter)", AppCategory.SOCIAL),
            AppInfo("com.zhiliaoapp.musically", "TikTok", AppCategory.SOCIAL),
            AppInfo("com.reddit.frontpage", "Reddit", AppCategory.SOCIAL),
            AppInfo("com.facebook.katana", "Facebook", AppCategory.SOCIAL),
            AppInfo("com.netflix.mediaclient", "Netflix", AppCategory.ENTERTAINMENT),
            AppInfo("com.discord", "Discord", AppCategory.COMMUNICATION),
            AppInfo("com.slack", "Slack", AppCategory.PRODUCTIVITY)
        )
    }
}
