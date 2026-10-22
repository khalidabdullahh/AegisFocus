package com.aegisfocus.app.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** A daily reminder shown as a notification at a fixed time. */
data class Reminder(
    val id: Long,
    val title: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true
)

/** Everything the Clean Streak tracker needs to render. */
data class CleanStreakState(
    val markedToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int,
    /** The last 7 days, oldest first, with whether each day was checked in. */
    val lastSevenDays: List<Pair<LocalDate, Boolean>>
)

/**
 * Small on-device store for the Productivity tab (reminders and the private Clean Streak).
 * Everything stays in app-private SharedPreferences and is never uploaded.
 */
class ProductivityStore(context: Context) {

    private val prefs = context.getSharedPreferences("aegis_productivity", Context.MODE_PRIVATE)

    // ─── Reminders ───────────────────────────────────────────────────────────

    fun getReminders(): List<Reminder> {
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Reminder(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    enabled = o.optBoolean("enabled", true)
                )
            }.sortedWith(compareBy({ it.hour }, { it.minute }))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveReminders(list: List<Reminder>) {
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("hour", it.hour)
                    .put("minute", it.minute)
                    .put("enabled", it.enabled)
            )
        }
        prefs.edit().putString(KEY_REMINDERS, array.toString()).apply()
    }

    fun addReminder(title: String, hour: Int, minute: Int): Reminder {
        val reminder = Reminder(
            id = System.currentTimeMillis() % 1_000_000_000L,
            title = title.trim(),
            hour = hour,
            minute = minute
        )
        saveReminders(getReminders() + reminder)
        return reminder
    }

    fun setReminderEnabled(id: Long, enabled: Boolean) {
        saveReminders(getReminders().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun deleteReminder(id: Long) {
        saveReminders(getReminders().filterNot { it.id == id })
    }

    fun getReminder(id: Long): Reminder? = getReminders().firstOrNull { it.id == id }

    // ─── Clean Streak ────────────────────────────────────────────────────────

    private fun cleanDays(): Set<LocalDate> =
        (prefs.getStringSet(KEY_CLEAN_DAYS, emptySet()) ?: emptySet())
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()

    private fun saveCleanDays(days: Set<LocalDate>) {
        prefs.edit().putStringSet(KEY_CLEAN_DAYS, days.map { it.toString() }.toSet()).apply()
    }

    fun setTodayChecked(checked: Boolean) {
        val today = LocalDate.now()
        val days = cleanDays().toMutableSet()
        if (checked) days.add(today) else days.remove(today)
        saveCleanDays(days)
        updateBest(computeStreak(days, today))
    }

    /** Clears the current streak. The best streak is kept. */
    fun startOver() {
        saveCleanDays(emptySet())
    }

    fun getCleanStreakState(): CleanStreakState {
        val today = LocalDate.now()
        val days = cleanDays()
        val current = computeStreak(days, today)
        updateBest(current)
        return CleanStreakState(
            markedToday = today in days,
            currentStreak = current,
            bestStreak = prefs.getInt(KEY_BEST_STREAK, 0).coerceAtLeast(current),
            lastSevenDays = (6 downTo 0).map { offset ->
                val d = today.minusDays(offset.toLong())
                d to (d in days)
            }
        )
    }

    /** Consecutive checked days ending today, or ending yesterday if today is not checked yet. */
    private fun computeStreak(days: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun updateBest(current: Int) {
        if (current > prefs.getInt(KEY_BEST_STREAK, 0)) {
            prefs.edit().putInt(KEY_BEST_STREAK, current).apply()
        }
    }

    private companion object {
        const val KEY_REMINDERS = "reminders_json"
        const val KEY_CLEAN_DAYS = "clean_days"
        const val KEY_BEST_STREAK = "clean_best_streak"
    }
}
