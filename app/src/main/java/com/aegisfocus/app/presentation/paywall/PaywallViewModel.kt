package com.aegisfocus.app.presentation.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.billing.PlayBillingManager
import com.aegisfocus.app.data.remote.PaymentRequest
import com.aegisfocus.app.data.remote.SupabasePaymentService
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
import com.aegisfocus.app.service.telemetry.DiscordNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections

data class PaywallUiState(
    val currentPlan: SubscriptionPlan = SubscriptionPlan.FREE,
    val selectedOption: PaywallOption = PaywallOption.YEARLY,
    val isLoading: Boolean = false,
    val purchaseSucceeded: Boolean = false,
    val errorMessage: String? = null,
    val submissionSuccessMessage: String? = null,
    val myClaims: List<PaymentRequest> = emptyList()
)

enum class PaywallOption(val label: String, val price: String, val perMonth: String, val planType: SubscriptionPlan) {
    MONTHLY("Monthly", "$2.99", "$2.99/mo", SubscriptionPlan.PRO_MONTHLY),
    YEARLY("Yearly", "$19.99", "$1.67/mo", SubscriptionPlan.PRO_YEARLY),
    LIFETIME("Lifetime", "$49.99", "One-time", SubscriptionPlan.LIFETIME),
    STUDENT("Student Yearly", "$9.99", "$0.83/mo", SubscriptionPlan.STUDENT_YEARLY)
}

