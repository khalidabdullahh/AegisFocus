package com.aegisfocus.app.domain.model

import com.aegisfocus.app.core.common.TimeUtils
import java.util.Calendar

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

    /**
     * Minutes left in this routine's window at [nowMs], or 0 when the routine is not running then.
     * Overnight windows (e.g. 23:00 to 07:00) belong to the day they started on, so the part
     * after midnight is checked against the previous day.
     */
    fun remainingMinutesAt(nowMs: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val day = cal.get(Calendar.DAY_OF_WEEK)
        val previousDay = if (day == Calendar.SUNDAY) Calendar.SATURDAY else day - 1
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = startHour * 60 + startMinute
        val end = endHour * 60 + endMinute
        if (start == end) return 0

        return if (start < end) {
            if (TimeUtils.isDaySelected(daysOfWeekBitmask, day) && now in start until end) end - now else 0
        } else {
            when {
                now >= start && TimeUtils.isDaySelected(daysOfWeekBitmask, day) -> (24 * 60 - now) + end
                now < end && TimeUtils.isDaySelected(daysOfWeekBitmask, previousDay) -> end - now
                else -> 0
            }
        }
    }

    fun isActiveAt(nowMs: Long): Boolean = remainingMinutesAt(nowMs) > 0
}
