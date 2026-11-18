package com.aegisfocus.app.core.common

/** Reads and prints durations typed by the user, like "90", "1h 30m", "1:30" or "1.5h". Pure Kotlin. */
object DurationInput {

    /** Returns the number of minutes, or null if the text is not a valid duration. */
    fun parse(raw: String): Int? {
        val t = raw.trim().lowercase().replace(" ", "")
        if (t.isEmpty()) return null

        Regex("^(\\d{1,4})$").matchEntire(t)?.let {
            return it.groupValues[1].toInt().takeIf { v -> v > 0 }
        }
        Regex("^(\\d{1,2}):(\\d{1,2})$").matchEntire(t)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt()
            return if (m < 60) (h * 60 + m).takeIf { v -> v > 0 } else null
        }
        Regex("^(\\d{1,2})\\.(\\d)h(?:ours?|rs?)?$").matchEntire(t)?.let {
            val h = it.groupValues[1].toInt()
            val tenth = it.groupValues[2].toInt()
            return (h * 60 + tenth * 6).takeIf { v -> v > 0 }
        }
        Regex("^(\\d{1,2})h(?:ours?|rs?)?(?:(\\d{1,3})m(?:in(?:ute)?s?)?)?$").matchEntire(t)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].ifEmpty { "0" }.toInt()
            return (h * 60 + m).takeIf { v -> v > 0 }
        }
        Regex("^(\\d{1,4})m(?:in(?:ute)?s?)?$").matchEntire(t)?.let {
            return it.groupValues[1].toInt().takeIf { v -> v > 0 }
        }
        return null
    }

    /** 45 -> "45 min", 120 -> "2h", 1020 -> "17h", 90 -> "1h 30m". */
    fun format(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}
