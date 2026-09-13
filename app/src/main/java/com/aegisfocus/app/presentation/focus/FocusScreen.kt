package com.aegisfocus.app.presentation.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.components.CalmMindfulnessDialog
import com.aegisfocus.app.core.components.ModeSelector
import com.aegisfocus.app.core.components.ProfileCard
import com.aegisfocus.app.core.components.SectionHeader
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.domain.model.SessionMode

@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    onNavigateToApps: () -> Unit,
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
            onStartSession = { viewModel.startFocusSession() },
            onNavigateToApps = onNavigateToApps,
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

@Composable
private fun FocusConfigurationScreen(
    uiState: FocusUiState,
    onNameChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onModeChange: (SessionMode) -> Unit,
    onProfileSelected: (com.aegisfocus.app.domain.model.FocusProfile) -> Unit,
    onStartSession: () -> Unit,
    onNavigateToApps: () -> Unit,
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

        // Duration Presets & Stepper
        SectionHeader(title = "Duration")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(25, 50, 90).forEach { mins ->
                val isSelected = uiState.selectedDurationMinutes == mins
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDurationChange(mins) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) ElevatedSurface else DeepSurface,
                    border = BorderStroke(1.dp, if (isSelected) CyanAccent else SubtleBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${mins}m",
                            fontSize = 17.sp,
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
                            if (uiState.selectedDurationMinutes < 720) {
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

        // Mode Selector (Normal vs Strict)
        SectionHeader(title = "Discipline Mode")
        ModeSelector(
            selectedMode = uiState.selectedMode,
            onModeSelected = onModeChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Shielded Apps Selection Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToApps),
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Column {
                        Text(
                            text = "Shielded Applications",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${uiState.selectedBlockedPackageNames.size} apps will be blocked",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Primary Start CTA
        AegisPrimaryButton(
            text = "START FOCUS SESSION",
            onClick = onStartSession
        )

        Spacer(modifier = Modifier.height(36.dp))
    }
}
