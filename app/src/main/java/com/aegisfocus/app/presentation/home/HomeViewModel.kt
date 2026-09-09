package com.aegisfocus.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.domain.analytics.DisciplineScoreCalculator
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.UsageStatsSummary
import com.aegisfocus.app.domain.motivation.DisciplineLevel
import com.aegisfocus.app.domain.motivation.XpManager
import com.aegisfocus.app.domain.repository.UsageStatsRepository
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
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
    val isLoading: Boolean = false,
    // v2: XP & Discipline Score
    val disciplineScore: Int = 80,
    val disciplineScoreBreakdown: DisciplineScoreCalculator.ScoreBreakdown? = null,
    val currentXp: Int = 0,
    val currentStreak: Int = 0,
    val currentLevel: DisciplineLevel = DisciplineLevel.NEW_STARTER,
    val isPro: Boolean = false,
    val activePlan: SubscriptionPlan = SubscriptionPlan.FREE
)

class HomeViewModel(
    private val focusSessionManager: FocusSessionManager,
    private val usageStatsRepository: UsageStatsRepository,
    private val disciplineScoreCalculator: DisciplineScoreCalculator? = null,
    private val xpManager: XpManager? = null,
    private val entitlementManager: EntitlementManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val activeSession: StateFlow<FocusSession?> = focusSessionManager.activeSession
    val remainingMillis: StateFlow<Long> = focusSessionManager.remainingMillis

    init {
        loadDashboardData()
        observeActiveSession()
        observeXpAndLevel()
        observeSubscription()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val summary = usageStatsRepository.getTodayUsageSummary()

            // Load discipline score
            val scoreBreakdown = disciplineScoreCalculator?.calculateScore()

            _uiState.value = _uiState.value.copy(
                greeting = TimeUtils.getGreeting(),
                displayDate = TimeUtils.getFormattedDisplayDate(),
                usageSummary = summary,
                isLoading = false,
                disciplineScore = scoreBreakdown?.total ?: 80,
                disciplineScoreBreakdown = scoreBreakdown
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

    private fun observeXpAndLevel() {
        xpManager ?: return
        viewModelScope.launch {
            xpManager.currentXp.collect { xp ->
                _uiState.value = _uiState.value.copy(
                    currentXp = xp,
                    currentLevel = DisciplineLevel.fromXp(xp)
                )
            }
        }
        viewModelScope.launch {
            xpManager.currentStreak.collect { streak ->
                _uiState.value = _uiState.value.copy(currentStreak = streak)
            }
        }
    }

    private fun observeSubscription() {
        entitlementManager ?: return
        viewModelScope.launch {
            entitlementManager.activePlan.collect { plan ->
                _uiState.value = _uiState.value.copy(
                    activePlan = plan,
                    isPro = plan != SubscriptionPlan.FREE
                )
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
