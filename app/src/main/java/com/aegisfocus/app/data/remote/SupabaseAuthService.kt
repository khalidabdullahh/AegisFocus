package com.aegisfocus.app.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
import com.aegisfocus.app.service.telemetry.DiscordNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val accessToken: String? = null,
    val avatarUri: String? = null
)

sealed class AuthResult {
    data class Success(val user: AuthUser, val message: String) : AuthResult()
    data class EmailVerificationRequired(val email: String, val message: String) : AuthResult()
    data class Error(val message: String, val code: Int? = null) : AuthResult()
}

/**
 * Server-authoritative Supabase Authentication Service.
 * Handles Sign-Up, Sign-In, Google ID-Token & Browser OAuth, Session Persistence, and Token Exchange.
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

        if (!userId.isNullOrBlank() && !email.isNullOrBlank() && !token.isNullOrBlank()) {
            val user = AuthUser(id = userId, email = email, displayName = name, accessToken = token, avatarUri = avatar)
            _currentUser.value = user
            grantSuperAdminIfEligible(email)
        }
    }

    /**
     * Registers a new account in Supabase Auth.
     */
    suspend fun signUp(email: String, pass: String, displayName: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim()

        try {
            val url = URL("${SupabaseConfig.AUTH_ENDPOINT}/signup?redirect_to=${Uri.encode(REDIRECT_URI)}")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val body = JSONObject().apply {
                put("email", cleanEmail)
                put("password", pass)
                put("data", JSONObject().apply {
                    put("display_name", cleanName)
                    put("full_name", cleanName)
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
                val userEmail = userObj.optString("email", cleanEmail)
                val token = json.optString("access_token", "")

                // Check if session token was returned or if email confirmation is required
                if (token.isNotBlank() && userId.isNotBlank()) {
                    val authUser = AuthUser(
                        id = userId,
                        email = userEmail,
                        displayName = cleanName.ifBlank { userEmail.substringBefore("@") },
                        accessToken = token
                    )
                    saveSession(authUser, json.optString("refresh_token", "").ifBlank { null })

                    DiscordNotifier.notifyUserAuth(
                        eventType = "Account Created (Auto-Confirmed)",
                        email = userEmail,
                        username = authUser.displayName ?: ""
                    )
                    AuthResult.Success(authUser, "Account created successfully!")
                } else {
                    // Supabase requires email verification
                    DiscordNotifier.notifyUserAuth(
                        eventType = "Account Created (Verification Sent)",
                        email = userEmail,
                        username = cleanName
                    )
                    AuthResult.EmailVerificationRequired(
                        email = cleanEmail,
                        message = "Verification email sent to $cleanEmail. Please check your inbox and confirm your email before signing in."
                    )
                }
            } else {
                val errorMsg = parseErrorMessage(respText, responseCode)
                Log.w(tag, "Supabase Sign Up failed ($responseCode): $errorMsg")
                AuthResult.Error(errorMsg, responseCode)
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign up network error: ${e.message}", e)
            AuthResult.Error("No internet connection or server timeout. Please check your network and try again.")
        }
    }

    /**
     * Logs into an existing Supabase account.
     */
    suspend fun signIn(email: String, pass: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()

        try {
            val url = URL("${SupabaseConfig.AUTH_ENDPOINT}/token?grant_type=password")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val body = JSONObject().apply {
                put("email", cleanEmail)
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
                val name = userMeta?.optString("display_name")
                    ?: userMeta?.optString("full_name")
                    ?: userMeta?.optString("name")
                    ?: userEmail.substringBefore("@")
                val token = json.getString("access_token")

                val authUser = AuthUser(
                    id = userId,
                    email = userEmail,
                    displayName = name.ifBlank { userEmail.substringBefore("@") },
                    accessToken = token
                )
                saveSession(authUser, json.optString("refresh_token", "").ifBlank { null })

                DiscordNotifier.notifyUserAuth(
                    eventType = "User Signed In",
                    email = userEmail,
                    username = authUser.displayName ?: ""
                )

                AuthResult.Success(authUser, "Welcome back!")
            } else {
                val errorMsg = parseErrorMessage(respText, responseCode)
                Log.w(tag, "Supabase Sign In failed ($responseCode): $errorMsg")
                if (errorMsg.contains("Email not confirmed", ignoreCase = true)) {
                    AuthResult.EmailVerificationRequired(
                        email = cleanEmail,
                        message = "Your email is not verified yet. Please check your inbox for the confirmation link."
                    )
                } else {
                    AuthResult.Error(errorMsg, responseCode)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign in network error: ${e.message}", e)
            AuthResult.Error("No internet connection or server timeout. Please check your connection and try again.")
        }
    }

    /**
     * Signs in or registers a user via Google Credential Manager (ID Token).
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
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

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
                val name = userMeta?.optString("full_name")
                    ?: userMeta?.optString("name")
                    ?: userMeta?.optString("display_name")
                    ?: displayName
                    ?: email.substringBefore("@")
                val token = json.optString("access_token", "")

                val authUser = AuthUser(id = userId, email = email, displayName = name, accessToken = token)
                saveSession(authUser, json.optString("refresh_token", "").ifBlank { null })

                DiscordNotifier.notifyUserAuth(
                    eventType = "Google Sign-In (ID Token)",
                    email = email,
                    username = name
                )

                AuthResult.Success(authUser, "Google Sign-In successful!")
            } else {
                val errorMsg = parseErrorMessage(respText, responseCode)
                Log.w(tag, "Supabase Google ID-token auth failed ($responseCode): $errorMsg")
                AuthResult.Error(errorMsg, responseCode)
            }
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In error: ${e.message}", e)
            AuthResult.Error("Network error during Google Sign-In: ${e.localizedMessage}")
        }
    }

    /**
     * Launches browser-based Google OAuth via Supabase `/auth/v1/authorize`.
     * Redirects back to `aegisfocus://auth-callback`.
     */
    fun launchBrowserGoogleAuth(activityContext: Context) {
        val redirectUri = REDIRECT_URI
        val authUrl = "${SupabaseConfig.AUTH_ENDPOINT}/authorize?provider=google&redirect_to=${Uri.encode(redirectUri)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activityContext.startActivity(intent)
    }

    /**
     * Intercepts and parses the incoming deep link from Google / Email OAuth redirect.
     */
    suspend fun handleOAuthCallback(uri: Uri): AuthResult = withContext(Dispatchers.IO) {
        try {
            // Supabase OAuth returns tokens in URL fragment: #access_token=...&refresh_token=...
            val fragment = uri.fragment
            val query = uri.query
            val params = mutableMapOf<String, String>()

            if (!fragment.isNullOrBlank()) {
                fragment.split("&").forEach { pair ->
                    val parts = pair.split("=")
                    if (parts.size >= 2) {
                        params[parts[0]] = URLDecoder.decode(parts[1], "UTF-8")
                    }
                }
            }

            if (!query.isNullOrBlank()) {
                query.split("&").forEach { pair ->
                    val parts = pair.split("=")
                    if (parts.size >= 2) {
                        params[parts[0]] = URLDecoder.decode(parts[1], "UTF-8")
                    }
                }
            }

            val accessToken = params["access_token"]
            val error = params["error_description"] ?: params["error"]

            if (!error.isNullOrBlank()) {
                return@withContext AuthResult.Error("OAuth error: $error")
            }

            if (accessToken.isNullOrBlank()) {
                return@withContext AuthResult.Error("No access token found in OAuth callback.")
            }

            // Validate token and fetch authentic user profile from Supabase
            val userUrl = URL("${SupabaseConfig.AUTH_ENDPOINT}/user")
            val conn = userUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val respCode = conn.responseCode
            if (respCode in 200..299) {
                val respText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(respText)
                val userId = json.getString("id")
                val email = json.getString("email")
                val userMeta = json.optJSONObject("user_metadata")
                val name = userMeta?.optString("full_name")
                    ?: userMeta?.optString("name")
                    ?: userMeta?.optString("display_name")
                    ?: email.substringBefore("@")

                val authUser = AuthUser(id = userId, email = email, displayName = name, accessToken = accessToken)
                saveSession(authUser, params["refresh_token"])

                DiscordNotifier.notifyUserAuth(
                    eventType = "OAuth Browser Callback Sign-In",
                    email = email,
                    username = name
                )

                AuthResult.Success(authUser, "Signed in successfully!")
            } else {
                conn.disconnect()
                AuthResult.Error("Failed to verify user session with Supabase ($respCode).")
            }
        } catch (e: Exception) {
            Log.e(tag, "OAuth callback handling failed: ${e.message}", e)
            AuthResult.Error("Authentication callback error: ${e.localizedMessage}")
        }
    }

    /**
     * Resends email confirmation link.
     */
    suspend fun resendVerificationEmail(email: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        try {
            val url = URL("${SupabaseConfig.AUTH_ENDPOINT}/resend?redirect_to=${Uri.encode(REDIRECT_URI)}")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val body = JSONObject().apply {
                put("type", "signup")
                put("email", cleanEmail)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            if (responseCode in 200..299) {
                AuthResult.Success(
                    AuthUser("temp", cleanEmail),
                    "Verification email has been resent to $cleanEmail. Please check your inbox."
                )
            } else {
                val errorMsg = parseErrorMessage(respText, responseCode)
                AuthResult.Error(errorMsg, responseCode)
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    /**
     * Signs out and clears stored credentials.
     */
    /**
     * Returns an access token that is valid right now, refreshing it first when it is expired or
     * about to expire. Blocking: call it from a background thread (all remote calls already are).
     */
    @Synchronized
    fun freshAccessToken(): String? {
        val user = _currentUser.value ?: return null
        val token = user.accessToken
        if (!token.isNullOrBlank() && !isJwtExpiring(token)) return token
        return refreshSessionBlocking() ?: token
    }

    private fun isJwtExpiring(jwt: String): Boolean {
        return try {
            val payload = jwt.split(".")[1]
            val bytes = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
            val exp = JSONObject(String(bytes, Charsets.UTF_8)).optLong("exp", 0L)
            exp == 0L || exp - System.currentTimeMillis() / 1000 < 60
        } catch (e: Exception) { false }
    }

    private fun refreshSessionBlocking(): String? {
        val refresh = prefs.getString("auth_refresh_token", null) ?: return null
        val user = _currentUser.value ?: return null
        return try {
            val conn = URL("${SupabaseConfig.AUTH_ENDPOINT}/token?grant_type=refresh_token").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.outputStream.use { it.write(JSONObject().put("refresh_token", refresh).toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code in 200..299) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                conn.disconnect()
                val newToken = json.optString("access_token", "")
                if (newToken.isBlank()) return null
                saveSession(user.copy(accessToken = newToken), json.optString("refresh_token", "").ifBlank { null })
                newToken
            } else {
                conn.disconnect()
                Log.w(tag, "Token refresh failed ($code)")
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Token refresh error: ${e.message}")
            null
        }
    }

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
        val token = user.accessToken ?: return@withContext AuthResult.Error("Invalid session token")

        try {
            val url = URL("${SupabaseConfig.AUTH_ENDPOINT}/user")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("password", newPassword)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            if (responseCode in 200..299) {
                AuthResult.Success(user, "Password updated successfully!")
            } else {
                val err = parseErrorMessage(respText, responseCode)
                AuthResult.Error(err, responseCode)
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        try {
            val url = URL("${SupabaseConfig.AUTH_ENDPOINT}/recover")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("email", cleanEmail)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val currentUserVal = _currentUser.value ?: AuthUser("temp", cleanEmail)
            if (responseCode in 200..299) {
                AuthResult.Success(currentUserVal, "Password reset email sent to $cleanEmail!")
            } else {
                val err = parseErrorMessage(respText, responseCode)
                AuthResult.Error(err, responseCode)
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    fun updateAvatarUri(uri: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(avatarUri = uri)
        saveSession(updated)
    }

    private fun saveSession(user: AuthUser, refreshToken: String? = null) {
        prefs.edit()
            .apply { if (!refreshToken.isNullOrBlank()) putString("auth_refresh_token", refreshToken) }
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

    private fun parseErrorMessage(respText: String, code: Int): String {
        return try {
            val json = JSONObject(respText)
            json.optString("error_description",
                json.optString("msg",
                    json.optString("message",
                        json.optString("error", "Request failed with status $code")
                    )
                )
            )
        } catch (e: Exception) {
            "Request failed ($code): $respText"
        }
    }

    companion object {
        const val REDIRECT_URI = "aegisfocus://auth-callback"

        val SUPER_ADMIN_EMAILS = setOf(
            "seamafridi1234567890@gmail.com"
        )

        fun isSuperAdmin(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            return email.trim().lowercase() in SUPER_ADMIN_EMAILS
        }
    }
}
