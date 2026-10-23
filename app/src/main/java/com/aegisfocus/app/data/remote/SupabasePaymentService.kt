package com.aegisfocus.app.data.remote

import android.util.Log
import com.aegisfocus.app.service.telemetry.DiscordNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PaymentRequest(
    val id: String,
    val userId: String,
    val paymentMethod: String,
    val senderNumber: String,
    val transactionId: String,
    val amount: Double,
    val currency: String = "BDT",
    val planRequested: String,
    val status: String, // PENDING, VERIFIED, REJECTED, CANCELLED
    val rejectionReason: String? = null,
    val createdAt: String? = null,
    val reviewedAt: String? = null,
    val userEmail: String? = null
)

data class PaymentSubmissionResult(
    val success: Boolean,
    val message: String,
    val requestId: String? = null
)

data class AdminActionResult(
    val success: Boolean,
    val message: String
)

data class LicenseRedeemResult(
    val success: Boolean,
    val plan: String? = null,
    val message: String,
    val expiryEpochMs: Long = 0L
)

/** One row of the admin key list. status is UNUSED, USED or REVOKED. */
data class LicenseKeyRow(
    val keyCode: String,
    val tier: String,
    val status: String,
    val redeemedByEmail: String?,
    val createdAt: String?,
    val note: String?
)

data class KeyGenerationResult(
    val success: Boolean,
    val key: String? = null,
    val message: String = ""
)

