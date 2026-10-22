package com.aegisfocus.app.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.local.dao.DailyGoalDao
import com.aegisfocus.app.data.local.dao.FocusSessionDao
import com.aegisfocus.app.data.local.dao.UserXpDao
import com.aegisfocus.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class DayGoalStatus {
    SUCCESS_MET,       // Met or exceeded daily goal
    MISSED,            // Missed goal in the past
    TODAY_IN_PROGRESS, // Today with active progress
    FUTURE             // Upcoming day in the month
}

data class GoalDayUiModel(
    val date: LocalDate,
    val dayNumber: Int,
    val dateKey: String,
    val achievedMinutes: Int,
    val targetMinutes: Int,
    val sessionsCompleted: Int,
    val status: DayGoalStatus,
    val isToday: Boolean,
    val isSelected: Boolean
)

data class GoalsUiState(
    val currentYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val targetMinutes: Int = 120,
    val days: List<GoalDayUiModel> = emptyList(),
    val totalMonthFocusMinutes: Int = 0,
    val daysWon: Int = 0,
    val daysMissed: Int = 0,
    val daysTotalElapsed: Int = 0,
    val consistencyPercentage: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val firstDayOffset: Int = 0 // Mon=0..Sun=6
)

class GoalsViewModel(
    private val dailyGoalDao: DailyGoalDao,
    private val focusSessionDao: FocusSessionDao,
    private val settingsRepository: SettingsRepository,
    private val userXpDao: UserXpDao
) : ViewModel() {

    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _targetMinutes = MutableStateFlow(settingsRepository.getDailyFocusGoalMinutes())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GoalsUiState> = combine(
        _currentYearMonth,
        _selectedDate,
        _targetMinutes,
        userXpDao.observeXp()
    ) { ym, selected, target, xp ->
        GoalsStateParams(ym, selected, target, xp?.currentStreak ?: 0, xp?.longestStreak ?: 0)
    }.flatMapLatest { params ->
        val prefix = params.ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        dailyGoalDao.getGoalsForMonth(prefix).combine(
            focusSessionDao.getAllSessionsFlow()
        ) { goalEntities, sessions ->
            buildUiState(params, goalEntities, sessions)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalsUiState())

    private data class GoalsStateParams(
        val ym: YearMonth,
        val selected: LocalDate,
        val target: Int,
        val currentStreak: Int,
        val bestStreak: Int
    )

    private fun buildUiState(
        params: GoalsStateParams,
        goalEntities: List<com.aegisfocus.app.data.local.entity.DailyGoalEntity>,
        sessions: List<com.aegisfocus.app.data.local.entity.FocusSessionEntity>
    ): GoalsUiState {
        val today = LocalDate.now(ZoneId.systemDefault())
        val daysInMonth = params.ym.lengthOfMonth()
        val firstDay = params.ym.atDay(1)
        // DayOfWeek: Monday=1 .. Sunday=7 -> offset 0..6
        val firstDayOffset = firstDay.dayOfWeek.value - 1

        val goalMap = goalEntities.associateBy { it.dateKey }

        // Also aggregate completed sessions per date
        val zoneId = ZoneId.systemDefault()
        val sessionDurationMap = mutableMapOf<String, Int>()
        val sessionCountMap = mutableMapOf<String, Int>()

        sessions.filter { it.status == "COMPLETED" }.forEach { session ->
            val sessionDate = java.time.Instant.ofEpochMilli(session.startEpochMs)
                .atZone(zoneId)
                .toLocalDate()
            val key = sessionDate.toString()
            sessionDurationMap[key] = (sessionDurationMap[key] ?: 0) + session.durationMinutes
            sessionCountMap[key] = (sessionCountMap[key] ?: 0) + 1
        }

        var totalMonthMinutes = 0
        var daysWon = 0
        var daysMissed = 0
        var daysElapsed = 0

        val daysList = (1..daysInMonth).map { dayNum ->
            val date = params.ym.atDay(dayNum)
            val dateKey = date.toString()
            val isToday = (date == today)
            val isFuture = date.isAfter(today)
            val isPast = date.isBefore(today)

            val dbGoal = goalMap[dateKey]
            val fromSessionsMinutes = sessionDurationMap[dateKey] ?: 0
            val fromSessionsCount = sessionCountMap[dateKey] ?: 0

            val achievedMinutes = maxOf(dbGoal?.achievedFocusMinutes ?: 0, fromSessionsMinutes)
            val sessionCount = maxOf(dbGoal?.achievedSessions ?: 0, fromSessionsCount)
            val isGoalMet = (achievedMinutes >= params.target) || (dbGoal?.isGoalMet == true)

            val status = when {
                isFuture -> DayGoalStatus.FUTURE
                isToday -> if (isGoalMet) DayGoalStatus.SUCCESS_MET else DayGoalStatus.TODAY_IN_PROGRESS
                else -> if (isGoalMet) DayGoalStatus.SUCCESS_MET else DayGoalStatus.MISSED
            }

            if (!isFuture) {
                daysElapsed++
                totalMonthMinutes += achievedMinutes
                if (status == DayGoalStatus.SUCCESS_MET) {
                    daysWon++
                } else if (status == DayGoalStatus.MISSED) {
                    daysMissed++
                }
            }

            GoalDayUiModel(
                date = date,
                dayNumber = dayNum,
                dateKey = dateKey,
                achievedMinutes = achievedMinutes,
                targetMinutes = params.target,
                sessionsCompleted = sessionCount,
                status = status,
                isToday = isToday,
                isSelected = (date == params.selected)
            )
        }

        val consistencyPct = if (daysElapsed > 0) {
            ((daysWon * 100) / daysElapsed).coerceIn(0, 100)
        } else {
            100
        }

        return GoalsUiState(
            currentYearMonth = params.ym,
            selectedDate = params.selected,
            targetMinutes = params.target,
            days = daysList,
            totalMonthFocusMinutes = totalMonthMinutes,
            daysWon = daysWon,
            daysMissed = daysMissed,
            daysTotalElapsed = daysElapsed,
            consistencyPercentage = consistencyPct,
            currentStreak = params.currentStreak,
            bestStreak = params.bestStreak,
            firstDayOffset = firstDayOffset
        )
    }

    fun previousMonth() {
        _currentYearMonth.value = _currentYearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        val next = _currentYearMonth.value.plusMonths(1)
        val now = YearMonth.now()
        // Allow browsing into current month or at most +1 month ahead
        if (!next.isAfter(now.plusMonths(1))) {
            _currentYearMonth.value = next
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setTargetMinutes(minutes: Int) {
        settingsRepository.setDailyFocusGoalMinutes(minutes)
        _targetMinutes.value = minutes
    }
}
