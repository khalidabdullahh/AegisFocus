package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val mode: String, // NORMAL, STRICT
    val status: String, // ACTIVE, COMPLETED, CANCELLED, EMERGENCY_EXITED
    val blockedPackageNames: String, // Comma-separated list or JSON
    val profileId: Long? = null,
    val totalInterventions: Int = 0
) {
    fun toDomain(): FocusSession {
        return FocusSession(
            id = id,
            name = name,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            durationMinutes = durationMinutes,
            mode = runCatching { SessionMode.valueOf(mode) }.getOrDefault(SessionMode.NORMAL),
            status = runCatching { SessionStatus.valueOf(status) }.getOrDefault(SessionStatus.ACTIVE),
            blockedPackageNames = if (blockedPackageNames.isBlank()) emptyList() else blockedPackageNames.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            profileId = profileId,
            totalInterventions = totalInterventions
        )
    }

    companion object {
        fun fromDomain(session: FocusSession): FocusSessionEntity {
            return FocusSessionEntity(
                id = session.id,
                name = session.name,
                startEpochMs = session.startEpochMs,
                endEpochMs = session.endEpochMs,
                durationMinutes = session.durationMinutes,
                mode = session.mode.name,
                status = session.status.name,
                blockedPackageNames = session.blockedPackageNames.joinToString(","),
                profileId = session.profileId,
                totalInterventions = session.totalInterventions
            )
        }
    }
}
