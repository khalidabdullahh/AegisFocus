package com.aegisfocus.app.data.local

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/** Small on-device counters of how often Aegis stepped in, used by the weekly report. */
class StatsStore(context: Context) {

    enum class Event { GATE_SHOWN, GATE_OPENED, LIMIT_BLOCKED, SHORTS_BLOCKED, INTENT_BLOCKED }

    private val prefs = context.getSharedPreferences("aegis_stats", Context.MODE_PRIVATE)

    @Synchronized
    fun record(event: Event) {
        val all = read()
        val today = LocalDate.now().toString()
        val day = all.optJSONObject(today) ?: JSONObject()
        day.put(event.name, day.optInt(event.name, 0) + 1)
        all.put(today, day)

        // Keep about three weeks
        val cutoff = LocalDate.now().minusDays(21).toString()
        all.keys().asSequence().toList().filter { it < cutoff }.forEach { all.remove(it) }
        prefs.edit().putString(KEY, all.toString()).apply()
    }

    /** Totals for today and the six days before it. */
    fun weekTotals(): Map<Event, Int> {
        val all = read()
        val totals = Event.values().associateWith { 0 }.toMutableMap()
        for (back in 0..6) {
            val day = all.optJSONObject(LocalDate.now().minusDays(back.toLong()).toString()) ?: continue
            Event.values().forEach { e -> totals[e] = (totals[e] ?: 0) + day.optInt(e.name, 0) }
        }
        return totals
    }

    private fun read(): JSONObject = try {
        JSONObject(prefs.getString(KEY, null) ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }

    companion object {
        private const val KEY = "days"
    }
}
