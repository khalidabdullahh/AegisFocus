package com.aegisfocus.app.presentation.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.domain.model.AppInfo
import com.aegisfocus.app.domain.model.FocusProfile
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.repository.AppDiscoveryRepository
import com.aegisfocus.app.domain.repository.ProfileRepository
import com.aegisfocus.app.domain.repository.SettingsRepository
import com.aegisfocus.app.engine.FocusSessionManager
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
    val errorMessage: String? = null
)

class FocusViewModel(
    private val focusSessionManager: FocusSessionManager,
    private val profileRepository: ProfileRepository,
    private val appDiscoveryRepository: AppDiscoveryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    val activeSession: StateFlow<FocusSession?> = focusSessionManager.activeSession
    val remainingMillis: StateFlow<Long> = focusSessionManager.remainingMillis

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            val defaultDuration = settingsRepository.getDefaultFocusDurationMinutes()
            val cooldown = settingsRepository.getEmergencyExitCooldownSeconds()
            val phraseEnabled = settingsRepository.isStrictModeFrictionPhraseEnabled()
            val profiles = profileRepository.getAllProfiles()
            val installed = appDiscoveryRepository.loadInstalledApps()
            val defaultBlocked = appDiscoveryRepository.getBlockedPackageNames().toSet()

            _uiState.value = _uiState.value.copy(
                selectedDurationMinutes = defaultDuration,
                emergencyCooldownSeconds = cooldown,
                isStrictFrictionPhraseEnabled = phraseEnabled,
                profiles = profiles,
                installedApps = installed,
                selectedBlockedPackageNames = defaultBlocked
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
            _uiState.value = _uiState.value.copy(selectedDurationMinutes = minutes)
        }
    }

    fun setMode(mode: SessionMode) {
        if (activeSession.value == null) {
            _uiState.value = _uiState.value.copy(selectedMode = mode)
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
            val current = _uiState.value.selectedBlockedPackageNames.toMutableSet()
            if (isBlocked) current.add(packageName) else current.remove(packageName)
            _uiState.value = _uiState.value.copy(selectedBlockedPackageNames = current)

            viewModelScope.launch {
                appDiscoveryRepository.setAppBlocked(packageName, isBlocked)
            }
        }
    }

    fun startFocusSession() {
        val state = _uiState.value
        viewModelScope.launch {
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
