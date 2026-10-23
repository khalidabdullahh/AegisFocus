package com.aegisfocus.app.presentation.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.remote.SupabaseAuthService
import com.aegisfocus.app.data.remote.SupabasePaymentService
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaywallUiState(
    val currentPlan: SubscriptionPlan = SubscriptionPlan.FREE,
    val selectedOption: PaywallOption = PaywallOption.MONTH_12,
    val isRedeeming: Boolean = false,
    val purchaseSucceeded: Boolean = false,
    val redeemSuccessMessage: String? = null,
    val redeemErrorMessage: String? = null,
    val licenseKeyInput: String = "",
    val isSignedIn: Boolean = false,
    val userEmail: String? = null
)

enum class PaywallOption(
    val label: String,
    val price: String,
    val perMonth: String,
    val planType: SubscriptionPlan,
    val durationDays: Int
) {
    MONTH_1("1 Month Pro", "৳150 / $1.99", "30 Days Access", SubscriptionPlan.PRO_1_MONTH, 30),
    MONTH_3("3 Months Pro", "৳350 / $4.49", "90 Days Access", SubscriptionPlan.PRO_3_MONTHS, 90),
    MONTH_6("6 Months Pro", "৳600 / $7.99", "180 Days Access", SubscriptionPlan.PRO_6_MONTHS, 180),
    MONTH_12("12 Months (1 Year)", "৳999 / $12.99", "365 Days • Best Value", SubscriptionPlan.PRO_12_MONTHS, 365),
    LIFETIME("Lifetime Pro", "৳1999 / $24.99", "Permanent Access", SubscriptionPlan.LIFETIME, 0)
}

class PaywallViewModel(
    private val entitlementManager: EntitlementManager,
    private val paymentService: SupabasePaymentService,
    private val authService: SupabaseAuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            entitlementManager.activePlan.collect { plan ->
                _uiState.value = _uiState.value.copy(
                    currentPlan = plan,
                    purchaseSucceeded = plan != SubscriptionPlan.FREE
                )
            }
        }
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    isSignedIn = user != null,
                    userEmail = user?.email
                )
            }
        }
    }

    fun selectOption(option: PaywallOption) {
        _uiState.value = _uiState.value.copy(selectedOption = option)
    }

    fun onLicenseKeyChange(newKey: String) {
        _uiState.value = _uiState.value.copy(
            licenseKeyInput = newKey,
            redeemErrorMessage = null,
            redeemSuccessMessage = null
        )
    }

    /**
     * Redeems a key on the server. Each key works exactly once, so a key that someone
     * else already used is rejected there, not here.
     */
    fun redeemLicenseKey(rawKey: String? = null) {
        val key = (rawKey ?: _uiState.value.licenseKeyInput).trim().uppercase()
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(redeemErrorMessage = "Please enter your license key.")
            return
        }

        _uiState.value = _uiState.value.copy(isRedeeming = true, redeemErrorMessage = null, redeemSuccessMessage = null)

        viewModelScope.launch {
            val result = paymentService.redeemLicenseKey(key)
            if (!result.success) {
                _uiState.value = _uiState.value.copy(isRedeeming = false, redeemErrorMessage = result.message)
                return@launch
            }

            val plan = SubscriptionPlan.values().firstOrNull { it.name == result.plan }
            if (plan == null) {
                _uiState.value = _uiState.value.copy(
                    isRedeeming = false,
                    redeemErrorMessage = "Key accepted, but the plan is unknown to this app version. Please update the app."
                )
                return@launch
            }

            try {
                entitlementManager.activatePlan(plan, result.expiryEpochMs, key)
                _uiState.value = _uiState.value.copy(
                    isRedeeming = false,
                    purchaseSucceeded = true,
                    redeemSuccessMessage = "${plan.displayName} activated.",
                    licenseKeyInput = ""
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRedeeming = false,
                    redeemErrorMessage = "Activation failed: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(redeemErrorMessage = null)
    }
}
