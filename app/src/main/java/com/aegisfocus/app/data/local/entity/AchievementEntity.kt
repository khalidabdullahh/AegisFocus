package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tracks earned achievements. */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val achievementId: String,   // e.g. "FIRST_FOCUS", "STREAK_7", "IRON_WILL"
    val name: String,
    val description: String,
    val xpReward: Int,
    val earnedAtEpochMs: Long = System.currentTimeMillis()
)
