package com.aegisfocus.app.data.repository

import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.aegisfocus.app.domain.model.PermissionState
import com.aegisfocus.app.domain.repository.SettingsRepository
import com.aegisfocus.app.service.receiver.AegisDeviceAdminReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("aegis_settings", Context.MODE_PRIVATE)
    private val _permissionState = MutableStateFlow(checkAllPermissions())

    override fun getPermissionStateFlow(): Flow<PermissionState> = _permissionState.asStateFlow()

    override fun refreshPermissionState(): PermissionState {
        val current = checkAllPermissions()
        _permissionState.value = current
        return current
    }

    private fun checkAllPermissions(): PermissionState {
        return PermissionState(
            hasUsageAccess = hasUsageStatsPermission(),
            hasAccessibilityService = isAccessibilityServiceEnabled(),
            hasNotificationPermission = hasNotificationPermission(),
            hasOverlayPermission = hasOverlayPermission(),
            hasExactAlarmPermission = hasExactAlarmPermission(),
            hasDeviceAdminPermission = isDeviceAdminActive()
        )
    }

    private fun isDeviceAdminActive(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        val adminComponent = ComponentName(context, AegisDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains("${context.packageName}/")
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    override fun getDefaultFocusDurationMinutes(): Int {
        return prefs.getInt(KEY_DEFAULT_DURATION, 25)
    }

    override fun setDefaultFocusDurationMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DEFAULT_DURATION, minutes).apply()
    }

    override fun getEmergencyExitCooldownSeconds(): Int {
        return prefs.getInt(KEY_EMERGENCY_COOLDOWN, 5)
    }

    override fun setEmergencyExitCooldownSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_EMERGENCY_COOLDOWN, seconds).apply()
    }

    override fun isStrictModeFrictionPhraseEnabled(): Boolean {
        return prefs.getBoolean(KEY_STRICT_FRICTION_PHRASE, true)
    }

    override fun setStrictModeFrictionPhraseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_FRICTION_PHRASE, enabled).apply()
    }

    override fun isDarkTheme(): Boolean {
        return prefs.getBoolean(KEY_DARK_THEME, true)
    }

    override fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, isDark).apply()
    }

    override fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
    }

    override fun isShortsBlockerEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHORTS_BLOCKER_ENABLED, true)
    }

    override fun setShortsBlockerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHORTS_BLOCKER_ENABLED, enabled).apply()
    }

    override fun isGrayscaleFatigueEnabled(): Boolean {
        return prefs.getBoolean(KEY_GRAYSCALE_FATIGUE_ENABLED, false)
    }

    override fun setGrayscaleFatigueEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GRAYSCALE_FATIGUE_ENABLED, enabled).apply()
    }

    override fun isAdBlockerVpnEnabled(): Boolean {
        return prefs.getBoolean(KEY_ADBLOCKER_VPN_ENABLED, false)
    }

    override fun setAdBlockerVpnEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ADBLOCKER_VPN_ENABLED, enabled).apply()
    }

    override fun isAdultContentBlockEnabled(): Boolean {
        return prefs.getBoolean(KEY_ADULT_BLOCK_ENABLED, false)
    }

    override fun setAdultContentBlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ADULT_BLOCK_ENABLED, enabled).apply()
    }

    override fun getDailyFocusGoalMinutes(): Int {
        return prefs.getInt(KEY_DAILY_FOCUS_GOAL_MINUTES, 120)
    }

    override fun setDailyFocusGoalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_FOCUS_GOAL_MINUTES, minutes).apply()
    }

    companion object {
        private const val KEY_DEFAULT_DURATION = "default_focus_duration"
        private const val KEY_EMERGENCY_COOLDOWN = "emergency_cooldown_seconds"
        private const val KEY_STRICT_FRICTION_PHRASE = "strict_friction_phrase"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_SHORTS_BLOCKER_ENABLED = "shorts_blocker_enabled"
        private const val KEY_GRAYSCALE_FATIGUE_ENABLED = "grayscale_fatigue_enabled"
        private const val KEY_ADBLOCKER_VPN_ENABLED = "adblocker_vpn_enabled"
        private const val KEY_ADULT_BLOCK_ENABLED = "adult_block_enabled"
        private const val KEY_DAILY_FOCUS_GOAL_MINUTES = "daily_focus_goal_minutes"
    }
}
