package com.aegisfocus.app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.remote.PaymentRequest
import com.aegisfocus.app.data.remote.SupabasePaymentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminPaymentUiState(
    val pendingRequests: List<PaymentRequest> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionFeedback: String? = null
)

class AdminPaymentViewModel(
    private val paymentService: SupabasePaymentService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPaymentUiState())
    val uiState: StateFlow<AdminPaymentUiState> = _uiState.asStateFlow()

    init {
        loadPendingRequests()
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val requests = paymentService.getAllPendingRequests()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                pendingRequests = requests
            )
        }
    }

    fun approveRequest(request: PaymentRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, actionFeedback = null)
            val result = paymentService.approvePayment(
                requestId = request.id,
                planType = request.planRequested,
                userEmail = request.userEmail ?: request.userId,
                transactionId = request.transactionId
            )
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    actionFeedback = "✅ Approved TrxID: ${request.transactionId} for plan ${request.planRequested}."
                )
                loadPendingRequests()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun rejectRequest(request: PaymentRequest, reason: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, actionFeedback = null)
            val result = paymentService.rejectPayment(
                requestId = request.id,
                reason = reason,
                userEmail = request.userEmail ?: request.userId,
                transactionId = request.transactionId
            )
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    actionFeedback = "❌ Rejected TrxID: ${request.transactionId}."
                )
                loadPendingRequests()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(actionFeedback = null, errorMessage = null)
    }
}
