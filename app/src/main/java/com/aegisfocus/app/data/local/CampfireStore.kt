package com.aegisfocus.app.data.local

import android.content.Context
import com.aegisfocus.app.domain.campfire.CampfireMode
import com.aegisfocus.app.domain.campfire.CampfireSound
import com.aegisfocus.app.domain.campfire.SavedMix
import org.json.JSONArray
import org.json.JSONObject

/** On-device store for the last Campfire mix and the user's saved mixes (never uploaded). */
class CampfireStore(context: Context) {

    private val prefs = context.getSharedPreferences("aegis_campfire", Context.MODE_PRIVATE)

    // ----- last used mix -----

    fun lastMode(): CampfireMode? =
        prefs.getString(KEY_LAST_MODE, null)?.let { CampfireMode.fromName(it) }

    fun lastLevels(): Map<CampfireSound, Float> =
        prefs.getString(KEY_LAST_LEVELS, null)?.let { decodeLevels(runCatching { JSONObject(it) }.getOrNull()) }
            ?: emptyMap()

    fun saveLast(mode: CampfireMode, levels: Map<CampfireSound, Float>) {
        prefs.edit()
            .putString(KEY_LAST_MODE, mode.name)
            .putString(KEY_LAST_LEVELS, encodeLevels(levels).toString())
            .apply()
    }

    // ----- saved mixes -----

    fun getMixes(): List<SavedMix> {
        val raw = prefs.getString(KEY_MIXES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val levels = decodeLevels(o.optJSONObject("levels")) ?: return@mapNotNull null
                SavedMix(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    mode = CampfireMode.fromName(o.optString("mode")),
                    levels = levels
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun addMix(name: String, mode: CampfireMode, levels: Map<CampfireSound, Float>): List<SavedMix> {
        val mix = SavedMix(System.currentTimeMillis(), name.trim().take(24), mode, levels)
        val next = getMixes() + mix
        writeMixes(next)
        return next
    }

    @Synchronized
    fun deleteMix(id: Long): List<SavedMix> {
        val next = getMixes().filterNot { it.id == id }
        writeMixes(next)
        return next
    }

    private fun writeMixes(list: List<SavedMix>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("mode", it.mode.name)
                    .put("levels", encodeLevels(it.levels))
            )
        }
        prefs.edit().putString(KEY_MIXES, arr.toString()).apply()
    }

    private fun encodeLevels(levels: Map<CampfireSound, Float>): JSONObject {
        val o = JSONObject()
        levels.forEach { (sound, v) -> o.put(sound.key, v.toDouble()) }
        return o
    }

    private fun decodeLevels(o: JSONObject?): Map<CampfireSound, Float>? {
        if (o == null) return null
        val out = mutableMapOf<CampfireSound, Float>()
        o.keys().forEach { key ->
            val sound = CampfireSound.fromKey(key) ?: return@forEach
            out[sound] = o.optDouble(key, 0.0).toFloat().coerceIn(0f, 1f)
        }
        return out
    }

    companion object {
        private const val KEY_LAST_MODE = "last_mode"
        private const val KEY_LAST_LEVELS = "last_levels"
        private const val KEY_MIXES = "saved_mixes"
    }
}
