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
 * Dispatches real-time Discord webhook notifications when:
 * 1. App is opened/launched (App Launch / Session Start telemetry)
 * 2. Pro license is activated or payment is confirmed
 */
object DiscordNotifier {

    private const val TAG = "DiscordNotifier"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Developer can set their Discord Webhook URL here or via environment/config
    @Volatile
    var webhookUrl: String = "https://discord.com/api/webhooks/1349377473068925000/YOUR_WEBHOOK_TOKEN"

    fun setCustomWebhook(url: String) {
        webhookUrl = url
    }

    /**
     * Notify when the app is launched or user initiates an active focus session.
     */
    fun notifyAppLaunch(context: Context, eventType: String = "App Opened") {
        scope.launch {
            try {
                if (webhookUrl.isBlank() || webhookUrl.contains("YOUR_WEBHOOK_TOKEN")) {
                    Log.d(TAG, "Discord webhook URL is default/unconfigured. Skipping post.")
                    return@launch
                }

                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val deviceModel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"
                val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

                val embed = JSONObject().apply {
                    put("title", "📱 ASR — $eventType")
                    put("description", "A user has active engagement in the ASR Discipline app.")
                    put("color", 0x38BDF8) // Cyan color
                    put("fields", JSONArray().apply {
                        put(JSONObject().put("name", "Device", true).put("value", deviceModel, true))
                        put(JSONObject().put("name", "OS Version", true).put("value", androidVersion, true))
                        put(JSONObject().put("name", "Timestamp", true).put("value", now, true))
                    })
                    put("footer", JSONObject().put("text", "ASR Local-First Engine"))
                }

                val payload = JSONObject().apply {
                    put("username", "ASR Discipline Bot")
                    put("embeds", JSONArray().put(embed))
                }

                sendHttpPost(webhookUrl, payload.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send Discord launch webhook: ${e.message}")
            }
        }
    }

    /**
     * Notify when a user unlocks Pro via Google Play or Web License Key.
     */
    fun notifyProPurchase(planName: String, method: String, licenseKey: String? = null) {
        scope.launch {
            try {
                if (webhookUrl.isBlank() || webhookUrl.contains("YOUR_WEBHOOK_TOKEN")) {
                    Log.d(TAG, "Discord webhook URL is unconfigured. Skipping payment notification.")
                    return@launch
                }

                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                val embed = JSONObject().apply {
                    put("title", "💎 NEW PRO ACTIVATION / PAYMENT!")
                    put("description", "A customer has successfully activated **ASR Pro**.")
                    put("color", 0x10B981) // Emerald Green
                    put("fields", JSONArray().apply {
                        put(JSONObject().put("name", "Plan Tier", true).put("value", planName, true))
                        put(JSONObject().put("name", "Payment Method", true).put("value", method, true))
                        licenseKey?.let {
                            put(JSONObject().put("name", "License Key", true).put("value", it, true))
                        }
                        put(JSONObject().put("name", "Timestamp", true).put("value", now, true))
                    })
                    put("footer", JSONObject().put("text", "ASR Turn Time Into Progress"))
                }

                val payload = JSONObject().apply {
                    put("username", "ASR Revenue & Sales")
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
