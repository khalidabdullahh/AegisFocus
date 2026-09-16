package com.aegisfocus.app.presentation.focus

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.domain.model.AppInfo
import com.aegisfocus.app.domain.model.FocusProfile
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.repository.AppDiscoveryRepository
import com.aegisfocus.app.domain.repository.ProfileRepository
import com.aegisfocus.app.domain.repository.SettingsRepository
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
import com.aegisfocus.app.engine.FocusSessionManager
import com.aegisfocus.app.service.receiver.AegisDeviceAdminReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FocusUiState(
    val sessionName: String = "Deep Focus",
    val selectedDurationMinutes: Int = 25,
    val selectedMode: SessionMode = SessionMode.NORMAL,
    val selectedProfileId: Long? = null,
    val profiles: List<FocusProfile> = emptyList(),
    val installedApps: List<AppInfo> = emptyList(),
    val selectedBlockedPackageNames: Set<String> = emptySet(),
    val isEmergencyDialogOpen: Boolean = false,
    val emergencyCooldownSeconds: Int = 5,
    val isStrictFrictionPhraseEnabled: Boolean = true,
    val hasDeviceAdminPermission: Boolean = false,
    val isDeviceAdminPromptOpen: Boolean = false,
    val isPro: Boolean = false,
    val maxAllowedApps: Int = 3,
    val maxAllowedMinutes: Int = 120,
    val errorMessage: String? = null
)

