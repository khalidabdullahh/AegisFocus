package com.aegisfocus.app.presentation.settings

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.remote.AuthUser
import com.aegisfocus.app.data.remote.SupabaseAuthService
import com.aegisfocus.app.domain.model.PermissionState
import com.aegisfocus.app.domain.model.Schedule
import com.aegisfocus.app.domain.repository.ScheduleRepository
import com.aegisfocus.app.domain.repository.SettingsRepository
import com.aegisfocus.app.service.receiver.AegisDeviceAdminReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val permissions: PermissionState = PermissionState(),
    val defaultDurationMinutes: Int = 25,
    val emergencyCooldownSeconds: Int = 5,
    val isStrictFrictionPhraseEnabled: Boolean = true,
    val isDarkTheme: Boolean = true,
    val schedules: List<Schedule> = emptyList(),
    val isAddScheduleDialogOpen: Boolean = false,
    val currentUser: AuthUser? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val authService: SupabaseAuthService
) : ViewModel() {

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    private val _isAddScheduleOpen = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getPermissionStateFlow(),
        _schedules,
        _isAddScheduleOpen,
        authService.currentUser
    ) { perms, scheds, addOpen, user ->
        SettingsUiState(
            permissions = perms,
            defaultDurationMinutes = settingsRepository.getDefaultFocusDurationMinutes(),
            emergencyCooldownSeconds = settingsRepository.getEmergencyExitCooldownSeconds(),
            isStrictFrictionPhraseEnabled = settingsRepository.isStrictModeFrictionPhraseEnabled(),
            isDarkTheme = settingsRepository.isDarkTheme(),
            schedules = scheds,
            isAddScheduleDialogOpen = addOpen,
            currentUser = user
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        refreshPermissions()
        loadSchedules()
    }

    fun refreshPermissions() {
        settingsRepository.refreshPermissionState()
    }

    fun loadSchedules() {
        viewModelScope.launch {
            scheduleRepository.getAllSchedulesFlow().collect {
                _schedules.value = it
            }
        }
    }

    fun setDefaultDuration(minutes: Int) {
        settingsRepository.setDefaultFocusDurationMinutes(minutes)
        refreshPermissions()
    }

    fun setEmergencyCooldown(seconds: Int) {
        settingsRepository.setEmergencyExitCooldownSeconds(seconds)
        refreshPermissions()
    }

    fun setStrictFrictionPhrase(enabled: Boolean) {
        settingsRepository.setStrictModeFrictionPhraseEnabled(enabled)
        refreshPermissions()
    }

    fun toggleSchedule(scheduleId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            scheduleRepository.setScheduleEnabled(scheduleId, isEnabled)
        }
    }

    fun addSchedule(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepository.saveSchedule(schedule)
            _isAddScheduleOpen.value = false
        }
    }

    fun openAddScheduleDialog() {
        _isAddScheduleOpen.value = true
    }

    fun closeAddScheduleDialog() {
        _isAddScheduleOpen.value = false
    }

    fun updateDisplayName(newName: String) {
        authService.updateDisplayName(newName)
    }

    // Direct Platform Intent Openers
    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            if (context !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            if (context !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                if (context !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                if (context !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        runCatching { context.startActivity(intent) }
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                if (context !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { context.startActivity(intent) }
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            ).apply {
                if (context !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { context.startActivity(intent) }
        }
    }

    fun openDeviceAdminSettings(context: Context) {
        val adminComponent = ComponentName(context, AegisDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activate Anti-Uninstall Protection to prevent deactivating or uninstalling Aegis Focus during strict focus sessions."
            )
            if (context !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }
}
