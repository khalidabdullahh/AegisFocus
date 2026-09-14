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
    val message: String
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
     * Redeems a license key via server-side RPC.
     */
    suspend fun redeemLicenseKey(rawKey: String): LicenseRedeemResult = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value
            ?: return@withContext LicenseRedeemResult(false, null, "Please log in first to redeem license.")
        val token = user.accessToken ?: ""

        try {
            val url = URL("${SupabaseConfig.REST_ENDPOINT}/rpc/redeem_license_key")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val payload = JSONObject().apply {
                put("p_key", rawKey.trim().uppercase())
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            if (responseCode in 200..299) {
                val json = try { JSONObject(respText) } catch (e: Exception) { JSONObject() }
                val plan = json.optString("plan", "PRO")
                DiscordNotifier.notifyProPurchase(
                    planName = plan,
                    method = "Server License Key Redemption",
                    licenseKey = rawKey.trim().uppercase()
                )
                LicenseRedeemResult(true, plan, "License redeemed successfully! Pro activated.")
            } else {
                val err = try {
                    JSONObject(respText).optString("message", respText)
                } catch (e: Exception) {
                    respText
                }
                LicenseRedeemResult(false, null, "Redemption failed: $err")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error redeeming license: ${e.message}", e)
            LicenseRedeemResult(false, null, "Network error: ${e.localizedMessage}")
        }
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
