package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stores cumulative XP, current level, and streak information. Singleton row (id=1). */
@Entity(tableName = "user_xp")
data class UserXpEntity(
    @PrimaryKey val id: Int = 1,
    val totalXp: Int = 0,
    val currentLevel: String = "NEW_STARTER",   // NEW_STARTER, CONSISTENT, FOCUSED, DISCIPLINED, IRON_WILL
    val currentStreak: Int = 0,                  // consecutive days with goal met
    val longestStreak: Int = 0,
    val lastActivityDateKey: String = "",        // yyyy-MM-dd
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
