package com.aegisfocus.app.domain.model

data class EmergencyExitRecord(
    val id: Long = 0,
    val sessionId: Long,
    val timestampEpochMs: Long,
    val sessionDurationMinutes: Int,
    val remainingMinutesAtExit: Int,
    val reasonNote: String? = null
)
