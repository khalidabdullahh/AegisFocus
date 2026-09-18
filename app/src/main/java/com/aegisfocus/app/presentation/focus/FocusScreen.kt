package com.aegisfocus.app.presentation.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.components.CalmMindfulnessDialog
import com.aegisfocus.app.core.components.ModeSelector
import com.aegisfocus.app.core.components.ProfileCard
import com.aegisfocus.app.core.components.SectionHeader
import com.aegisfocus.app.core.theme.AlertStrict
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.domain.model.AppInfo
import com.aegisfocus.app.domain.model.FocusProfile
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode

@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    onNavigateToApps: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val remainingMillis by viewModel.remainingMillis.collectAsState()
    val context = LocalContext.current

    if (activeSession != null) {
        ActiveFocusScreen(
            session = activeSession!!,
            remainingMillis = remainingMillis,
            onCancel = { viewModel.cancelActiveSession() },
            onEmergencyExit = { viewModel.showEmergencyDialog() }
        )
    } else {
        FocusConfigurationScreen(
            uiState = uiState,
            onNameChange = { viewModel.setSessionName(it) },
            onDurationChange = { viewModel.setDuration(it) },
            onModeChange = { viewModel.setMode(it) },
            onProfileSelected = { viewModel.selectProfile(it) },
            onToggleApp = { pkg, blocked -> viewModel.toggleAppBlocked(pkg, blocked) },
            onRemoveApp = { pkg -> viewModel.removeBlockedApp(pkg) },
            onStartSession = { viewModel.startFocusSession() },
            onNavigateToApps = onNavigateToApps,
            onNavigateToPaywall = onNavigateToPaywall,
            onClearError = { viewModel.clearError() },
            modifier = modifier
        )
    }

    if (uiState.isDeviceAdminPromptOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeviceAdminPrompt() },
            title = {
                Text(
                    text = "Activate Anti-Uninstall Protection",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Strict Mode provides anti-uninstall protection. To prevent accidental deactivation or uninstallation during your focus session, grant Device Administrator permission.",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                AegisPrimaryButton(
                    text = "Activate Device Admin",
                    onClick = {
                        viewModel.openDeviceAdminSettings(context)
                        viewModel.dismissDeviceAdminPrompt()
                    }
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.startFocusSession(skipAdminCheck = true)
                    }
                ) {
                    Text("Start Session Anyway", color = TextMuted, fontSize = 13.sp)
                }
            },
            containerColor = DeepSurface
        )
    }

    if (uiState.isEmergencyDialogOpen) {
        CalmMindfulnessDialog(
            title = "Emergency Exit Request",
            message = "Exiting early breaks your discipline streak and records an emergency event. Take a mindful breath before proceeding.",
            confirmButtonText = "Confirm Exit",
            cooldownSeconds = uiState.emergencyCooldownSeconds,
            requireConfirmationPhrase = uiState.isStrictFrictionPhraseEnabled,
            onConfirm = { reason -> viewModel.confirmEmergencyExit(reason) },
            onDismiss = { viewModel.dismissEmergencyDialog() }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusConfigurationScreen(
    uiState: FocusUiState,
    onNameChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onModeChange: (SessionMode) -> Unit,
    onProfileSelected: (FocusProfile) -> Unit,
    onToggleApp: (String, Boolean) -> Unit,
    onRemoveApp: (String) -> Unit,
    onStartSession: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Configure Session",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Deliberate focus, undisturbed mind.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        // Error message banner
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.errorMessage?.let { errorMsg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertStrict.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AlertStrict.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertStrict,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                        if (errorMsg.contains("Pro", ignoreCase = true) || errorMsg.contains("Free", ignoreCase = true)) {
                            TextButton(onClick = onNavigateToPaywall) {
                                Text("Upgrade", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Session Name Input
        OutlinedTextField(
            value = uiState.sessionName,
            onValueChange = onNameChange,
            label = { Text("Session Goal / Name", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = SubtleBorder,
                focusedContainerColor = DeepSurface,
                unfocusedContainerColor = DeepSurface
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Focus Profiles Carousel
        if (uiState.profiles.isNotEmpty()) {
            SectionHeader(title = "Focus Profiles")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.profiles) { profile ->
                    ProfileCard(
                        profile = profile,
                        isSelected = uiState.selectedProfileId == profile.id,
                        onClick = { onProfileSelected(profile) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Discipline Mode (Normal vs Strict)
        SectionHeader(title = "Discipline Mode")
        ModeSelector(
            selectedMode = uiState.selectedMode,
            onModeSelected = onModeChange
        )

        if (uiState.selectedMode == SessionMode.STRICT) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, if (uiState.isPro) CyanAccent.copy(alpha = 0.3f) else SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.isPro) Icons.Default.Star else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (uiState.isPro) CyanAccent else AlertStrict,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isPro) {
                            "Strict Mode: Anti-uninstall protection active. Unlimited duration & apps (Pro unlocked)."
                        } else {
                            "Strict Mode Free tier: Max 2 hours (120 min) & max 3 shielded apps."
                        },
                        fontSize = 12.sp,
                        color = if (uiState.isPro) TextSecondary else AlertStrict
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Duration Presets & Stepper
        SectionHeader(
            title = "Duration",
            actionText = if (uiState.selectedMode == SessionMode.STRICT && !uiState.isPro) "Free: Max 2h" else null
        )

        val durationPresets = if (uiState.selectedMode == SessionMode.STRICT) {
            if (uiState.isPro) listOf(30, 60, 120, 180, 360) else listOf(15, 30, 60, 90, 120)
        } else {
            listOf(25, 50, 90)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            durationPresets.forEach { mins ->
                val isSelected = uiState.selectedDurationMinutes == mins
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDurationChange(mins) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) ElevatedSurface else DeepSurface,
                    border = BorderStroke(1.dp, if (isSelected) CyanAccent else SubtleBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (mins >= 60) "${mins / 60}h${if (mins % 60 != 0) " ${mins % 60}m" else ""}" else "${mins}m",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CyanAccent else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Duration Stepper
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Custom Duration",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (uiState.selectedDurationMinutes > 5) {
                                onDurationChange(uiState.selectedDurationMinutes - 5)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = CyanAccent
                        )
                    }

                    Text(
                        text = "${uiState.selectedDurationMinutes} min",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            val maxLimit = if (uiState.selectedMode == SessionMode.STRICT && !uiState.isPro) 120 else 720
                            if (uiState.selectedDurationMinutes < maxLimit) {
                                onDurationChange(uiState.selectedDurationMinutes + 5)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = CyanAccent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // =========================================================================
        // LIVE SHIELDED APPS SELECTION SECTION (Shows live selected apps immediately)
        // =========================================================================
        val selectedPkgs = uiState.selectedBlockedPackageNames
        val blockedAppsList = uiState.installedApps.filter { it.packageName in selectedPkgs }
        val maxAppsLabel = if (uiState.isPro) "Unlimited" else "Max 3 (Free)"

        SectionHeader(
            title = "Shielded Applications",
            actionText = "${selectedPkgs.size} selected • $maxAppsLabel"
        )

        // Live Selected Apps Display
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (blockedAppsList.isNotEmpty()) {
                    Text(
                        text = "Apps to be blocked during this session:",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        blockedAppsList.forEach { app ->
                            SelectedAppChip(
                                appName = app.appName,
                                onRemove = { onRemoveApp(app.packageName) }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "No apps shielded yet. Tap below or search to select apps.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick add recommendations row
                val unselectedApps = uiState.installedApps.filter { it.packageName !in selectedPkgs }.take(6)
                if (unselectedApps.isNotEmpty()) {
                    Text(
                        text = "Quick add suggestions:",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(unselectedApps) { app ->
                            QuickAddAppChip(
                                app = app,
                                onAdd = { onToggleApp(app.packageName, true) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Full Apps Screen Navigation Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElevatedSurface,
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToApps)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Apps, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Search & Manage All Apps (${uiState.installedApps.size} installed)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Icon(Icons.Default.Add, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Primary Start CTA
        AegisPrimaryButton(
            text = if (uiState.selectedMode == SessionMode.STRICT) "LOCK IN & START STRICT FOCUS" else "START FOCUS SESSION",
            onClick = onStartSession
        )

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun SelectedAppChip(
    appName: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CyanAccent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(CyanAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appName.take(1).uppercase(),
                    color = ObsidianBg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = appName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove $appName",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickAddAppChip(
    app: AppInfo,
    onAdd: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ElevatedSurface,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.clickable(onClick = onAdd)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(TextMuted.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = app.appName,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = CyanAccent,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ActiveFocusScreen(
    session: FocusSession,
    remainingMillis: Long,
    onCancel: () -> Unit,
    onEmergencyExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (session.mode == SessionMode.STRICT) AlertStrict.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f),
                border = BorderStroke(
                    1.dp,
                    if (session.mode == SessionMode.STRICT) AlertStrict.copy(alpha = 0.4f) else CyanAccent.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (session.mode == SessionMode.STRICT) Icons.Default.Lock else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (session.mode == SessionMode.STRICT) AlertStrict else CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (session.mode == SessionMode.STRICT) "STRICT MODE ACTIVE" else "FOCUS SESSION ACTIVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (session.mode == SessionMode.STRICT) AlertStrict else CyanAccent,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = session.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Timer Display
            Text(
                text = TimeUtils.formatRemainingMillis(remainingMillis),
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${session.durationMinutes} minute focus commitment",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Active Shielded Apps Summary
            if (session.blockedPackageNames.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DeepSurface,
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${session.blockedPackageNames.size} Applications Shielded",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Distracting app access is blocked and monitored.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (session.mode == SessionMode.STRICT) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertStrict.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, AlertStrict.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEmergencyExit)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AlertStrict, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Emergency Breakout",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertStrict
                        )
                    }
                }
            } else {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "End Focus Session",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
