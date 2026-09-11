package com.aegisfocus.app.data.remote

import android.util.Log
import com.aegisfocus.app.data.local.dao.SubscriptionDao
import com.aegisfocus.app.data.local.dao.UserXpDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Syncs user discipline statistics and subscription records to Supabase PostgreSQL.
 */
class SupabaseSyncService(
    private val authService: SupabaseAuthService,
    private val userXpDao: UserXpDao,
    private val subscriptionDao: SubscriptionDao
) {
    private val tag = "SupabaseSyncService"

    /**
     * Uploads local discipline metrics and streaks to cloud.
     */
    suspend fun syncStatsToCloud(): Boolean = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value ?: return@withContext false
        val token = user.accessToken ?: return@withContext false

        try {
            val xp = userXpDao.getXp() ?: return@withContext false

            val url = URL("${SupabaseConfig.REST_ENDPOINT}/user_stats_sync?on_conflict=user_id")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("user_id", user.id)
                put("total_focus_minutes", 0)
                put("discipline_score", 85)
                put("current_streak", xp.currentStreak)
                put("longest_streak", xp.longestStreak)
                put("total_xp", xp.totalXp)
                put("current_level", xp.currentLevel)
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            conn.disconnect()
            Log.d(tag, "Stats synced to Supabase with response: $code")
            code in 200..299
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync stats: ${e.message}")
            false
        }
    }
}
