package com.aegisfocus.app.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/** A daily habit the user defines themselves. [days] uses ISO numbers: 1 = Monday ... 7 = Sunday. */
data class Habit(
    val id: String,
    val title: String,
    val days: Set<Int>,
    val createdDate: String
)

/** On-device store for custom habits and their daily check-ins (app-private SharedPreferences). */
class HabitStore(context: Context) {

    private val prefs = context.getSharedPreferences("aegis_habits", Context.MODE_PRIVATE)

    fun getHabits(): List<Habit> {
        val raw = prefs.getString(KEY_HABITS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val daysArray = o.getJSONArray("days")
                Habit(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    days = (0 until daysArray.length()).map { daysArray.getInt(it) }.toSet(),
                    createdDate = o.optString("created", LocalDate.now().toString())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun addHabit(title: String, days: Set<Int>): Habit? {
        val cleanTitle = title.trim().take(60)
        if (cleanTitle.isEmpty()) return null
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            days = if (days.isEmpty()) (1..7).toSet() else days,
            createdDate = LocalDate.now().toString()
        )
        writeHabits(getHabits() + habit)
        return habit
    }

    @Synchronized
    fun deleteHabit(id: String) {
        writeHabits(getHabits().filterNot { it.id == id })
        val done = readDone()
        done.remove(id)
        writeDone(done)
    }

    fun isDone(id: String, date: LocalDate): Boolean =
        readDone()[id]?.contains(date.toString()) == true

    @Synchronized
    fun setDone(id: String, date: LocalDate, done: Boolean) {
        val all = readDone()
        val set = all.getOrPut(id) { mutableSetOf() }
        if (done) set.add(date.toString()) else set.remove(date.toString())
        writeDone(all)
    }

    /** Consecutive scheduled days completed, ending today (or yesterday if today is still open). */
    fun streak(habit: Habit, today: LocalDate): Int {
        val done = readDone()[habit.id] ?: emptySet<String>()
        val created = LocalDate.parse(habit.createdDate)
        var day = today
        if (habit.days.contains(day.dayOfWeek.value) && !done.contains(day.toString())) {
            day = day.minusDays(1)
        }
        var streak = 0
        var guard = 0
        while (guard++ < 400 && !day.isBefore(created)) {
            if (!habit.days.contains(day.dayOfWeek.value)) {
                day = day.minusDays(1)
                continue
            }
            if (done.contains(day.toString())) {
                streak++
                day = day.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    /** Last 7 days, oldest first: true = done, false = missed, null = not scheduled or before creation. */
    fun lastSevenDays(habit: Habit, today: LocalDate): List<Boolean?> {
        val done = readDone()[habit.id] ?: emptySet<String>()
        val created = LocalDate.parse(habit.createdDate)
        return (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            when {
                d.isBefore(created) -> null
                !habit.days.contains(d.dayOfWeek.value) -> null
                else -> done.contains(d.toString())
            }
        }
    }

    private fun writeHabits(list: List<Habit>) {
        val array = JSONArray()
        list.forEach { h ->
            array.put(
                JSONObject()
                    .put("id", h.id)
                    .put("title", h.title)
                    .put("days", JSONArray(h.days.toList()))
                    .put("created", h.createdDate)
            )
        }
        prefs.edit().putString(KEY_HABITS, array.toString()).apply()
    }

    private fun readDone(): MutableMap<String, MutableSet<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()
        val raw = prefs.getString(KEY_DONE, null) ?: return result
        try {
            val obj = JSONObject(raw)
            obj.keys().forEach { id ->
                val arr = obj.getJSONArray(id)
                result[id] = (0 until arr.length()).map { arr.getString(it) }.toMutableSet()
            }
        } catch (e: Exception) {
            // ignore corrupted data
        }
        return result
    }

    private fun writeDone(all: Map<String, Set<String>>) {
        val obj = JSONObject()
        all.forEach { (id, dates) ->
            // Keep roughly the last 14 months of check-ins
            obj.put(id, JSONArray(dates.sorted().takeLast(430)))
        }
        prefs.edit().putString(KEY_DONE, obj.toString()).apply()
    }

    companion object {
        private const val KEY_HABITS = "habits"
        private const val KEY_DONE = "done"
    }
}