class SupabasePaymentService(
    private val authService: SupabaseAuthService
) {
    private val tag = "SupabasePaymentService"

    /**
     * Submits a manual bKash/Nagad payment claim to Supabase.
     */
    suspend fun submitPayment(
        paymentMethod: String,
        senderNumber: String,
        transactionId: String,
        amount: Double,
        planRequested: String
    ): PaymentSubmissionResult = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value
            ?: return@withContext PaymentSubmissionResult(false, "User must be logged in to submit payment claim.")

        val token = user.accessToken ?: ""
        val cleanTrx = transactionId.trim().uppercase()
        val cleanSender = senderNumber.trim()

        try {
            // Call submit_payment_request RPC
            val url = URL("${SupabaseConfig.REST_ENDPOINT}/rpc/submit_payment_request")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            if (token.isNotBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val payload = JSONObject().apply {
                put("p_payment_method", paymentMethod)
                put("p_sender_number", cleanSender)
                put("p_transaction_id", cleanTrx)
                put("p_amount", amount)
                put("p_currency", "BDT")
                put("p_plan_requested", planRequested)
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            if (responseCode in 200..299) {
                val requestId = respText.trim().replace("\"", "")
                DiscordNotifier.notifyPaymentSubmitted(
                    email = user.email,
                    method = paymentMethod,
                    transactionId = cleanTrx,
                    senderNumber = cleanSender,
                    amount = amount,
                    planRequested = planRequested,
                    requestId = requestId
                )
                PaymentSubmissionResult(true, "Payment submitted successfully! Status is Pending Verification.", requestId)
            } else {
                val errorMsg = try {
                    val json = JSONObject(respText)
                    json.optString("message", json.optString("error", "Submission failed ($responseCode)"))
                } catch (e: Exception) {
                    "Submission failed ($responseCode): $respText"
                }
                PaymentSubmissionResult(false, errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to submit payment request: ${e.message}", e)
            PaymentSubmissionResult(false, "Network error: ${e.localizedMessage}")
        }
    }

    /**
     * Fetches user's past and current payment claims.
     */
    suspend fun getUserPaymentRequests(): List<PaymentRequest> = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value ?: return@withContext emptyList()
        val token = user.accessToken ?: ""

        try {
            val url = URL("${SupabaseConfig.REST_ENDPOINT}/payment_requests?user_id=eq.${user.id}&order=created_at.desc")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            if (token.isNotBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val respText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                parsePaymentRequestsJson(respText)
            } else {
                conn.disconnect()
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch user payment requests: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Admin: Fetches all PENDING claims across all users.
     */
    suspend fun getAllPendingRequests(): List<PaymentRequest> = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value ?: return@withContext emptyList()
        val token = user.accessToken ?: ""

        try {
            val url = URL("${SupabaseConfig.REST_ENDPOINT}/payment_requests?status=eq.PENDING&order=created_at.asc")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            if (token.isNotBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val respText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                parsePaymentRequestsJson(respText)
            } else {
                conn.disconnect()
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch pending payment requests: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Admin: Approves a payment request and unlocks subscription.
     */
    suspend fun approvePayment(
        requestId: String,
        planType: String,
        userEmail: String,
        transactionId: String
    ): AdminActionResult = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value
            ?: return@withContext AdminActionResult(false, "Admin must be logged in.")
        val token = user.accessToken ?: ""

        try {
            val url = URL("${SupabaseConfig.REST_ENDPOINT}/rpc/approve_payment_request")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val payload = JSONObject().apply {
                put("p_request_id", requestId)
                put("p_plan_type", planType)
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            if (responseCode in 200..299) {
                DiscordNotifier.notifyPaymentApproved(
                    email = userEmail,
                    planRequested = planType,
                    transactionId = transactionId,
                    reviewer = user.email
                )
                AdminActionResult(true, "Payment approved successfully! Pro activated.")
            } else {
                val errorMsg = try {
                    JSONObject(respText).optString("message", respText)
                } catch (e: Exception) {
                    respText
                }
                AdminActionResult(false, "Approval failed: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error approving payment: ${e.message}", e)
            AdminActionResult(false, "Network error: ${e.localizedMessage}")
        }
    }

    /**
     * Admin: Rejects a payment claim with a reason.
     */
    suspend fun rejectPayment(
        requestId: String,
        reason: String,
        userEmail: String,
        transactionId: String
    ): AdminActionResult = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value
            ?: return@withContext AdminActionResult(false, "Admin must be logged in.")
        val token = user.accessToken ?: ""

        try {
            val url = URL("${SupabaseConfig.REST_ENDPOINT}/rpc/reject_payment_request")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val payload = JSONObject().apply {
                put("p_request_id", requestId)
                put("p_reason", reason.ifBlank { "Invalid transaction ID or amount mismatch." })
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            if (responseCode in 200..299) {
                DiscordNotifier.notifyPaymentRejected(
                    email = userEmail,
                    transactionId = transactionId,
                    reason = reason,
                    reviewer = user.email
                )
                AdminActionResult(true, "Payment rejected.")
            } else {
                val errorMsg = try {
                    JSONObject(respText).optString("message", respText)
                } catch (e: Exception) {
                    respText
                }
                AdminActionResult(false, "Rejection failed: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error rejecting payment: ${e.message}", e)
            AdminActionResult(false, "Network error: ${e.localizedMessage}")
        }
    }

    /**
     * Calls a Supabase RPC as the signed-in user.
     * Returns the parsed JSON object, or a JSON object with success=false and an error message.
     */
    private fun callRpc(name: String, payload: JSONObject): JSONObject {
        val user = authService.currentUser.value
            ?: return JSONObject().put("success", false).put("error", "Please sign in first.")
        val token = user.accessToken ?: ""

        return try {
            val conn = URL("${SupabaseConfig.REST_ENDPOINT}/rpc/$name").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()

            if (code in 200..299) {
                try { JSONObject(text) } catch (e: Exception) {
                    JSONObject().put("success", false).put("error", "Unexpected server reply.")
                }
            } else {
                val msg = try { JSONObject(text).optString("message", text) } catch (e: Exception) { text }
                val friendly = if (code == 404 || msg.contains("Could not find the function", ignoreCase = true)) {
                    "Server is not set up for license keys yet."
                } else msg
                JSONObject().put("success", false).put("error", friendly)
            }
        } catch (e: Exception) {
            Log.e(tag, "RPC $name failed: ${e.message}", e)
            JSONObject().put("success", false).put("error", "Network error: ${e.localizedMessage}")
        }
    }

    /**
     * Redeems a license key on the server. The server accepts each key exactly once.
     */
    suspend fun redeemLicenseKey(rawKey: String): LicenseRedeemResult = withContext(Dispatchers.IO) {
        if (authService.currentUser.value == null) {
            return@withContext LicenseRedeemResult(false, null, "Please sign in first to redeem a key.")
        }
        val key = rawKey.trim().uppercase()
        val json = callRpc("redeem_license_key", JSONObject().put("p_key", key))

        if (json.optBoolean("success", false)) {
            val plan = json.optString("plan", "")
            DiscordNotifier.notifyProPurchase(
                planName = plan,
                method = "Server License Key Redemption",
                licenseKey = key
            )
            LicenseRedeemResult(true, plan, json.optString("message", "Pro activated."), json.optLong("expiry_epoch_ms", 0L))
        } else {
            LicenseRedeemResult(false, null, json.optString("error", "Could not redeem this key."))
        }
    }

    /** Admin only: creates a new unused key on the server. */
    suspend fun generateLicenseKey(tier: String, note: String? = null): KeyGenerationResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("p_tier", tier).put("p_note", note ?: JSONObject.NULL)
        val json = callRpc("admin_generate_license_key", payload)
        if (json.optBoolean("success", false)) {
            KeyGenerationResult(true, json.optString("key"), "Key created.")
        } else {
            KeyGenerationResult(false, null, json.optString("error", "Could not create the key."))
        }
    }

    /** Admin only: most recent keys with their status. */
    suspend fun listLicenseKeys(limit: Int = 50): Pair<List<LicenseKeyRow>, String?> = withContext(Dispatchers.IO) {
        val json = callRpc("admin_list_license_keys", JSONObject().put("p_limit", limit))
        if (!json.optBoolean("success", false)) {
            return@withContext emptyList<LicenseKeyRow>() to json.optString("error", "Could not load keys.")
        }
        val array = json.optJSONArray("keys") ?: JSONArray()
        val rows = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            LicenseKeyRow(
                keyCode = o.optString("key_code"),
                tier = o.optString("tier"),
                status = o.optString("status"),
                redeemedByEmail = o.optString("redeemed_by_email").takeIf { it.isNotBlank() && it != "null" },
                createdAt = o.optString("created_at").takeIf { it.isNotBlank() && it != "null" },
                note = o.optString("note").takeIf { it.isNotBlank() && it != "null" }
            )
        }
        rows to null
    }

    /** Admin only: disables a key that has not been used yet. */
    suspend fun revokeLicenseKey(key: String): AdminActionResult = withContext(Dispatchers.IO) {
        val json = callRpc("admin_revoke_license_key", JSONObject().put("p_key", key))
        if (json.optBoolean("success", false)) AdminActionResult(true, "Key revoked.")
        else AdminActionResult(false, json.optString("error", "Could not revoke the key."))
    }

    fun isCurrentUserAdmin(): Boolean {
        val email = authService.currentUser.value?.email ?: return false
        return SupabaseAuthService.isSuperAdmin(email)
    }

    private fun parsePaymentRequestsJson(jsonText: String): List<PaymentRequest> {
        val list = mutableListOf<PaymentRequest>()
        try {
            val array = JSONArray(jsonText)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PaymentRequest(
                        id = obj.getString("id"),
                        userId = obj.getString("user_id"),
                        paymentMethod = obj.optString("payment_method", "bKash"),
                        senderNumber = obj.optString("sender_number", ""),
                        transactionId = obj.optString("transaction_id", ""),
                        amount = obj.optDouble("amount", 0.0),
                        currency = obj.optString("currency", "BDT"),
                        planRequested = obj.optString("plan_requested", "PRO_MONTHLY"),
                        status = obj.optString("status", "PENDING"),
                        rejectionReason = obj.optString("rejection_reason").takeIf { it.isNotBlank() },
                        createdAt = obj.optString("created_at"),
                        reviewedAt = obj.optString("reviewed_at")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing payment requests JSON: ${e.message}", e)
        }
        return list
    }
}
