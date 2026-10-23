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
import com.aegisfocus.app.data.remote.LicenseKeyRow
import com.aegisfocus.app.data.remote.SupabasePaymentService
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
import com.aegisfocus.app.service.receiver.AegisDeviceAdminReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isGrayscaleFatigueEnabled: Boolean = false,
    val isAdBlockerVpnEnabled: Boolean = false,
    val isAdultBlockEnabled: Boolean = false,
    val currentUser: com.aegisfocus.app.data.remote.AuthUser? = null
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

data class AdminKeyState(
    val isBusy: Boolean = false,
    val lastKey: String? = null,
    val keys: List<LicenseKeyRow> = emptyList(),
    val message: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val entitlementManager: EntitlementManager? = null,
    private val authService: SupabaseAuthService? = null,
    private val paymentService: SupabasePaymentService? = null
) : ViewModel() {

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    private val _localState = MutableStateFlow(SettingsLocalDialogState())
    // Bumped after every preference change so toggles update on screen immediately
    private val _refreshTick = MutableStateFlow(0)
    private var adminTapCount = 0
    private var lastAdminTapMs = 0L

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

    private val accountFlow = combine(
        subDataFlow,
        authService?.currentUser ?: MutableStateFlow(null)
    ) { sub, user ->
        Triple(sub.first, sub.second, user)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getPermissionStateFlow(),
        _schedules,
        accountFlow,
        _localState,
        _refreshTick
    ) { perms, scheds, account, local, _ ->
        val (plan, remaining, user) = account
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
            isGrayscaleFatigueEnabled = settingsRepository.isGrayscaleFatigueEnabled(),
            isAdBlockerVpnEnabled = settingsRepository.isAdBlockerVpnEnabled(),
            isAdultBlockEnabled = settingsRepository.isAdultContentBlockEnabled(),
            currentUser = user
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

    fun toggleAdBlockerVpn(enabled: Boolean, context: Context) {
        settingsRepository.setAdBlockerVpnEnabled(enabled)
        if (enabled) {
            com.aegisfocus.app.service.vpn.AegisAdBlockerVpnService.start(context)
        } else {
            // 18+ filtering runs through the same VPN, so it turns off with it
            settingsRepository.setAdultContentBlockEnabled(false)
            com.aegisfocus.app.service.vpn.AegisAdBlockerVpnService.stop(context)
        }
        refreshPermissions()
    }

    /** 18+ blocking uses the family-safe DNS inside the same VPN, so it also keeps the VPN on. */
    fun toggleAdultBlock(enabled: Boolean, context: Context) {
        settingsRepository.setAdultContentBlockEnabled(enabled)
        if (enabled) {
            settingsRepository.setAdBlockerVpnEnabled(true)
        }
        if (settingsRepository.isAdBlockerVpnEnabled()) {
            // Re-run start so the running VPN switches to the new DNS servers
            com.aegisfocus.app.service.vpn.AegisAdBlockerVpnService.start(context)
        }
        refreshPermissions()
    }

    init {
        refreshPermissions()
        loadSchedules()
    }

    fun refreshPermissions() {
        settingsRepository.refreshPermissionState()
        _refreshTick.value += 1
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

    // ---- Admin: one-time license keys (generated and stored on the server) ----
    private val _adminKeys = MutableStateFlow(AdminKeyState())
    val adminKeys: StateFlow<AdminKeyState> = _adminKeys.asStateFlow()

    fun generateKey(tier: String, note: String?) {
        val svc = paymentService ?: return
        _adminKeys.value = _adminKeys.value.copy(isBusy = true, message = null)
        viewModelScope.launch {
            val r = svc.generateLicenseKey(tier, note?.takeIf { it.isNotBlank() })
            _adminKeys.value = _adminKeys.value.copy(
                isBusy = false,
                lastKey = if (r.success) r.key else _adminKeys.value.lastKey,
                message = if (r.success) null else r.message
            )
            if (r.success) refreshKeys()
        }
    }

    fun refreshKeys() {
        val svc = paymentService ?: return
        viewModelScope.launch {
            val (rows, err) = svc.listLicenseKeys(30)
            _adminKeys.value = _adminKeys.value.copy(keys = rows, message = err ?: _adminKeys.value.message)
        }
    }

    fun revokeKey(key: String) {
        val svc = paymentService ?: return
        viewModelScope.launch {
            val r = svc.revokeLicenseKey(key)
            _adminKeys.value = _adminKeys.value.copy(message = if (r.success) null else r.message)
            refreshKeys()
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

        val now = System.currentTimeMillis()
        if (now - lastAdminTapMs > ADMIN_TAP_WINDOW_MS) adminTapCount = 0
        lastAdminTapMs = now

        adminTapCount++
        if (adminTapCount >= ADMIN_TAP_TARGET) {
            _localState.value = _localState.value.copy(
                isPasswordPromptOpen = true,
                adminPasswordError = null
            )
            adminTapCount = 0
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
                Toast.makeText(context, "Super Admin Mode Unlocked!", Toast.LENGTH_SHORT).show()
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

    private companion object {
        const val ADMIN_TAP_TARGET = 3
        const val ADMIN_TAP_WINDOW_MS = 1_500L
    }
}
