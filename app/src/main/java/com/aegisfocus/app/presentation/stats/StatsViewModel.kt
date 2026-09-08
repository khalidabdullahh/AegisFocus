package com.aegisfocus.app.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.domain.model.UsageStatsSummary
import com.aegisfocus.app.domain.usecase.GetUsageAnalyticsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val todaySummary: UsageStatsSummary = UsageStatsSummary(date = ""),
    val weeklyHistory: List<UsageStatsSummary> = emptyList(),
    val isLoading: Boolean = false
)

class StatsViewModel(
    private val getUsageAnalyticsUseCase: GetUsageAnalyticsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState(isLoading = true))
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val today = getUsageAnalyticsUseCase.getTodaySummary()
            val weekly = getUsageAnalyticsUseCase.getWeeklyHistory()
            _uiState.value = StatsUiState(
                todaySummary = today,
                weeklyHistory = weekly,
                isLoading = false
            )
        }
    }
}
