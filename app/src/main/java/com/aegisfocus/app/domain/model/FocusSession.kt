package com.aegisfocus.app.domain.model

data class FocusSession(
    val id: Long = 0,
    val name: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val mode: SessionMode,
    val status: SessionStatus,
    val blockedPackageNames: List<String>,
    val profileId: Long? = null,
    val totalInterventions: Int = 0
) {
    val remainingMillis: Long
        get() = (endEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)

    val isExpired: Boolean
        get() = System.currentTimeMillis() >= endEpochMs

    val progressFraction: Float
        get() {
            val totalDurationMs = durationMinutes * 60 * 1000L
            if (totalDurationMs <= 0) return 1f
            val elapsed = (System.currentTimeMillis() - startEpochMs).coerceAtLeast(0L)
            return (elapsed.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        }
}
