package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_snapshots")
data class UsageSnapshotEntity(
    @PrimaryKey
    val date: String, // yyyy-MM-dd
    val totalScreenTimeMs: Long = 0L,
    val totalFocusTimeMs: Long = 0L,
    val sessionsCompleted: Int = 0,
    val streakDays: Int = 0,
    val topDistractingAppsJson: String = ""
)
