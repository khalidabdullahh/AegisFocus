package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Records every meaningful focus lifecycle event for analytics. */
@Entity(tableName = "focus_events")
data class FocusEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,                  // FK to focus_sessions.id
    val eventType: String,                // SESSION_STARTED, SESSION_COMPLETED, SESSION_CANCELLED,
                                          // BLOCK_ATTEMPT, MINDFUL_PAUSE, EMERGENCY_EXIT, STRICT_COMMITTED
    val packageName: String? = null,      // present for BLOCK_ATTEMPT events
    val reason: String? = null,           // mindful reason for MINDFUL_PAUSE events
    val epochMs: Long = System.currentTimeMillis()
)
