package com.aegisfocus.app.presentation.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val errorMessage: String? = null
)

enum class PaywallOption(val label: String, val price: String, val perMonth: String, val planType: SubscriptionPlan) {
    MONTHLY("Monthly", "$2.99", "$2.99/mo", SubscriptionPlan.PRO_MONTHLY),
    YEARLY("Yearly", "$19.99", "$1.67/mo", SubscriptionPlan.PRO_YEARLY),
    LIFETIME("Lifetime", "$49.99", "One-time", SubscriptionPlan.LIFETIME),
    STUDENT("Student Yearly", "$9.99", "$0.83/mo", SubscriptionPlan.STUDENT_YEARLY)
}

class PaywallViewModel(private val entitlementManager: EntitlementManager) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            entitlementManager.activePlan.collect { plan ->
                _uiState.value = _uiState.value.copy(currentPlan = plan)
            }
        }
    }

    fun selectOption(option: PaywallOption) {
        _uiState.value = _uiState.value.copy(selectedOption = option)
    }

    /**
     * Initiates purchase flow. In production this would call Google Play Billing.
     * For now simulates success for demo/test purposes.
     */
    fun purchaseSelected() {
        val option = _uiState.value.selectedOption
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // TODO: integrate Google Play Billing here
                // For now: directly activate the plan (demo mode)
                val expiryMs = when (option.planType) {
                    SubscriptionPlan.PRO_MONTHLY -> System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                    SubscriptionPlan.PRO_YEARLY, SubscriptionPlan.STUDENT_YEARLY -> System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
                    SubscriptionPlan.LIFETIME -> 0L
                    else -> 0L
                }
                entitlementManager.activatePlan(option.planType, expiryMs)
                _uiState.value = _uiState.value.copy(isLoading = false, purchaseSucceeded = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Purchase failed. Please try again."
                )
            }
        }
    }

    fun restorePurchases() {
        // TODO: implement Play Billing restore
        _uiState.value = _uiState.value.copy(errorMessage = "Restore purchases coming soon.")
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
