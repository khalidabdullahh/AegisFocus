package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aegisfocus.app.domain.model.AppCategory
import com.aegisfocus.app.domain.model.AppInfo

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val category: String = "OTHER",
    val isDefaultBlocked: Boolean = false,
    val lastInterventionEpochMs: Long = 0L
) {
    fun toDomain(): AppInfo {
        return AppInfo(
            packageName = packageName,
            appName = appName,
            category = runCatching { AppCategory.valueOf(category) }.getOrDefault(AppCategory.OTHER),
            isBlocked = true
        )
    }

    companion object {
        fun fromDomain(app: AppInfo): BlockedAppEntity {
            return BlockedAppEntity(
                packageName = app.packageName,
                appName = app.appName,
                category = app.category.name,
                isDefaultBlocked = app.isBlocked
            )
        }
    }
}
