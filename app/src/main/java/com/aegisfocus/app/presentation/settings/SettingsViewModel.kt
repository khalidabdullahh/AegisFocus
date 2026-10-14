package com.aegisfocus.app.presentation.settings

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.local.entity.SubscriptionEntity
import com.aegisfocus.app.data.remote.SupabaseAuthService
import com.aegisfocus.app.domain.model.PermissionState
import com.aegisfocus.app.domain.model.Schedule
import com.aegisfocus.app.domain.repository.ScheduleRepository
import com.aegisfocus.app.domain.repository.SettingsRepository
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.domain.subscription.LicenseKeyValidator
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
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
    val isRedeemDialogOpen: Boolean = false,
    val redeemError: String? = null,
    val redeemSuccess: String? = null,
    val isAdminMode: Boolean = false,
    val isPasswordPromptOpen: Boolean = false,
    val adminPasswordError: String? = null,
    val activePlan: SubscriptionPlan = SubscriptionPlan.FREE,
    val daysRemaining: Long = 0L,
    val isShortsBlockerEnabled: Boolean = true,
    val isGrayscaleFatigueEnabled: Boolean = false
)

private data class SettingsLocalDialogState(
    val isAddScheduleOpen: Boolean = false,
    val isRedeemDialogOpen: Boolean = false,
    val redeemError: String? = null,
    val redeemSuccess: String? = null,
    val isAdminMode: Boolean = false,
    val isPasswordPromptOpen: Boolean = false,
    val adminPasswordError: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val entitlementManager: EntitlementManager? = null,
    private val authService: SupabaseAuthService? = null
) : ViewModel() {

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    private val _localState = MutableStateFlow(SettingsLocalDialogState())
    private var adminTapCount = 0

    private val activePlanFlow: StateFlow<SubscriptionPlan> = entitlementManager?.activePlan?.stateIn(
        viewModelScope, SharingStarted.Eagerly, SubscriptionPlan.FREE
    ) ?: MutableStateFlow(SubscriptionPlan.FREE)

    private val subEntityFlow: StateFlow<SubscriptionEntity?> = entitlementManager?.subscriptionEntity?.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    ) ?: MutableStateFlow(null)

    private val subDataFlow = combine(activePlanFlow, subEntityFlow) { plan, entity ->
        val remaining = if (entity != null && entity.expiryEpochMs > System.currentTimeMillis()) {
            val diffMs = entity.expiryEpochMs - System.currentTimeMillis()
            (diffMs / (24L * 60 * 60 * 1000)).coerceAtLeast(1)
        } else {
            0L
        }
        plan to remaining
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getPermissionStateFlow(),
        _schedules,
        subDataFlow,
        _localState
    ) { perms, scheds, subData, local ->
        val (plan, remaining) = subData
        SettingsUiState(
            permissions = perms,
            defaultDurationMinutes = settingsRepository.getDefaultFocusDurationMinutes(),
            emergencyCooldownSeconds = settingsRepository.getEmergencyExitCooldownSeconds(),
            isStrictFrictionPhraseEnabled = settingsRepository.isStrictModeFrictionPhraseEnabled(),
            isDarkTheme = settingsRepository.isDarkTheme(),
            schedules = scheds,
            isAddScheduleDialogOpen = local.isAddScheduleOpen,
            isRedeemDialogOpen = local.isRedeemDialogOpen,
            redeemError = local.redeemError,
            redeemSuccess = local.redeemSuccess,
            isAdminMode = local.isAdminMode,
            isPasswordPromptOpen = local.isPasswordPromptOpen,
            adminPasswordError = local.adminPasswordError,
            activePlan = plan,
            daysRemaining = remaining,
            isShortsBlockerEnabled = settingsRepository.isShortsBlockerEnabled(),
            isGrayscaleFatigueEnabled = settingsRepository.isGrayscaleFatigueEnabled()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun toggleShortsBlocker(enabled: Boolean) {
        settingsRepository.setShortsBlockerEnabled(enabled)
        refreshPermissions()
    }

    fun toggleGrayscaleFatigue(enabled: Boolean) {
        settingsRepository.setGrayscaleFatigueEnabled(enabled)
        refreshPermissions()
    }

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
            _localState.value = _localState.value.copy(isAddScheduleOpen = false)
        }
    }

    fun openAddScheduleDialog() {
        _localState.value = _localState.value.copy(isAddScheduleOpen = true)
    }

    fun closeAddScheduleDialog() {
        _localState.value = _localState.value.copy(isAddScheduleOpen = false)
    }

    fun openRedeemDialog() {
        _localState.value = _localState.value.copy(
            isRedeemDialogOpen = true,
            redeemError = null,
            redeemSuccess = null
        )
    }

    fun closeRedeemDialog() {
        _localState.value = _localState.value.copy(
            isRedeemDialogOpen = false,
            redeemError = null,
            redeemSuccess = null
        )
    }

    fun redeemLicenseKey(rawKey: String) {
        val key = rawKey.trim().uppercase()
        if (key.isBlank()) {
            _localState.value = _localState.value.copy(redeemError = "Please enter a valid license key.")
            return
        }

        val result = LicenseKeyValidator.validateKey(key)
        if (!result.isValid) {
            _localState.value = _localState.value.copy(redeemError = result.message)
            return
        }

        viewModelScope.launch {
            try {
                val expiryMs = if (result.durationDays > 0) {
                    System.currentTimeMillis() + result.durationDays.toLong() * 24 * 60 * 60 * 1000
                } else {
                    0L
                }
                entitlementManager?.activatePlan(result.plan, expiryMs, key)
                _localState.value = _localState.value.copy(
                    redeemSuccess = result.message,
                    redeemError = null
                )
            } catch (e: Exception) {
                _localState.value = _localState.value.copy(
                    redeemError = "Activation failed: ${e.message}"
                )
            }
        }
    }

    fun onVersionTapped(context: Context? = null) {
        if (_localState.value.isAdminMode) {
            _localState.value = _localState.value.copy(isAdminMode = false)
            adminTapCount = 0
            if (context != null) {
                Toast.makeText(context, "Super Admin Mode Deactivated", Toast.LENGTH_SHORT).show()
            }
            return
        }

        adminTapCount++
        if (adminTapCount >= 5) {
            _localState.value = _localState.value.copy(
                isPasswordPromptOpen = true,
                adminPasswordError = null
            )
            adminTapCount = 0
        } else if (adminTapCount >= 2 && context != null) {
            val remaining = 5 - adminTapCount
            Toast.makeText(context, "Tap $remaining more times for Admin Mode", Toast.LENGTH_SHORT).show()
        }
    }

    fun closeAdminPasswordPrompt() {
        _localState.value = _localState.value.copy(
            isPasswordPromptOpen = false,
            adminPasswordError = null
        )
    }

    fun verifyAdminPassword(password: String, context: Context? = null) {
        val trimmed = password.trim()
        if (trimmed == "6924923086") {
            _localState.value = _localState.value.copy(
                isAdminMode = true,
                isPasswordPromptOpen = false,
                adminPasswordError = null
            )
            if (context != null) {
                Toast.makeText(context, "👑 Super Admin Mode Unlocked!", Toast.LENGTH_SHORT).show()
            }
        } else {
            _localState.value = _localState.value.copy(
                adminPasswordError = "Incorrect password. Access denied."
            )
        }
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