class FocusViewModel(
    private val focusSessionManager: FocusSessionManager,
    private val profileRepository: ProfileRepository,
    private val appDiscoveryRepository: AppDiscoveryRepository,
    private val settingsRepository: SettingsRepository,
    private val entitlementManager: EntitlementManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    val activeSession: StateFlow<FocusSession?> = focusSessionManager.activeSession
    val remainingMillis: StateFlow<Long> = focusSessionManager.remainingMillis

    init {
        loadInitialData()
        observePermissionState()
        observeBlockedApps()
        observeEntitlements()
    }

    private fun observePermissionState() {
        viewModelScope.launch {
            settingsRepository.getPermissionStateFlow().collect { perms ->
                _uiState.value = _uiState.value.copy(hasDeviceAdminPermission = perms.hasDeviceAdminPermission)
            }
        }
    }

    private fun observeBlockedApps() {
        viewModelScope.launch {
            appDiscoveryRepository.getBlockedPackageNamesFlow().collect { blockedSet ->
                _uiState.value = _uiState.value.copy(selectedBlockedPackageNames = blockedSet)
            }
        }
    }

    private fun observeEntitlements() {
        viewModelScope.launch {
            entitlementManager.activePlan.collect { plan ->
                val isPro = entitlementManager.isPro()
                val maxApps = entitlementManager.maxBlockedApps()
                val maxMins = entitlementManager.maxStrictDurationMinutes()
                _uiState.value = _uiState.value.copy(
                    isPro = isPro,
                    maxAllowedApps = maxApps,
                    maxAllowedMinutes = maxMins
                )
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            val defaultDuration = settingsRepository.getDefaultFocusDurationMinutes()
            val cooldown = settingsRepository.getEmergencyExitCooldownSeconds()
            val phraseEnabled = settingsRepository.isStrictModeFrictionPhraseEnabled()
            val profiles = profileRepository.getAllProfiles()
            val installed = appDiscoveryRepository.loadInstalledApps()
            val defaultBlocked = appDiscoveryRepository.getBlockedPackageNames().toSet()
            val isPro = entitlementManager.isPro()
            val maxApps = entitlementManager.maxBlockedApps()
            val maxMins = entitlementManager.maxStrictDurationMinutes()

            _uiState.value = _uiState.value.copy(
                selectedDurationMinutes = defaultDuration,
                emergencyCooldownSeconds = cooldown,
                isStrictFrictionPhraseEnabled = phraseEnabled,
                profiles = profiles,
                installedApps = installed,
                selectedBlockedPackageNames = defaultBlocked,
                isPro = isPro,
                maxAllowedApps = maxApps,
                maxAllowedMinutes = maxMins
            )
        }
    }

    fun setSessionName(name: String) {
        if (activeSession.value == null) {
            _uiState.value = _uiState.value.copy(sessionName = name)
        }
    }

    fun setDuration(minutes: Int) {
        if (activeSession.value == null) {
            val state = _uiState.value
            val isPro = state.isPro
            val clampedMinutes = if (!isPro && state.selectedMode == SessionMode.STRICT && minutes > 120) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Free tier Strict Mode is limited to 2 hours (120 min). Upgrade to Pro for unlimited duration."
                )
                120
            } else {
                minutes
            }
            _uiState.value = _uiState.value.copy(selectedDurationMinutes = clampedMinutes)
        }
    }

    fun setMode(mode: SessionMode) {
        if (activeSession.value == null) {
            var duration = _uiState.value.selectedDurationMinutes
            if (mode == SessionMode.STRICT && !_uiState.value.isPro && duration > 120) {
                duration = 120
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Free tier Strict Mode is limited to 2 hours (120 min)."
                )
            }
            _uiState.value = _uiState.value.copy(
                selectedMode = mode,
                selectedDurationMinutes = duration
            )
        }
    }

    fun selectProfile(profile: FocusProfile) {
        if (activeSession.value == null) {
            _uiState.value = _uiState.value.copy(
                sessionName = profile.name,
                selectedDurationMinutes = profile.defaultDurationMinutes,
                selectedMode = if (profile.isStrictModeDefault) SessionMode.STRICT else SessionMode.NORMAL,
                selectedProfileId = profile.id,
                selectedBlockedPackageNames = if (profile.blockedPackageNames.isNotEmpty()) {
                    profile.blockedPackageNames.toSet()
                } else {
                    _uiState.value.selectedBlockedPackageNames
                }
            )
        }
    }

    fun toggleAppBlocked(packageName: String, isBlocked: Boolean) {
        if (activeSession.value == null) {
            val state = _uiState.value
            val current = state.selectedBlockedPackageNames

            if (isBlocked && !state.isPro && current.size >= 3 && !current.contains(packageName)) {
                _uiState.value = state.copy(
                    errorMessage = "Free tier is limited to 3 shielded apps. Upgrade to Pro for unlimited apps."
                )
                return
            }

            viewModelScope.launch {
                appDiscoveryRepository.setAppBlocked(packageName, isBlocked)
            }
        }
    }

    fun removeBlockedApp(packageName: String) {
        toggleAppBlocked(packageName, false)
    }

    fun startFocusSession(skipAdminCheck: Boolean = false) {
        val state = _uiState.value

        // Check Free tier constraints
        if (state.selectedMode == SessionMode.STRICT && !state.isPro) {
            if (state.selectedDurationMinutes > 120) {
                _uiState.value = state.copy(
                    errorMessage = "Free tier Strict Mode is limited to 2 hours (120 min). Upgrade to Pro for longer sessions."
                )
                return
            }
            if (state.selectedBlockedPackageNames.size > 3) {
                _uiState.value = state.copy(
                    errorMessage = "Free tier allows blocking up to 3 apps. Upgrade to Pro for unlimited apps."
                )
                return
            }
        }

        if (state.selectedMode == SessionMode.STRICT && !state.hasDeviceAdminPermission && !skipAdminCheck) {
            _uiState.value = state.copy(isDeviceAdminPromptOpen = true)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeviceAdminPromptOpen = false)
            val result = focusSessionManager.startSession(
                name = state.sessionName,
                durationMinutes = state.selectedDurationMinutes,
                mode = state.selectedMode,
                blockedPackageNames = state.selectedBlockedPackageNames.toList(),
                profileId = state.selectedProfileId
            )
            result.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
            }
        }
    }

    fun dismissDeviceAdminPrompt() {
        _uiState.value = _uiState.value.copy(isDeviceAdminPromptOpen = false)
    }

    fun openDeviceAdminSettings(context: Context) {
        val adminComponent = ComponentName(context, AegisDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activate Anti-Uninstall Protection so Aegis Focus cannot be uninstalled during strict focus sessions."
            )
            if (context !is Activity) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        runCatching { context.startActivity(intent) }
    }

    fun cancelActiveSession() {
        viewModelScope.launch {
            val result = focusSessionManager.cancelSession()
            result.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
            }
        }
    }

    fun showEmergencyDialog() {
        _uiState.value = _uiState.value.copy(isEmergencyDialogOpen = true)
    }

    fun dismissEmergencyDialog() {
        _uiState.value = _uiState.value.copy(isEmergencyDialogOpen = false)
    }

    fun confirmEmergencyExit(reason: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEmergencyDialogOpen = false)
            focusSessionManager.triggerEmergencyExit(reason)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
