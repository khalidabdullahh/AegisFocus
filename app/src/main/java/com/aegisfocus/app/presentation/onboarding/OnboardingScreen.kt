package com.aegisfocus.app.presentation.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.R
import com.aegisfocus.app.core.components.AegisCompactButton
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            for (i in 0..2) {
                val isSelected = uiState.currentPage == i
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (isSelected) 24.dp else 6.dp)
                        .background(
                            color = if (isSelected) CyanAccent else DeepSurface,
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        }

        // Page Content
        AnimatedContent(
            targetState = uiState.currentPage,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding_page"
        ) { page ->
            when (page) {
                0 -> OnboardingStep1()
                1 -> OnboardingStep2()
                else -> OnboardingStep3(
                    permissions = uiState.permissions,
                    onConfigureUsage = { viewModel.openUsageAccessSettings(context) },
                    onConfigureAccessibility = { viewModel.openAccessibilitySettings(context) },
                    onConfigureNotification = { viewModel.openNotificationSettings(context) },
                    onConfigureDeviceAdmin = { viewModel.openDeviceAdminSettings(context) },
                    onRefresh = { viewModel.refreshPermissions() }
                )
            }
        }

        val hasRequiredPermissions = uiState.permissions.hasAccessibilityService &&
                uiState.permissions.hasUsageAccess &&
                uiState.permissions.hasNotificationPermission

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.currentPage > 0) {
                TextButton(onClick = { viewModel.previousPage() }) {
                    Text("Back", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                Spacer(modifier = Modifier.width(60.dp))
            }

            AegisPrimaryButton(
                text = when {
                    uiState.currentPage < 2 -> "Continue"
                    hasRequiredPermissions -> "Enter Aegis Focus"
                    else -> "Grant permissions"
                },
                onClick = {
                    if (uiState.currentPage < 2) {
                        viewModel.nextPage()
                    } else if (hasRequiredPermissions) {
                        viewModel.completeOnboarding()
                        onComplete()
                    } else {
                        // Jump straight to the first permission that is still missing
                        when {
                            !uiState.permissions.hasUsageAccess -> viewModel.openUsageAccessSettings(context)
                            !uiState.permissions.hasAccessibilityService -> viewModel.openAccessibilitySettings(context)
                            else -> viewModel.openNotificationSettings(context)
                        }
                    }
                },
                wrapContent = true
            )
        }
    }
}

@Composable
private fun OnboardingStep1() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = ElevatedSurface,
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.app_slogan),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.3).sp,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_subtitle),
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun OnboardingStep2() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = ElevatedSurface,
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Strict Mode & Immutable Rules",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "When activated, Strict Mode locks your session parameters and app shield rules. There is no emergency exit in Strict Mode, so choose your duration with care.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun OnboardingStep3(
    permissions: com.aegisfocus.app.domain.model.PermissionState,
    onConfigureUsage: () -> Unit,
    onConfigureAccessibility: () -> Unit,
    onConfigureNotification: () -> Unit,
    onConfigureDeviceAdmin: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Grant Required Permissions",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "To safeguard your focus and block distractions, Aegis Focus requires the following permissions. All data stays 100% on your device.",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Usage Access Card (Required)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, if (permissions.hasUsageAccess) EmeraldFocus.copy(alpha = 0.5f) else SubtleBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (permissions.hasUsageAccess) Icons.Default.CheckCircle else Icons.Default.BarChart,
                        contentDescription = null,
                        tint = if (permissions.hasUsageAccess) EmeraldFocus else CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Usage Access", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REQUIRED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (permissions.hasUsageAccess) EmeraldFocus else CyanAccent)
                        }
                        Text("Tracks screen time & distraction apps", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                if (!permissions.hasUsageAccess) {
                    AegisCompactButton(
                        text = "Grant",
                        onClick = onConfigureUsage
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Accessibility Service Card (Required)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, if (permissions.hasAccessibilityService) EmeraldFocus.copy(alpha = 0.5f) else SubtleBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (permissions.hasAccessibilityService) Icons.Default.CheckCircle else Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = if (permissions.hasAccessibilityService) EmeraldFocus else CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Accessibility Service", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REQUIRED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (permissions.hasAccessibilityService) EmeraldFocus else CyanAccent)
                        }
                        Text("Real-time blocking of restricted apps", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                if (!permissions.hasAccessibilityService) {
                    AegisCompactButton(
                        text = "Enable",
                        onClick = onConfigureAccessibility
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Notification Access Card (Required)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, if (permissions.hasNotificationPermission) EmeraldFocus.copy(alpha = 0.5f) else SubtleBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (permissions.hasNotificationPermission) Icons.Default.CheckCircle else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (permissions.hasNotificationPermission) EmeraldFocus else CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notification Permission", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REQUIRED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (permissions.hasNotificationPermission) EmeraldFocus else CyanAccent)
                        }
                        Text("Live timer status and discipline alarms", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                if (!permissions.hasNotificationPermission) {
                    AegisCompactButton(
                        text = "Allow",
                        onClick = onConfigureNotification
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Anti-Uninstall Device Admin Card (Optional)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, if (permissions.hasDeviceAdminPermission) EmeraldFocus.copy(alpha = 0.5f) else SubtleBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (permissions.hasDeviceAdminPermission) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (permissions.hasDeviceAdminPermission) EmeraldFocus else CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Anti-Uninstall Admin", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RECOMMENDED", fontSize = 9.sp, fontWeight = FontWeight.Normal, color = TextMuted)
                        }
                        Text("Prevents deleting app during Strict Mode", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                if (!permissions.hasDeviceAdminPermission) {
                    AegisCompactButton(
                        text = "Activate",
                        onClick = onConfigureDeviceAdmin,
                        containerColor = ElevatedSurface,
                        contentColor = CyanAccent
                    )
                }
            }
        }
    }
}
