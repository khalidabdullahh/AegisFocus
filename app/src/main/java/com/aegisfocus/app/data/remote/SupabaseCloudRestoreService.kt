package com.aegisfocus.app.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Brings the user's Pro plan, name and photo back after a reinstall or on a new phone, and keeps
 * the name and photo saved in the cloud. Pro is read from the server's `subscriptions` table
 * (the single source of truth); name and photo come from the user's `profiles` row.
 */
class SupabaseCloudRestoreService(
    private val context: Context,
    private val authService: SupabaseAuthService,
    private val entitlementManager: EntitlementManager
) {
    private val tag = "CloudRestore"

    /** What the last server call answered, so the screen can show the real reason when something fails. */
    @Volatile
    var lastDetail: String = ""
        private set

    suspend fun restoreAll() = withContext(Dispatchers.IO) {
        try {
            restoreSubscription()
            restoreProfile()
        } catch (e: Exception) {
            Log.w(tag, "Restore failed: ${e.message}")
        }
    }

    private suspend fun restoreSubscription() {
        val user = authService.currentUser.value ?: return
        val response = request("GET", "subscriptions?user_id=eq.${user.id}&select=plan_type,status,expiry_epoch_ms", null)
            ?: return
        if (response.first !in 200..299) return

        val rows = JSONArray(response.second)
        if (rows.length() == 0) return
        val row = rows.getJSONObject(0)
        if (row.optString("status") != "active") return

        val plan = SubscriptionPlan.values().firstOrNull { it.name == row.optString("plan_type") } ?: return
        if (plan == SubscriptionPlan.FREE) return

        val expiry = row.optLong("expiry_epoch_ms", 0L)
        val expired = plan != SubscriptionPlan.LIFETIME && expiry > 0L && expiry <= System.currentTimeMillis()
        if (expired) return

        entitlementManager.activatePlan(plan, expiry, "")
    }

    private fun restoreProfile() {
        val user = authService.currentUser.value ?: return

        request("GET", "profiles?id=eq.${user.id}&select=display_name", null)?.let { (code, text) ->
            if (code in 200..299) {
                val rows = JSONArray(text)
                val name = if (rows.length() > 0) rows.getJSONObject(0).optString("display_name", "") else ""
                if (name.isNotBlank() && name != user.displayName) {
                    authService.updateDisplayName(name, sync = false)
                }
            }
        }

        // Photo is requested on its own so a missing column never breaks the name restore
        val localAvatar = authService.currentUser.value?.avatarUri
        if (localAvatar != null && File(localAvatar).exists()) return
        request("GET", "profiles?id=eq.${user.id}&select=avatar_b64", null)?.let { (code, text) ->
            if (code in 200..299) {
                val rows = JSONArray(text)
                val encoded = if (rows.length() > 0) rows.getJSONObject(0).optString("avatar_b64", "") else ""
                if (encoded.isNotBlank() && encoded != "null") {
                    val bytes = Base64.decode(encoded, Base64.DEFAULT)
                    val file = File(context.filesDir, "user_avatar_profile.jpg")
                    file.writeBytes(bytes)
                    authService.updateAvatarUri(file.absolutePath, sync = false)
                }
            }
        }
    }

    /** Returns (reachable, json). reachable is false when the server column is missing or there is no network. */
    suspend fun fetchSettingsJson(): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value ?: return@withContext Pair(false, null)
        val response = request("GET", "profiles?id=eq.${user.id}&select=settings_json", null)
            ?: return@withContext Pair(false, null)
        if (response.first !in 200..299) return@withContext Pair(false, null)
        val rows = JSONArray(response.second)
        val text = if (rows.length() > 0) rows.getJSONObject(0).optString("settings_json", "") else ""
        Pair(true, text.takeIf { it.isNotBlank() && it != "null" })
    }

    suspend fun pushSettingsJson(json: String): Boolean = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value ?: return@withContext false
        val body = JSONObject().put("settings_json", json).toString()
        val response = request("PATCH", "profiles?id=eq.${user.id}", body)
        response != null && response.first in 200..299
    }

    suspend fun pushDisplayName(name: String) = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value ?: return@withContext
        val body = JSONObject().put("display_name", name).toString()
        request("PATCH", "profiles?id=eq.${user.id}", body)
    }

    suspend fun pushAvatar(path: String) = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value ?: return@withContext
        try {
            val source = BitmapFactory.decodeFile(path) ?: return@withContext
            val longest = maxOf(source.width, source.height)
            val scale = if (longest > MAX_AVATAR_PX) MAX_AVATAR_PX.toFloat() / longest else 1f
            val scaled = Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true
            )
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            val body = JSONObject().put("avatar_b64", encoded).toString()
            request("PATCH", "profiles?id=eq.${user.id}", body)
        } catch (e: Exception) {
            Log.w(tag, "Avatar upload failed: ${e.message}")
        }
    }

    private fun request(method: String, path: String, body: String?): Pair<Int, String>? {
        val token = authService.freshAccessToken()
        if (token == null) {
            lastDetail = "not signed in or the session expired"
            return null
        }
        return try {
            val conn = URL("${SupabaseConfig.REST_ENDPOINT}/$path").openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            if (body != null) {
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            lastDetail = if (code in 200..299) "" else "server said $code: ${text.take(160)}"
            Pair(code, text)
        } catch (e: Exception) {
            Log.w(tag, "$method $path failed: ${e.message}")
            lastDetail = "${e.javaClass.simpleName}: ${e.message}"
            null
        }
    }

    companion object {
        private const val MAX_AVATAR_PX = 256
    }
}
