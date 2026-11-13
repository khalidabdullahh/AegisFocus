package com.aegisfocus.app.data.local

import android.content.Context
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * A "Mindful Gate" for one app: before the app opens, the user must tick every
 * self-written mindfulness item. Items are plain text the user wrote themselves.
 */
data class MindfulGate(
    val packageName: String,
    val appName: String,
    val items: List<String>,
    val graceMinutes: Int = 5,
    val enabled: Boolean = true
)

/** On-device store for Mindful Gates (app-private SharedPreferences, never uploaded). */
class MindfulGateStore(context: Context) {

    private val prefs = context.getSharedPreferences("aegis_mindful_gate", Context.MODE_PRIVATE)

    @Volatile
    private var cache: List<MindfulGate>? = null

    fun getGates(): List<MindfulGate> {
        cache?.let { return it }
        val loaded = readFromPrefs()
        cache = loaded
        return loaded
    }

    fun getGate(packageName: String): MindfulGate? =
        getGates().firstOrNull { it.packageName == packageName }

    @Synchronized
    fun saveGate(gate: MindfulGate) {
        val cleaned = gate.copy(
            items = gate.items.map { it.trim() }.filter { it.isNotEmpty() }.take(MAX_ITEMS)
        )
        if (cleaned.items.isEmpty()) return
        write(getGates().filterNot { it.packageName == cleaned.packageName } + cleaned)
    }

    @Synchronized
    fun setEnabled(packageName: String, enabled: Boolean) {
        write(getGates().map { if (it.packageName == packageName) it.copy(enabled = enabled) else it })
    }

    @Synchronized
    fun deleteGate(packageName: String) {
        write(getGates().filterNot { it.packageName == packageName })
        unlockedUntil.remove(packageName)
    }

    /** Returns the gate only if the app must be gated right now. */
    fun gateRequiredFor(packageName: String): MindfulGate? {
        val gate = getGate(packageName) ?: return null
        if (!gate.enabled || gate.items.isEmpty()) return null
        return if (isUnlocked(packageName)) null else gate
    }

    fun markUnlocked(packageName: String, minutes: Int) {
        unlockedUntil[packageName] = SystemClock.elapsedRealtime() + minutes.coerceAtLeast(1) * 60_000L
    }

    private fun isUnlocked(packageName: String): Boolean {
        val until = unlockedUntil[packageName] ?: return false
        return SystemClock.elapsedRealtime() < until
    }

    private fun write(list: List<MindfulGate>) {
        val array = JSONArray()
        list.forEach { g ->
            array.put(
                JSONObject()
                    .put("pkg", g.packageName)
                    .put("name", g.appName)
                    .put("items", JSONArray(g.items))
                    .put("grace", g.graceMinutes)
                    .put("enabled", g.enabled)
            )
        }
        prefs.edit().putString(KEY_GATES, array.toString()).apply()
        cache = list
    }

    private fun readFromPrefs(): List<MindfulGate> {
        val raw = prefs.getString(KEY_GATES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val itemsArray = o.getJSONArray("items")
                MindfulGate(
                    packageName = o.getString("pkg"),
                    appName = o.optString("name", o.getString("pkg")),
                    items = (0 until itemsArray.length()).map { itemsArray.getString(it) },
                    graceMinutes = o.optInt("grace", 5),
                    enabled = o.optBoolean("enabled", true)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_GATES = "gates"
        const val MAX_ITEMS = 10

        /** Temporary "already unlocked" windows. In memory only, so a reboot re-arms every gate. */
        private val unlockedUntil = ConcurrentHashMap<String, Long>()
    }
}
