package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aegisfocus.app.domain.model.FocusProfile

@Entity(tableName = "focus_profiles")
data class FocusProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "shield",
    val defaultDurationMinutes: Int = 25,
    val isStrictModeDefault: Boolean = false,
    val blockedPackageNames: String = "",
    val isDefault: Boolean = false
) {
    fun toDomain(): FocusProfile {
        return FocusProfile(
            id = id,
            name = name,
            iconName = iconName,
            defaultDurationMinutes = defaultDurationMinutes,
            isStrictModeDefault = isStrictModeDefault,
            blockedPackageNames = if (blockedPackageNames.isBlank()) emptyList() else blockedPackageNames.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            isDefault = isDefault
        )
    }

    companion object {
        fun fromDomain(profile: FocusProfile): FocusProfileEntity {
            return FocusProfileEntity(
                id = profile.id,
                name = profile.name,
                iconName = profile.iconName,
                defaultDurationMinutes = profile.defaultDurationMinutes,
                isStrictModeDefault = profile.isStrictModeDefault,
                blockedPackageNames = profile.blockedPackageNames.joinToString(","),
                isDefault = profile.isDefault
            )
        }
    }
}
