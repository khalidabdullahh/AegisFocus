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

data class PaywallUiState(
    val currentPlan: SubscriptionPlan = SubscriptionPlan.FREE,
    val selectedOption: PaywallOption = PaywallOption.YEARLY,
    val isLoading: Boolean = false,
    val purchaseSucceeded: Boolean = false,
    val errorMessage: String? = null,
    val storeName: String = "Google Play"
)

enum class PaywallOption(
    val label: String,
    val price: String,
    val perMonth: String,
    val planType: SubscriptionPlan
) {
    MONTHLY("Monthly", "$2.99", "$2.99/mo", SubscriptionPlan.PRO_MONTHLY),
    YEARLY("Yearly", "$19.99", "$1.67/mo", SubscriptionPlan.PRO_YEARLY),
    LIFETIME("Lifetime", "$49.99", "One-time", SubscriptionPlan.LIFETIME),
    STUDENT("Student Yearly", "$9.99", "$0.83/mo", SubscriptionPlan.STUDENT_YEARLY)
}

class PaywallViewModel(
    private val entitlementManager: EntitlementManager,
    private val storeBillingManager: StoreBillingManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PaywallUiState(
            storeName = storeBillingManager?.currentStore?.displayName ?: "Google Play"
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

    /**
     * Initiates official Store In-App Purchase flow (Google Play / Samsung / Amazon).
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
                    val expiryMs = when (option.planType) {
                        SubscriptionPlan.PRO_MONTHLY -> System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                        SubscriptionPlan.PRO_YEARLY, SubscriptionPlan.STUDENT_YEARLY -> System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
                        SubscriptionPlan.LIFETIME -> 0L
                        else -> 0L
                    }
                    entitlementManager.activatePlan(option.planType, expiryMs, "store_activated_token")
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
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
