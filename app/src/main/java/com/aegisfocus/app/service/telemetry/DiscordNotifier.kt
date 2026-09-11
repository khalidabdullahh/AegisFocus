package com.aegisfocus.app.service.telemetry

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dispatches real-time Discord webhook notifications exclusively for:
 * 1. User Account Creation / Sign-In (Auth Events)
 * 2. Pro License Purchases / Payment Confirmations (Revenue Events)
 */
object DiscordNotifier {

    private const val TAG = "DiscordNotifier"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Discord Webhook URL for #aegis-focus
    @Volatile
    var webhookUrl: String = "https://discord.com/api/webhooks/1547850672987447326/5ac4mp72Zb479k18tQe8LnCyXpQBjekjX8R2w-Bc7sT9njpAwm40x8K5p4wULjDBWvcS"

    /**
     * Notify when a user registers or logs into their account.
     * @param eventType "Account Created" or "User Login"
     */
    fun notifyUserAuth(eventType: String, email: String, username: String? = null) {
        scope.launch {
            try {
                if (webhookUrl.isBlank()) return@launch

                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val deviceModel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"
                val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

                val isNewAccount = eventType.contains("Create", ignoreCase = true) || eventType.contains("Register", ignoreCase = true)
                val embedColor = if (isNewAccount) 0x10B981 else 0x38BDF8 // Green for new user, Cyan for login

                val embed = JSONObject().apply {
                    put("title", if (isNewAccount) "🎉 New User Registered!" else "👤 User Logged In")
                    put("description", "User authenticated in ASR Focus app.")
                    put("color", embedColor)
                    put("fields", JSONArray().apply {
                        put(JSONObject().put("name", "Email", true).put("value", email, true))
                        username?.let {
                            put(JSONObject().put("name", "Username", true).put("value", it, true))
                        }
                        put(JSONObject().put("name", "Auth Event", true).put("value", eventType, true))
                        put(JSONObject().put("name", "Device", true).put("value", deviceModel, true))
                        put(JSONObject().put("name", "OS Version", true).put("value", androidVersion, true))
                        put(JSONObject().put("name", "Timestamp", true).put("value", now, true))
                    })
                    put("footer", JSONObject().put("text", "ASR Auth & User Activity"))
                }

                val payload = JSONObject().apply {
                    put("username", "ASR User Monitor")
                    put("embeds", JSONArray().put(embed))
                }

                sendHttpPost(webhookUrl, payload.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send Discord auth webhook: ${e.message}")
            }
        }
    }

    /**
     * Notify when a user makes a payment or unlocks Pro via license key.
     */
    fun notifyProPurchase(planName: String, method: String, licenseKey: String? = null, amount: String? = null) {
        scope.launch {
            try {
                if (webhookUrl.isBlank()) return@launch

                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                val embed = JSONObject().apply {
                    put("title", "💎 NEW PRO ACTIVATION / PAYMENT CONFIRMED!")
                    put("description", "A customer has successfully purchased or activated **ASR Pro**.")
                    put("color", 0xF59E0B) // Amber/Gold color
                    put("fields", JSONArray().apply {
                        put(JSONObject().put("name", "Plan Tier", true).put("value", planName, true))
                        put(JSONObject().put("name", "Payment Method", true).put("value", method, true))
                        amount?.let {
                            put(JSONObject().put("name", "Amount", true).put("value", it, true))
                        }
                        licenseKey?.let {
                            put(JSONObject().put("name", "License Key", true).put("value", it, true))
                        }
                        put(JSONObject().put("name", "Timestamp", true).put("value", now, true))
                    })
                    put("footer", JSONObject().put("text", "ASR Turn Time Into Progress — Revenue"))
                }

                val payload = JSONObject().apply {
                    put("username", "ASR Revenue Bot")
                    put("embeds", JSONArray().put(embed))
                }

                sendHttpPost(webhookUrl, payload.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send Discord payment webhook: ${e.message}")
            }
        }
    }

    private fun sendHttpPost(urlStr: String, jsonBody: String) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("User-Agent", "ASR-Android/1.0")
        conn.doOutput = true
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        conn.outputStream.use { os ->
            os.write(jsonBody.toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val code = conn.responseCode
        Log.d(TAG, "Discord webhook dispatched with response code: $code")
        conn.disconnect()
    }
}
