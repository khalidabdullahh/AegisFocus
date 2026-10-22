package com.aegisfocus.app.service.telemetry

import android.os.Build
import android.util.Log
import com.aegisfocus.app.data.remote.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Dispatches real-time telemetry securely through the backend proxy.
 *
 * Security Architecture:
 * Client (ASAR Android) -> Supabase Edge Function (/functions/v1/discord-telemetry) -> Discord Webhook
 *
 * No Discord Webhook URLs or private secrets are ever stored in client code.
 */
object DiscordNotifier {

    private const val TAG = "DiscordNotifier"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Backend endpoint that securely relays notifications to Discord
    private val endpointUrl: String
        get() = "${SupabaseConfig.SUPABASE_URL}/functions/v1/discord-telemetry"

    /**
     * Notify when a user registers or logs into their account.
     * @param eventType "SIGN_UP" or "SIGN_IN"
     */
    fun notifyUserAuth(eventType: String, email: String, username: String? = null) {
        scope.launch {
            try {
                val deviceModel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"
                val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                val isSignUp = eventType.contains("Create", ignoreCase = true) || eventType.contains("Register", ignoreCase = true)

                val embed = JSONObject().apply {
                    put("title", if (isSignUp) "New User Account Created" else "User Signed In")
                    put("color", if (isSignUp) 3066993 else 3447003)
                    put("fields", JSONArray().apply {
                        put(JSONObject().apply { put("name", "Event"); put("value", eventType); put("inline", true) })
                        put(JSONObject().apply { put("name", "Email"); put("value", email); put("inline", true) })
                        put(JSONObject().apply { put("name", "Username"); put("value", username ?: email.substringBefore("@")); put("inline", true) })
                        put(JSONObject().apply { put("name", "Device"); put("value", deviceModel); put("inline", false) })
                        put(JSONObject().apply { put("name", "OS"); put("value", androidVersion); put("inline", true) })
                    })
                }

                val payload = JSONObject().apply {
                    put("eventType", if (isSignUp) "SIGN_UP" else "SIGN_IN")
                    put("email", email)
                    username?.let { put("username", it) }
                    put("device", deviceModel)
                    put("osVersion", androidVersion)
                    put("discordEmbed", embed)
                }

                sendHttpPost(payload.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Telemetry dispatch omitted: ${e.message}")
            }
        }
    }

    /**
     * Notify when a user makes a payment or unlocks Pro via license key.
     */
    fun notifyProPurchase(planName: String, method: String, licenseKey: String? = null, amount: String? = null) {
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("eventType", "PRO_PURCHASE")
                    put("planName", planName)
                    put("method", method)
                    licenseKey?.let { put("licenseKey", it) }
                    amount?.let { put("amount", it) }
                }

                sendHttpPost(payload.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Telemetry dispatch omitted: ${e.message}")
            }
        }
    }

    /**
     * Notify admin queue when a customer submits a manual payment claim (bKash/Nagad).
     */
    fun notifyPaymentSubmitted(
        email: String,
        method: String,
        transactionId: String,
        senderNumber: String,
        amount: Double,
        planRequested: String,
        requestId: String? = null
    ) {
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("eventType", "PAYMENT_SUBMITTED")
                    put("email", email)
                    put("method", method)
                    put("transactionId", transactionId)
                    put("senderNumber", senderNumber)
                    put("amount", amount)
                    put("planRequested", planRequested)
                    requestId?.let { put("requestId", it) }
                }
                sendHttpPost(payload.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Telemetry dispatch omitted: ${e.message}")
            }
        }
    }

    /**
     * Notify channel when an admin approves a payment claim.
     */
    fun notifyPaymentApproved(
        email: String,
        planRequested: String,
        transactionId: String,
        reviewer: String
    ) {
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("eventType", "PAYMENT_APPROVED")
                    put("email", email)
                    put("planRequested", planRequested)
                    put("transactionId", transactionId)
                    put("reviewer", reviewer)
                }
                sendHttpPost(payload.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Telemetry dispatch omitted: ${e.message}")
            }
        }
    }

    /**
     * Notify channel when an admin rejects a payment claim.
     */
    fun notifyPaymentRejected(
        email: String,
        transactionId: String,
        reason: String,
        reviewer: String
    ) {
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("eventType", "PAYMENT_REJECTED")
                    put("email", email)
                    put("transactionId", transactionId)
                    put("reason", reason)
                    put("reviewer", reviewer)
                }
                sendHttpPost(payload.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Telemetry dispatch omitted: ${e.message}")
            }
        }
    }

    private fun sendHttpPost(jsonBody: String) {
        try {
            val url = URL(endpointUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("User-Agent", "ASAR-Android/1.0")
            conn.doOutput = true
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            conn.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            Log.d(TAG, "Backend telemetry relay response: $code")
            conn.disconnect()
        } catch (e: Exception) {
            Log.d(TAG, "Backend telemetry unavailable (offline or non-blocking): ${e.message}")
        }
    }
}
