package com.aegisfocus.app.data.remote

import android.content.Context
import android.util.Log
import com.aegisfocus.app.service.telemetry.DiscordNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val accessToken: String? = null
)

sealed class AuthResult {
    data class Success(val user: AuthUser, val message: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Native Supabase Authentication Service handling SignUp, SignIn, and User Sessions.
 */
class SupabaseAuthService(private val context: Context) {

    private val tag = "SupabaseAuthService"
    private val prefs = context.getSharedPreferences("aegis_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val userId = prefs.getString("auth_user_id", null)
        val email = prefs.getString("auth_user_email", null)
        val name = prefs.getString("auth_user_name", null)
        val token = prefs.getString("auth_access_token", null)

        if (userId != null && email != null) {
            _currentUser.value = AuthUser(id = userId, email = email, displayName = name, accessToken = token)
        }
    }

    /**
     * Registers a new account in Supabase Auth.
     */
    suspend fun signUp(email: String, pass: String, displayName: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseConfig.AUTH_ENDPOINT}/signup")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val body = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", pass)
                put("data", JSONObject().apply {
                    put("display_name", displayName.trim())
                })
            }

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            if (responseCode in 200..299) {
                val json = JSONObject(respText)
                val userObj = json.optJSONObject("user") ?: json
                val userId = userObj.optString("id", "")
                val userEmail = userObj.optString("email", email)
                val token = json.optString("access_token", "")

                val authUser = AuthUser(id = userId, email = userEmail, displayName = displayName, accessToken = token)
                saveSession(authUser)

                // Dispatch real-time Discord alert
                DiscordNotifier.notifyUserAuth(
                    eventType = "Account Created (Sign Up)",
                    email = userEmail,
                    username = displayName
                )

                AuthResult.Success(authUser, "Account created successfully!")
            } else {
                val errJson = try { JSONObject(respText) } catch (e: Exception) { null }
                val errorMsg = errJson?.optString("msg")
                    ?: errJson?.optString("message")
                    ?: errJson?.optString("error_description")
                    ?: "Sign up failed ($responseCode)"
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign up error: ${e.message}", e)
            AuthResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    /**
     * Logs into existing Supabase account.
     */
    suspend fun signIn(email: String, pass: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseConfig.AUTH_ENDPOINT}/token?grant_type=password")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val body = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", pass)
            }

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            if (responseCode in 200..299) {
                val json = JSONObject(respText)
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val userEmail = userObj.getString("email")
                val userMeta = userObj.optJSONObject("user_metadata")
                val name = userMeta?.optString("display_name", "")
                val token = json.getString("access_token")

                val authUser = AuthUser(id = userId, email = userEmail, displayName = name, accessToken = token)
                saveSession(authUser)

                // Dispatch real-time Discord alert
                DiscordNotifier.notifyUserAuth(
                    eventType = "User Logged In (Sign In)",
                    email = userEmail,
                    username = name
                )

                AuthResult.Success(authUser, "Welcome back!")
            } else {
                val errJson = try { JSONObject(respText) } catch (e: Exception) { null }
                val errorMsg = errJson?.optString("error_description")
                    ?: errJson?.optString("msg")
                    ?: errJson?.optString("message")
                    ?: "Invalid email or password."
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign in error: ${e.message}", e)
            AuthResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    /**
     * Signs in or registers a user via Google Sign-In ID Token.
     */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String? = null): AuthResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseConfig.AUTH_ENDPOINT}/token?grant_type=id_token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val body = JSONObject().apply {
                put("provider", "google")
                put("id_token", idToken)
                if (rawNonce != null) put("nonce", rawNonce)
            }

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            if (responseCode in 200..299) {
                val json = JSONObject(respText)
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val userEmail = userObj.getString("email")
                val userMeta = userObj.optJSONObject("user_metadata")
                val name = userMeta?.optString("full_name") ?: userMeta?.optString("name") ?: ""
                val token = json.getString("access_token")

                val authUser = AuthUser(id = userId, email = userEmail, displayName = name, accessToken = token)
                saveSession(authUser)

                DiscordNotifier.notifyUserAuth(
                    eventType = "Google 1-Tap Sign-In",
                    email = userEmail,
                    username = name
                )

                AuthResult.Success(authUser, "Google Sign-In successful!")
            } else {
                val errJson = try { JSONObject(respText) } catch (e: Exception) { null }
                val errorMsg = errJson?.optString("error_description")
                    ?: errJson?.optString("msg")
                    ?: errJson?.optString("message")
                    ?: "Google Sign-In failed ($responseCode)."
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In error: ${e.message}", e)
            AuthResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    /**
     * Signs out and clears stored credentials.
     */
    fun signOut() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    private fun saveSession(user: AuthUser) {
        prefs.edit()
            .putString("auth_user_id", user.id)
            .putString("auth_user_email", user.email)
            .putString("auth_user_name", user.displayName)
            .putString("auth_access_token", user.accessToken)
            .apply()
        _currentUser.value = user
    }
}
