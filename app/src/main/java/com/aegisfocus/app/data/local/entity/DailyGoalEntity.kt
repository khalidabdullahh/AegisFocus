package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stores the user's daily focus goal and completion state. */
@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val dateKey: String,        // e.g. "2025-09-08" (yyyy-MM-dd)
    val targetFocusMinutes: Int = 120,      // default 2h
    val targetSessions: Int = 3,            // default 3 sessions
    val achievedFocusMinutes: Int = 0,
    val achievedSessions: Int = 0,
    val isGoalMet: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
