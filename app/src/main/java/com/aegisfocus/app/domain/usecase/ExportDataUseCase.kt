package com.aegisfocus.app.domain.usecase

import com.aegisfocus.app.data.local.dao.FocusEventDao
import com.aegisfocus.app.data.local.dao.FocusSessionDao
import com.aegisfocus.app.data.local.dao.UserXpDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exports all local focus history, sessions, and discipline metrics to a structured JSON string.
 * Supports the 100% on-device data sovereignty guarantee.
 */
class ExportDataUseCase(
    private val focusSessionDao: FocusSessionDao,
    private val focusEventDao: FocusEventDao,
    private val userXpDao: UserXpDao
) {
    suspend fun exportToJsonString(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "ASR — Turn Time Into Progress")
        root.put("version", "1.0.0")
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        // User XP & Discipline
        val xp = userXpDao.getXp()
        val xpObj = JSONObject().apply {
            put("totalXp", xp?.totalXp ?: 0)
            put("level", xp?.currentLevel ?: "NEW_STARTER")
            put("currentStreak", xp?.currentStreak ?: 0)
            put("longestStreak", xp?.longestStreak ?: 0)
        }
        root.put("disciplineProfile", xpObj)

        // Focus Sessions
        val sessions = focusSessionDao.getAllSessions()
        val sessionsArr = JSONArray()
        sessions.forEach { s ->
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("mode", s.mode)
                put("startTime", s.startTime)
                put("endTime", s.endTime)
                put("targetDurationMinutes", s.targetDurationMinutes)
                put("actualDurationMinutes", s.actualDurationMinutes)
                put("isCompleted", s.isCompleted)
                put("emergencyExitUsed", s.emergencyExitUsed)
            }
            sessionsArr.put(sObj)
        }
        root.put("focusSessions", sessionsArr)

        root.toString(2)
    }
}
