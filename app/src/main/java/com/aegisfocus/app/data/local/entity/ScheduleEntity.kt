package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aegisfocus.app.domain.model.Schedule

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeekBitmask: Int,
    val profileId: Long? = null,
    val blockedPackageNames: String = "",
    val isStrictMode: Boolean = false,
    val isEnabled: Boolean = true
) {
    fun toDomain(): Schedule {
        return Schedule(
            id = id,
            name = name,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            daysOfWeekBitmask = daysOfWeekBitmask,
            profileId = profileId,
            blockedPackageNames = if (blockedPackageNames.isBlank()) emptyList() else blockedPackageNames.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            isStrictMode = isStrictMode,
            isEnabled = isEnabled
        )
    }

    companion object {
        fun fromDomain(schedule: Schedule): ScheduleEntity {
            return ScheduleEntity(
                id = schedule.id,
                name = schedule.name,
                startHour = schedule.startHour,
                startMinute = schedule.startMinute,
                endHour = schedule.endHour,
                endMinute = schedule.endMinute,
                daysOfWeekBitmask = schedule.daysOfWeekBitmask,
                profileId = schedule.profileId,
                blockedPackageNames = schedule.blockedPackageNames.joinToString(","),
                isStrictMode = schedule.isStrictMode,
                isEnabled = schedule.isEnabled
            )
        }
    }
}
