package com.aegisfocus.app.data.remote

import android.content.Context
import com.aegisfocus.app.core.theme.AegisThemeState
import com.aegisfocus.app.core.theme.ThemeMode
import com.aegisfocus.app.data.local.AppLimitStore
import com.aegisfocus.app.data.local.HabitStore
import com.aegisfocus.app.data.local.IntentStore
import com.aegisfocus.app.data.local.MindfulGateStore
import com.aegisfocus.app.domain.model.Schedule
import com.aegisfocus.app.domain.repository.ScheduleRepository
import com.aegisfocus.app.service.receiver.ScheduleAlarmScheduler
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps the user's own setup (habits, gates, limits, YouTube topics, routines, theme) in the cloud
 * so it survives a reinstall or a new phone. Stored as one JSON text in the user's profile row.
 * Pro plan, name and photo are restored separately by [SupabaseCloudRestoreService].
 */
class SettingsBackupService(
    private val context: Context,
    private val cloud: SupabaseCloudRestoreService,
    private val authService: SupabaseAuthService,
    private val habitStore: HabitStore,
    private val gateStore: MindfulGateStore,
    private val limitStore: AppLimitStore,
    private val intentStore: IntentStore,
    private val scheduleRepository: ScheduleRepository
) {
    private val prefs = context.getSharedPreferences("aegis_backup", Context.MODE_PRIVATE)

    fun lastBackupMs(): Long = prefs.getLong(KEY_LAST_MS, 0L)

    private fun signedIn() = authService.currentUser.value != null

    private suspend fun buildSnapshot(): JSONObject {
        val schedules = scheduleRepository.getAllSchedulesFlow().first()
        val scheduleArray = JSONArray()
        schedules.forEach { s ->
            scheduleArray.put(
                JSONObject()
                    .put("name", s.name)
                    .put("sh", s.startHour).put("sm", s.startMinute)
                    .put("eh", s.endHour).put("em", s.endMinute)
                    .put("days", s.daysOfWeekBitmask)
                    .put("strict", s.isStrictMode)
                    .put("enabled", s.isEnabled)
                    .put("apps", JSONArray(s.blockedPackageNames))
            )
        }
        return JSONObject()
            .put("v", 1)
            .put("habits", habitStore.exportRaw())
            .put("gates", gateStore.exportRaw())
            .put("limits", limitStore.exportRaw())
            .put("intent", intentStore.exportJson())
            .put("theme", AegisThemeState.mode.name)
            .put("schedules", scheduleArray)
    }

    /** Uploads the backup. Returns a short message for the screen. */
    suspend fun backupNow(): String {
        if (!signedIn()) return "Sign in first to use cloud backup."
        val json = buildSnapshot().toString()
        return if (cloud.pushSettingsJson(json)) {
            markUploaded(json)
            "Backed up."
        } else {
            "Backup failed. Check the internet, or run the one-time server SQL for backups."
        }
    }

    /** Uploads only when something changed since the last upload. Safe to call often. */
    suspend fun backupIfChanged() {
        if (!signedIn()) return
        val json = buildSnapshot().toString()
        if (json.hashCode().toString() == prefs.getString(KEY_HASH, null)) return
        if (cloud.pushSettingsJson(json)) markUploaded(json)
    }

    /** Replaces local setup with the cloud copy. */
    suspend fun restoreNow(): String {
        if (!signedIn()) return "Sign in first to restore."
        val (reachable, json) = cloud.fetchSettingsJson()
        if (!reachable) return "Could not reach the backup. Check the internet, or run the one-time server SQL."
        if (json == null) return "No backup found for this account yet."
        apply(JSONObject(json), clearSchedules = true)
        markUploaded(buildSnapshot().toString())
        return "Restored. Some screens update after you reopen them."
    }

    /** After login on a fresh install: restore the cloud copy only if nothing is set up here yet. */
    suspend fun restoreIfFresh() {
        if (!signedIn()) return
        val schedules = scheduleRepository.getAllSchedulesFlow().first()
        val fresh = habitStore.getHabits().isEmpty() && gateStore.getGates().isEmpty() &&
            limitStore.getLimits().isEmpty() && schedules.isEmpty() && intentStore.isUntouched()
        if (!fresh) return
        val (_, json) = cloud.fetchSettingsJson()
        if (json == null) return
        apply(JSONObject(json), clearSchedules = false)
        markUploaded(buildSnapshot().toString())
    }

    private suspend fun apply(o: JSONObject, clearSchedules: Boolean) {
        habitStore.importRaw(JSONObject().put("habits", o.optJSONObject("habits")?.optString("habits", "") ?: "")
            .put("done", o.optJSONObject("habits")?.optString("done", "") ?: ""))
        gateStore.importRaw(o.optString("gates", ""))
        limitStore.importRaw(o.optString("limits", ""))
        o.optJSONObject("intent")?.let { intentStore.importJson(it) }
        ThemeMode.values().firstOrNull { it.name == o.optString("theme") }?.let { AegisThemeState.set(context, it) }

        if (clearSchedules) {
            scheduleRepository.getAllSchedulesFlow().first().forEach {
                scheduleRepository.deleteSchedule(it)
                ScheduleAlarmScheduler.cancelAlarm(context, it.id)
            }
        }
        val array = o.optJSONArray("schedules") ?: JSONArray()
        for (i in 0 until array.length()) {
            val s = array.getJSONObject(i)
            val apps = s.optJSONArray("apps") ?: JSONArray()
            scheduleRepository.saveSchedule(
                Schedule(
                    name = s.optString("name", "Routine"),
                    startHour = s.optInt("sh"), startMinute = s.optInt("sm"),
                    endHour = s.optInt("eh"), endMinute = s.optInt("em"),
                    daysOfWeekBitmask = s.optInt("days", 127),
                    blockedPackageNames = (0 until apps.length()).map { apps.getString(it) },
                    isStrictMode = s.optBoolean("strict", false),
                    isEnabled = s.optBoolean("enabled", true)
                )
            )
        }
    }

    private fun markUploaded(json: String) {
        prefs.edit()
            .putString(KEY_HASH, json.hashCode().toString())
            .putLong(KEY_LAST_MS, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val KEY_HASH = "last_hash"
        private const val KEY_LAST_MS = "last_ms"
    }
}
