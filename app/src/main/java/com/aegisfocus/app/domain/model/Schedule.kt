package com.aegisfocus.app.domain.model

data class Schedule(
    val id: Long = 0,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeekBitmask: Int, // e.g. 62 for Mon-Fri
    val profileId: Long? = null,
    val blockedPackageNames: List<String> = emptyList(),
    val isStrictMode: Boolean = false,
    val isEnabled: Boolean = true
) {
    val durationMinutes: Int
        get() {
            val startTotalMinutes = startHour * 60 + startMinute
            val endTotalMinutes = endHour * 60 + endMinute
            return if (endTotalMinutes >= startTotalMinutes) {
                endTotalMinutes - startTotalMinutes
            } else {
                (24 * 60 - startTotalMinutes) + endTotalMinutes
            }
        }
}
