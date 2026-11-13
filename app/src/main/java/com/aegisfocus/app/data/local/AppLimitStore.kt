package com.aegisfocus.app.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Daily limit for one app. A value of 0 means "no limit" for that kind. */
data class AppLimit(
    val packageName: String,
    val appName: String,
    val dailyMinutes: Int,
    val maxOpens: Int,
    val enabled: Boolean = true
)

/** On-device store for per-app daily limits and today's open counter. */
class AppLimitStore(context: Context) {

    private val prefs = context.getSharedPreferences("aegis_app_limits", Context.MODE_PRIVATE)

    @Volatile
    private var cache: List<AppLimit>? = null

    fun getLimits(): List<AppLimit> {
        cache?.let { return it }
        val loaded = read()
        cache = loaded
        return loaded
    }

    fun getLimit(packageName: String): AppLimit? = getLimits().firstOrNull { it.packageName == packageName }

    @Synchronized
    fun saveLimit(limit: AppLimit) {
        if (limit.dailyMinutes <= 0 && limit.maxOpens <= 0) return
        write(getLimits().filterNot { it.packageName == limit.packageName } + limit)
    }

    @Synchronized
    fun setEnabled(packageName: String, enabled: Boolean) {
        write(getLimits().map { if (it.packageName == packageName) it.copy(enabled = enabled) else it })
    }

    @Synchronized
    fun deleteLimit(packageName: String) {
        write(getLimits().filterNot { it.packageName == packageName })
    }

    // Opens counter (resets every day)

    @Synchronized
    fun recordOpen(packageName: String) {
        val counts = readOpens()
        counts[packageName] = (counts[packageName] ?: 0) + 1
        writeOpens(counts)
    }

    fun opensToday(packageName: String): Int = readOpens()[packageName] ?: 0

    private fun readOpens(): MutableMap<String, Int> {
        val today = LocalDate.now().toString()
        if (prefs.getString(KEY_OPENS_DATE, null) != today) return mutableMapOf()
        val raw = prefs.getString(KEY_OPENS, null) ?: return mutableMapOf()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Int>()
            obj.keys().forEach { map[it] = obj.getInt(it) }
            map
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun writeOpens(counts: Map<String, Int>) {
        val obj = JSONObject()
        counts.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit()
            .putString(KEY_OPENS_DATE, LocalDate.now().toString())
            .putString(KEY_OPENS, obj.toString())
            .apply()
    }

    private fun write(list: List<AppLimit>) {
        val array = JSONArray()
        list.forEach { l ->
            array.put(
                JSONObject()
                    .put("pkg", l.packageName)
                    .put("name", l.appName)
                    .put("minutes", l.dailyMinutes)
                    .put("opens", l.maxOpens)
                    .put("enabled", l.enabled)
            )
        }
        prefs.edit().putString(KEY_LIMITS, array.toString()).apply()
        cache = list
    }

    private fun read(): List<AppLimit> {
        val raw = prefs.getString(KEY_LIMITS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                AppLimit(
                    packageName = o.getString("pkg"),
                    appName = o.optString("name", o.getString("pkg")),
                    dailyMinutes = o.optInt("minutes", 0),
                    maxOpens = o.optInt("opens", 0),
                    enabled = o.optBoolean("enabled", true)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_LIMITS = "limits"
        private const val KEY_OPENS = "opens"
        private const val KEY_OPENS_DATE = "opens_date"
    }
}