class PaywallViewModel(
    private val entitlementManager: EntitlementManager,
    private val playBillingManager: PlayBillingManager? = null,
    private val paymentService: SupabasePaymentService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        playBillingManager?.startConnection()
        viewModelScope.launch {
            entitlementManager.activePlan.collect { plan ->
                _uiState.value = _uiState.value.copy(
                    currentPlan = plan,
                    purchaseSucceeded = plan != SubscriptionPlan.FREE
                )
            }
        }
        loadUserClaims()
    }

    fun loadUserClaims() {
        if (paymentService == null) return
        viewModelScope.launch {
            val claims = paymentService.getUserPaymentRequests()
            _uiState.value = _uiState.value.copy(myClaims = claims)
        }
    }

    fun selectOption(option: PaywallOption) {
        _uiState.value = _uiState.value.copy(selectedOption = option)
    }

    /**
     * Initiates Google Play purchase flow or falls back gracefully in offline/demo mode.
     */
    fun purchaseSelected(activity: Activity? = null) {
        val option = _uiState.value.selectedOption
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, submissionSuccessMessage = null)

        if (activity != null && playBillingManager != null) {
            playBillingManager.launchPurchase(activity, option.planType) { success, error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    purchaseSucceeded = success,
                    errorMessage = error
                )
            }
        } else {
            // Local fallback
            viewModelScope.launch {
                try {
                    val expiryMs = when (option.planType) {
                        SubscriptionPlan.PRO_MONTHLY -> System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                        SubscriptionPlan.PRO_YEARLY, SubscriptionPlan.STUDENT_YEARLY -> System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
                        SubscriptionPlan.LIFETIME -> 0L
                        else -> 0L
                    }
                    entitlementManager.activatePlan(option.planType, expiryMs, "local_demo_token")
                    _uiState.value = _uiState.value.copy(isLoading = false, purchaseSucceeded = true)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Purchase failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun restorePurchases() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, submissionSuccessMessage = null)
        playBillingManager?.restorePurchases { restored ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                purchaseSucceeded = restored,
                errorMessage = if (!restored) "No active subscriptions found for this account." else null
            )
        } ?: run {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Purchases up to date.")
        }
    }

    fun redeemLicenseKey(rawKey: String) {
        val cleanKey = rawKey.trim().uppercase()
        if (cleanKey.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid license key.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, submissionSuccessMessage = null)

            // Try backend RPC validation first
            if (paymentService != null) {
                val serverResult = paymentService.redeemLicenseKey(cleanKey)
                if (serverResult.success) {
                    val plan = when (serverResult.plan?.uppercase()) {
                        "PRO_MONTHLY" -> SubscriptionPlan.PRO_MONTHLY
                        "PRO_YEARLY" -> SubscriptionPlan.PRO_YEARLY
                        "STUDENT_YEARLY" -> SubscriptionPlan.STUDENT_YEARLY
                        else -> SubscriptionPlan.LIFETIME
                    }
                    val expiryMs = when (plan) {
                        SubscriptionPlan.PRO_MONTHLY -> System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                        SubscriptionPlan.PRO_YEARLY, SubscriptionPlan.STUDENT_YEARLY -> System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
                        else -> 0L
                    }
                    entitlementManager.activatePlan(plan, expiryMs, token = "license_server:$cleanKey")
                    _uiState.value = _uiState.value.copy(isLoading = false, purchaseSucceeded = true)
                    return@launch
                }
            }

            // Fallback to local cryptographic validator
            val localResult = com.aegisfocus.app.domain.subscription.LicenseKeyValidator.validateKey(cleanKey)
            if (!localResult.isValid) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = localResult.message)
                return@launch
            }

            val expiryMs = if (localResult.durationDays > 0) {
                System.currentTimeMillis() + localResult.durationDays.toLong() * 24 * 60 * 60 * 1000
            } else {
                0L
            }

            entitlementManager.activatePlan(localResult.plan, expiryMs, token = "license_key:$cleanKey")
            DiscordNotifier.notifyProPurchase(
                planName = localResult.plan.displayName,
                method = "Web License Redemption",
                licenseKey = cleanKey
            )

            _uiState.value = _uiState.value.copy(isLoading = false, purchaseSucceeded = true)
        }
    }

    fun submitBkashPayment(rawTrxId: String, senderPhone: String, userEmail: String? = null) {
        val cleanTrxId = rawTrxId.trim().uppercase()
        val cleanPhone = senderPhone.trim()

        if (cleanTrxId.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid bKash/Nagad Transaction ID (e.g. 9A2B3C4D)")
            return
        }

        if (cleanPhone.length < 10) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid sender mobile number.")
            return
        }

        if (usedTrxIds.contains(cleanTrxId)) {
            _uiState.value = _uiState.value.copy(errorMessage = "⚠️ This Transaction ID ($cleanTrxId) has already been submitted.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, submissionSuccessMessage = null)
            usedTrxIds.add(cleanTrxId)

            val option = _uiState.value.selectedOption
            val plan = option.planType
            val amountBdt = when (option) {
                PaywallOption.MONTHLY -> 300.0
                PaywallOption.YEARLY -> 1500.0
                PaywallOption.LIFETIME -> 2500.0
                PaywallOption.STUDENT -> 800.0
            }

            if (paymentService != null) {
                val result = paymentService.submitPayment(
                    paymentMethod = "bKash / Nagad",
                    senderNumber = cleanPhone,
                    transactionId = cleanTrxId,
                    amount = amountBdt,
                    planRequested = plan.name
                )
                if (result.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        submissionSuccessMessage = "✅ Payment claim submitted! Transaction ID: $cleanTrxId.\nStatus: PENDING VERIFICATION. Pro will unlock once approved by admin."
                    )
                    loadUserClaims()
                    return@launch
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                    return@launch
                }
            }

            // Offline / fallback immediate unlock for testing/demo
            val expiryMs = when (plan) {
                SubscriptionPlan.PRO_MONTHLY -> System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                SubscriptionPlan.PRO_YEARLY, SubscriptionPlan.STUDENT_YEARLY -> System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
                else -> 0L
            }

            entitlementManager.activatePlan(plan, expiryMs, token = "bkash_trx:$cleanTrxId")

            DiscordNotifier.notifyProPurchase(
                planName = plan.displayName,
                method = "bKash In-App TrxID ($cleanPhone -> 01643526721)",
                licenseKey = "TrxID: $cleanTrxId | Phone: $cleanPhone | Email: ${userEmail ?: "N/A"}"
            )

            _uiState.value = _uiState.value.copy(isLoading = false, purchaseSucceeded = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(submissionSuccessMessage = null)
    }

    companion object {
        private val usedTrxIds = Collections.synchronizedSet(mutableSetOf<String>())
    }
}
