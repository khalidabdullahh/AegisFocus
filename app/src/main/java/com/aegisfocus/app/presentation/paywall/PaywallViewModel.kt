package com.aegisfocus.app.presentation.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.billing.StoreBillingManager
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.aegisfocus.app.domain.subscription.LicenseKeyValidator

data class PaywallUiState(
    val currentPlan: SubscriptionPlan = SubscriptionPlan.FREE,
    val selectedOption: PaywallOption = PaywallOption.MONTH_12,
    val isLoading: Boolean = false,
    val isRedeeming: Boolean = false,
    val purchaseSucceeded: Boolean = false,
    val errorMessage: String? = null,
    val redeemSuccessMessage: String? = null,
    val redeemErrorMessage: String? = null,
    val licenseKeyInput: String = "",
    val storeName: String = "Direct APK / License Key"
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
    private val storeBillingManager: StoreBillingManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PaywallUiState(
            storeName = storeBillingManager?.currentStore?.displayName ?: "Direct APK / License Key"
        )
    )
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
     * Offline Cryptographic License Key Redemption (Instant, 100% On-Device, No DB).
     */
    fun redeemLicenseKey(rawKey: String? = null) {
        val key = (rawKey ?: _uiState.value.licenseKeyInput).trim().uppercase()
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(redeemErrorMessage = "Please enter a valid license key.")
            return
        }

        _uiState.value = _uiState.value.copy(isRedeeming = true, redeemErrorMessage = null, redeemSuccessMessage = null)

        val result = LicenseKeyValidator.validateKey(key)
        if (!result.isValid) {
            _uiState.value = _uiState.value.copy(
                isRedeeming = false,
                redeemErrorMessage = result.message
            )
            return
        }

        viewModelScope.launch {
            try {
                val expiryMs = if (result.durationDays > 0) {
                    System.currentTimeMillis() + result.durationDays.toLong() * 24 * 60 * 60 * 1000
                } else {
                    0L // Lifetime
                }
                entitlementManager.activatePlan(result.plan, expiryMs, key)
                _uiState.value = _uiState.value.copy(
                    isRedeeming = false,
                    purchaseSucceeded = true,
                    redeemSuccessMessage = result.message,
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

    /**
     * Initiates official Store In-App Purchase flow (if store configured) or local activation fallback.
     */
    fun purchaseSelected(activity: Activity? = null) {
        val option = _uiState.value.selectedOption
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        if (activity != null && storeBillingManager != null) {
            storeBillingManager.launchPurchase(activity, option.planType) { success, error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    purchaseSucceeded = success,
                    errorMessage = error
                )
            }
        } else {
            // Local activation fallback
            viewModelScope.launch {
                try {
                    val expiryMs = if (option.durationDays > 0) {
                        System.currentTimeMillis() + option.durationDays.toLong() * 24 * 60 * 60 * 1000
                    } else {
                        0L
                    }
                    entitlementManager.activatePlan(option.planType, expiryMs, "local_activated")
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

    /**
     * Restores previous purchases across connected store accounts.
     */
    fun restorePurchases() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        if (storeBillingManager != null) {
            storeBillingManager.restorePurchases { success, message ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = if (!success) message else null
                )
            }
        } else {
            viewModelScope.launch {
                val isPro = entitlementManager.isPro()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = if (!isPro) "No active subscriptions found." else null
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, redeemErrorMessage = null)
    }
}
