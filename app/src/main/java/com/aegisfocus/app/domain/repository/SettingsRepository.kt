package com.aegisfocus.app.domain.repository

import com.aegisfocus.app.domain.model.PermissionState
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getPermissionStateFlow(): Flow<PermissionState>
    fun refreshPermissionState(): PermissionState
    fun getDefaultFocusDurationMinutes(): Int
    fun setDefaultFocusDurationMinutes(minutes: Int)
    fun getEmergencyExitCooldownSeconds(): Int
    fun setEmergencyExitCooldownSeconds(seconds: Int)
    fun isStrictModeFrictionPhraseEnabled(): Boolean
    fun setStrictModeFrictionPhraseEnabled(enabled: Boolean)
    fun isDarkTheme(): Boolean
    fun setDarkTheme(isDark: Boolean)
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(completed: Boolean)
}
