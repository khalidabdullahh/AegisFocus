package com.aegisfocus.app.presentation.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.billing.PlayBillingManager
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
    val errorMessage: String? = null
)

enum class PaywallOption(val label: String, val price: String, val perMonth: String, val planType: SubscriptionPlan) {
    MONTHLY("Monthly", "$2.99", "$2.99/mo", SubscriptionPlan.PRO_MONTHLY),
    YEARLY("Yearly", "$19.99", "$1.67/mo", SubscriptionPlan.PRO_YEARLY),
    LIFETIME("Lifetime", "$49.99", "One-time", SubscriptionPlan.LIFETIME),
    STUDENT("Student Yearly", "$9.99", "$0.83/mo", SubscriptionPlan.STUDENT_YEARLY)
}

class PaywallViewModel(
    private val entitlementManager: EntitlementManager,
    private val playBillingManager: PlayBillingManager? = null
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
    }

    fun selectOption(option: PaywallOption) {
        _uiState.value = _uiState.value.copy(selectedOption = option)
    }

    /**
     * Initiates Google Play purchase flow or falls back gracefully in offline/demo mode.
     */
    fun purchaseSelected(activity: Activity? = null) {
        val option = _uiState.value.selectedOption
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        if (activity != null && playBillingManager != null) {
            playBillingManager.launchPurchase(activity, option.planType) { success, error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    purchaseSucceeded = success,
                    errorMessage = error
                )
            }
        } else {
            // Local simulation fallback
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
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
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
        val result = com.aegisfocus.app.domain.subscription.LicenseKeyValidator.validateKey(rawKey)
        if (!result.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = result.message)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val expiryMs = if (result.durationDays > 0) {
                System.currentTimeMillis() + result.durationDays.toLong() * 24 * 60 * 60 * 1000
            } else {
                0L // Lifetime
            }

            entitlementManager.activatePlan(result.plan, expiryMs, token = "license_key:$rawKey")
            com.aegisfocus.app.service.telemetry.DiscordNotifier.notifyProPurchase(
                planName = result.plan.displayName,
                method = "Web License Redemption",
                licenseKey = rawKey.trim().uppercase()
            )

            _uiState.value = _uiState.value.copy(isLoading = false, purchaseSucceeded = true)
        }
    }

    fun submitBkashPayment(rawTrxId: String, senderPhone: String, userEmail: String? = null) {
        val cleanTrxId = rawTrxId.trim().uppercase()
        val cleanPhone = senderPhone.trim()

        if (cleanTrxId.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid bKash Transaction ID (e.g. 9A2B3C4D)")
            return
        }

        if (cleanPhone.length < 10) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid bKash sender mobile number.")
            return
        }

        if (usedTrxIds.contains(cleanTrxId)) {
            _uiState.value = _uiState.value.copy(errorMessage = "⚠️ This Transaction ID ($cleanTrxId) has already been used to unlock Pro. Duplicate TrxIDs are prohibited.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            usedTrxIds.add(cleanTrxId)

            val option = _uiState.value.selectedOption
            val plan = option.planType
            val expiryMs = when (plan) {
                SubscriptionPlan.PRO_MONTHLY -> System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                SubscriptionPlan.PRO_YEARLY, SubscriptionPlan.STUDENT_YEARLY -> System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
                else -> 0L // Lifetime
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

    companion object {
        private val usedTrxIds = Collections.synchronizedSet(mutableSetOf<String>())
    }
}
