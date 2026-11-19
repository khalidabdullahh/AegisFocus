package com.aegisfocus.app.domain.model

import com.aegisfocus.app.core.common.TimeUtils
import java.util.Calendar

/** True if this routine is switched on and its time window includes [now] (overnight windows too). */
fun Schedule.isActiveNow(now: Calendar = Calendar.getInstance()): Boolean {
    if (!isEnabled) return false
    val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val start = startHour * 60 + startMinute
    val end = endHour * 60 + endMinute
    val today = now.get(Calendar.DAY_OF_WEEK)
    val yesterday = if (today == Calendar.SUNDAY) Calendar.SATURDAY else today - 1

    return if (end >= start) {
        TimeUtils.isDaySelected(daysOfWeekBitmask, today) && minutes in start until end
    } else {
        // Overnight window, like 22:00 to 06:00
        (TimeUtils.isDaySelected(daysOfWeekBitmask, today) && minutes >= start) ||
            (TimeUtils.isDaySelected(daysOfWeekBitmask, yesterday) && minutes < end)
    }
}
