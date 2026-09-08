package com.aegisfocus.app.domain.model

data class PermissionState(
    val hasUsageAccess: Boolean = false,
    val hasAccessibilityService: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasExactAlarmPermission: Boolean = false
) {
    val isFullyConfigured: Boolean
        get() = hasUsageAccess && hasAccessibilityService && hasNotificationPermission

    val coreBlockingReady: Boolean
        get() = hasAccessibilityService || hasUsageAccess
}
