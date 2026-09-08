package com.aegisfocus.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.domain.model.PermissionState
import com.aegisfocus.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class OnboardingUiState(
    val currentPage: Int = 0,
    val permissions: PermissionState = PermissionState(),
    val isOnboardingComplete: Boolean = false
)

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)

    val uiState: StateFlow<OnboardingUiState> = combine(
        _currentPage,
        settingsRepository.getPermissionStateFlow()
    ) { page, perms ->
        OnboardingUiState(
            currentPage = page,
            permissions = perms,
            isOnboardingComplete = settingsRepository.isOnboardingCompleted()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OnboardingUiState())

    fun refreshPermissions() {
        settingsRepository.refreshPermissionState()
    }

    fun nextPage() {
        if (_currentPage.value < 2) {
            _currentPage.value += 1
        } else {
            completeOnboarding()
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
        }
    }

    fun completeOnboarding() {
        settingsRepository.setOnboardingCompleted(true)
    }
}
