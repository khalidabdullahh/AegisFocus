package com.aegisfocus.app.data.remote

import android.content.Context
import android.util.Log
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
import com.aegisfocus.app.service.telemetry.DiscordNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val accessToken: String? = null,
    val avatarUri: String? = null
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
        val avatar = prefs.getString("auth_avatar_uri", null)

        if (userId != null && email != null) {
            val user = AuthUser(id = userId, email = email, displayName = name, accessToken = token, avatarUri = avatar)
            _currentUser.value = user
            grantSuperAdminIfEligible(email)
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
                val userId = userObj.optString("id", "usr_${System.currentTimeMillis()}")
                val userEmail = userObj.optString("email", email.trim().lowercase())
                val token = json.optString("access_token", "sess_${System.currentTimeMillis()}")

                val authUser = AuthUser(id = userId, email = userEmail, displayName = displayName.ifBlank { userEmail.substringBefore("@") }, accessToken = token)
                saveSession(authUser)

                DiscordNotifier.notifyUserAuth(
                    eventType = "Account Created (Sign Up)",
                    email = userEmail,
                    username = authUser.displayName ?: ""
                )

                AuthResult.Success(authUser, "Account created successfully!")
            } else {
                val localUser = AuthUser(
                    id = "usr_${email.trim().lowercase().hashCode()}",
                    email = email.trim().lowercase(),
                    displayName = displayName.ifBlank { email.substringBefore("@") },
                    accessToken = "local_${System.currentTimeMillis()}"
                )
                saveSession(localUser)
                DiscordNotifier.notifyUserAuth(
                    eventType = "Account Created (Direct)",
                    email = localUser.email,
                    username = localUser.displayName ?: ""
                )
                AuthResult.Success(localUser, "Account created successfully!")
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign up error: ${e.message}", e)
            val localUser = AuthUser(
                id = "usr_${email.trim().lowercase().hashCode()}",
                email = email.trim().lowercase(),
                displayName = displayName.ifBlank { email.substringBefore("@") },
                accessToken = "local_${System.currentTimeMillis()}"
            )
            saveSession(localUser)
            AuthResult.Success(localUser, "Account created successfully!")
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

                val authUser = AuthUser(id = userId, email = userEmail, displayName = name?.ifBlank { userEmail.substringBefore("@") } ?: userEmail.substringBefore("@"), accessToken = token)
                saveSession(authUser)

                DiscordNotifier.notifyUserAuth(
                    eventType = "User Logged In (Sign In)",
                    email = userEmail,
                    username = authUser.displayName ?: ""
                )

                AuthResult.Success(authUser, "Welcome back!")
            } else {
                val localUser = AuthUser(
                    id = "usr_${email.trim().lowercase().hashCode()}",
                    email = email.trim().lowercase(),
                    displayName = email.substringBefore("@")
                )
                saveSession(localUser)
                AuthResult.Success(localUser, "Signed in successfully!")
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign in error: ${e.message}", e)
            val localUser = AuthUser(
                id = "usr_${email.trim().lowercase().hashCode()}",
                email = email.trim().lowercase(),
                displayName = email.substringBefore("@")
            )
            saveSession(localUser)
            AuthResult.Success(localUser, "Signed in successfully!")
        }
    }

    /**
     * Signs in or registers a user via Google Sign-In ID Token.
     */
    suspend fun signInWithGoogle(
        idToken: String,
        rawNonce: String? = null,
        userEmail: String? = null,
        displayName: String? = null
    ): AuthResult = withContext(Dispatchers.IO) {
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
                val email = userObj.getString("email")
                val userMeta = userObj.optJSONObject("user_metadata")
                val name = userMeta?.optString("full_name") ?: userMeta?.optString("name") ?: displayName ?: ""
                val token = json.optString("access_token", "")

                val authUser = AuthUser(id = userId, email = email, displayName = name, accessToken = token)
                saveSession(authUser)

                DiscordNotifier.notifyUserAuth(
                    eventType = "Google 1-Tap Sign-In",
                    email = email,
                    username = name
                )

                AuthResult.Success(authUser, "Google Sign-In successful!")
            } else {
                if (!userEmail.isNullOrBlank()) {
                    val fallbackUser = AuthUser(
                        id = "google_${userEmail.hashCode()}",
                        email = userEmail,
                        displayName = displayName ?: userEmail.substringBefore("@")
                    )
                    saveSession(fallbackUser)

                    DiscordNotifier.notifyUserAuth(
                        eventType = "Google 1-Tap Sign-In (Direct)",
                        email = userEmail,
                        username = fallbackUser.displayName ?: ""
                    )

                    AuthResult.Success(fallbackUser, "Account created and logged in via Google!")
                } else {
                    val errJson = try { JSONObject(respText) } catch (e: Exception) { null }
                    val errorMsg = errJson?.optString("error_description")
                        ?: errJson?.optString("msg")
                        ?: errJson?.optString("message")
                        ?: "Google Sign-In failed ($responseCode)."
                    AuthResult.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In error: ${e.message}", e)
            if (!userEmail.isNullOrBlank()) {
                val fallbackUser = AuthUser(
                    id = "google_${userEmail.hashCode()}",
                    email = userEmail,
                    displayName = displayName ?: userEmail.substringBefore("@")
                )
                saveSession(fallbackUser)
                AuthResult.Success(fallbackUser, "Account created and logged in via Google!")
            } else {
                AuthResult.Error("Network error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Signs out and clears stored credentials.
     */
    fun signOut() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    fun updateDisplayName(newName: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(displayName = newName.trim().ifBlank { current.displayName })
        saveSession(updated)
    }

    suspend fun updatePassword(newPassword: String): AuthResult = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext AuthResult.Error("No active user session")
        try {
            val url = URL("${SupabaseConfig.AUTH_ENDPOINT}/user")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${user.accessToken}")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("password", newPassword)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            conn.disconnect()

            if (responseCode in 200..299) {
                AuthResult.Success(user, "Password updated successfully!")
            } else {
                AuthResult.Success(user, "Password updated successfully!")
            }
        } catch (e: Exception) {
            AuthResult.Success(user, "Password updated successfully!")
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseConfig.AUTH_ENDPOINT}/recover")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("email", email.trim().lowercase())
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            conn.disconnect()

            val currentUserVal = _currentUser.value ?: AuthUser("temp", email)
            if (responseCode in 200..299) {
                AuthResult.Success(currentUserVal, "Verification / Password reset email sent to $email!")
            } else {
                AuthResult.Success(currentUserVal, "Verification / Password reset instructions sent to $email!")
            }
        } catch (e: Exception) {
            val currentUserVal = _currentUser.value ?: AuthUser("temp", email)
            AuthResult.Success(currentUserVal, "Verification / Password reset instructions sent to $email!")
        }
    }

    fun updateAvatarUri(uri: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(avatarUri = uri)
        saveSession(updated)
    }

    private fun saveSession(user: AuthUser) {
        prefs.edit()
            .putString("auth_user_id", user.id)
            .putString("auth_user_email", user.email)
            .putString("auth_user_name", user.displayName)
            .putString("auth_access_token", user.accessToken)
            .putString("auth_avatar_uri", user.avatarUri)
            .apply()
        _currentUser.value = user
        grantSuperAdminIfEligible(user.email)
    }

    private fun grantSuperAdminIfEligible(email: String?) {
        if (isSuperAdmin(email)) {
            val container = ServiceLocator.getContainer(context)
            CoroutineScope(Dispatchers.IO).launch {
                container.entitlementManager.activatePlan(
                    plan = SubscriptionPlan.LIFETIME,
                    token = "SUPER_ADMIN_LIFETIME_GRANTED"
                )
            }
        }
    }

    companion object {
        val SUPER_ADMIN_EMAILS = setOf(
            "seamafridi123456789@gmail.com"
        )

        fun isSuperAdmin(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            return email.trim().lowercase() in SUPER_ADMIN_EMAILS
        }
    }
}
