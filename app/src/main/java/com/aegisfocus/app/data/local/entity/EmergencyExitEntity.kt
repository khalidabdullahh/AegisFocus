package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aegisfocus.app.domain.model.EmergencyExitRecord

@Entity(tableName = "emergency_exits")
data class EmergencyExitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestampEpochMs: Long,
    val sessionDurationMinutes: Int,
    val remainingMinutesAtExit: Int,
    val reasonNote: String? = null
) {
    fun toDomain(): EmergencyExitRecord {
        return EmergencyExitRecord(
            id = id,
            sessionId = sessionId,
            timestampEpochMs = timestampEpochMs,
            sessionDurationMinutes = sessionDurationMinutes,
            remainingMinutesAtExit = remainingMinutesAtExit,
            reasonNote = reasonNote
        )
    }

    companion object {
        fun fromDomain(record: EmergencyExitRecord): EmergencyExitEntity {
            return EmergencyExitEntity(
                id = record.id,
                sessionId = record.sessionId,
                timestampEpochMs = record.timestampEpochMs,
                sessionDurationMinutes = record.sessionDurationMinutes,
                remainingMinutesAtExit = record.remainingMinutesAtExit,
                reasonNote = record.reasonNote
            )
        }
    }
}
