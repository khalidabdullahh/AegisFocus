package com.aegisfocus.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.UsageStatsSummary
import com.aegisfocus.app.domain.repository.UsageStatsRepository
import com.aegisfocus.app.engine.FocusSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val greeting: String = TimeUtils.getGreeting(),
    val displayDate: String = TimeUtils.getFormattedDisplayDate(),
    val usageSummary: UsageStatsSummary = UsageStatsSummary(date = TimeUtils.getTodayDateString()),
    val activeSession: FocusSession? = null,
    val remainingMillis: Long = 0L,
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val focusSessionManager: FocusSessionManager,
    private val usageStatsRepository: UsageStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val activeSession: StateFlow<FocusSession?> = focusSessionManager.activeSession
    val remainingMillis: StateFlow<Long> = focusSessionManager.remainingMillis

    init {
        loadDashboardData()
        observeActiveSession()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val summary = usageStatsRepository.getTodayUsageSummary()
            _uiState.value = _uiState.value.copy(
                greeting = TimeUtils.getGreeting(),
                displayDate = TimeUtils.getFormattedDisplayDate(),
                usageSummary = summary,
                isLoading = false
            )
        }
    }

    private fun observeActiveSession() {
        viewModelScope.launch {
            focusSessionManager.activeSession.collect { session ->
                _uiState.value = _uiState.value.copy(activeSession = session)
                if (session == null) {
                    loadDashboardData()
                }
            }
        }
        viewModelScope.launch {
            focusSessionManager.remainingMillis.collect { remaining ->
                _uiState.value = _uiState.value.copy(remainingMillis = remaining)
            }
        }
    }

    fun startQuickFocus(durationMinutes: Int = 25, isStrict: Boolean = false) {
        viewModelScope.launch {
            focusSessionManager.startSession(
                name = "Deep Focus",
                durationMinutes = durationMinutes,
                mode = if (isStrict) SessionMode.STRICT else SessionMode.NORMAL
            )
        }
    }
}
