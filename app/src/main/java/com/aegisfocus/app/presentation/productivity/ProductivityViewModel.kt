package com.aegisfocus.app.presentation.productivity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.local.MindfulGate
import com.aegisfocus.app.data.local.MindfulGateStore
import com.aegisfocus.app.domain.model.AppInfo
import com.aegisfocus.app.domain.repository.AppDiscoveryRepository
import kotlinx.coroutines.launch
import com.aegisfocus.app.data.local.CleanStreakState
import com.aegisfocus.app.data.local.ProductivityStore
import com.aegisfocus.app.data.local.Reminder
import com.aegisfocus.app.service.receiver.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProductivityUiState(
    val reminders: List<Reminder> = emptyList(),
    val streak: CleanStreakState,
    val gates: List<MindfulGate> = emptyList()
)

class ProductivityViewModel(
    private val store: ProductivityStore,
    private val appContext: Context,
    private val gateStore: MindfulGateStore,
    private val appDiscoveryRepository: AppDiscoveryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ProductivityUiState> = _state.asStateFlow()

    private fun load() = ProductivityUiState(
        reminders = store.getReminders(),
        streak = store.getCleanStreakState(),
        gates = gateStore.getGates().sortedBy { it.appName.lowercase() }
    )

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    /** Loads launchable, non-system apps for the Mindful Gate app picker. */
    fun loadApps() {
        if (_apps.value.isNotEmpty()) return
        viewModelScope.launch {
            _apps.value = appDiscoveryRepository.loadInstalledApps()
                .filter { !it.isSystemApp && it.packageName != appContext.packageName }
                .sortedBy { it.appName.lowercase() }
        }
    }

    // Mindful Gate

    fun saveGate(gate: MindfulGate) {
        gateStore.saveGate(gate)
        refresh()
    }

    fun setGateEnabled(gate: MindfulGate, enabled: Boolean) {
        gateStore.setEnabled(gate.packageName, enabled)
        refresh()
    }

    fun deleteGate(gate: MindfulGate) {
        gateStore.deleteGate(gate.packageName)
        refresh()
    }

    fun refresh() {
        _state.value = load()
    }

    // Reminders

    fun addReminder(title: String, hour: Int, minute: Int) {
        if (title.isBlank()) return
        val reminder = store.addReminder(title, hour, minute)
        ReminderScheduler.schedule(appContext, reminder)
        refresh()
    }

    fun setReminderEnabled(reminder: Reminder, enabled: Boolean) {
        store.setReminderEnabled(reminder.id, enabled)
        val updated = reminder.copy(enabled = enabled)
        if (enabled) ReminderScheduler.schedule(appContext, updated) else ReminderScheduler.cancel(appContext, reminder.id)
        refresh()
    }

    fun deleteReminder(reminder: Reminder) {
        ReminderScheduler.cancel(appContext, reminder.id)
        store.deleteReminder(reminder.id)
        refresh()
    }

    // Clean Streak

    fun setTodayChecked(checked: Boolean) {
        store.setTodayChecked(checked)
        refresh()
    }

    fun startOver() {
        store.startOver()
        refresh()
    }
}
