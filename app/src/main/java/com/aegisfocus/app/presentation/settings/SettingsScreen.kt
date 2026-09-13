package com.aegisfocus.app.presentation.settings

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import com.aegisfocus.app.core.theme.ObsidianBg
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisOutlinedButton
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.components.PermissionStatusRow
import com.aegisfocus.app.core.components.ScheduleCard
import com.aegisfocus.app.core.components.SectionHeader
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.data.remote.SupabaseAuthService
import com.aegisfocus.app.domain.model.Schedule
import com.aegisfocus.app.domain.subscription.LicenseKeyValidator

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAuth: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Settings & Guardrails",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Permissions, cloud sync, and discipline rules.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Account & Cloud Sync Card
        val user = uiState.currentUser
        if (user == null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAuth() },
                shape = RoundedCornerShape(16.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("Account & Cloud Sync", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Sign in to backup & sync your focus stats", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CyanAccent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, CyanAccent)
                    ) {
                        Text(
                            text = "Sign In",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAuth() },
                shape = RoundedCornerShape(16.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val avatarBitmap = remember(user.avatarUri) {
                            user.avatarUri?.let { path ->
                                try {
                                    val file = File(path)
                                    if (file.exists()) {
                                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                                    } else null
                                } catch (e: Exception) { null }
                            }
                        }

                        val initial = user.displayName?.firstOrNull()?.uppercaseChar()
                            ?: user.email.firstOrNull()?.uppercaseChar()
                            ?: 'A'
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.2f))
                                .border(BorderStroke(1.5.dp, CyanAccent), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = initial.toString(),
                                    color = CyanAccent,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Column {
                            Text(
                                text = user.displayName?.takeIf { it.isNotBlank() } ?: user.email.substringBefore("@"),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = user.email,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val isSuper = SupabaseAuthService.isSuperAdmin(user.email)
                            Text(
                                text = if (isSuper) "👑 Super Admin • Lifetime Pro" else "✓ Cloud Sync Active",
                                color = if (isSuper) CyanAccent else EmeraldFocus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ElevatedSurface,
                        border = BorderStroke(1.dp, SubtleBorder)
                    ) {
                        Text(
                            text = "Edit Profile",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (user != null && SupabaseAuthService.isSuperAdmin(user.email)) {
            Spacer(modifier = Modifier.height(12.dp))
            SuperAdminKeyGeneratorCard()
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Aegis Pro Membership Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToPaywall() },
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text("Aegis Pro Membership", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Unlimited Strict Mode, Website Blocking & Analytics", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CyanAccent,
                    border = BorderStroke(1.dp, CyanAccent)
                ) {
                    Text(
                        text = "Upgrade",
                        color = ObsidianBg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // System Permissions Section
        SectionHeader(
            title = "Android Permissions",
            actionText = "Refresh",
            onActionClick = { viewModel.refreshPermissions() }
        )

        PermissionStatusRow(
            title = "Usage Access",
            description = "Enables screen time tracking, behavioral analytics, and background foreground verification.",
            isGranted = uiState.permissions.hasUsageAccess,
            onGrantClick = { viewModel.openUsageAccessSettings(context) },
            icon = Icons.Default.BarChart
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionStatusRow(
            title = "Accessibility Blocker Service",
            description = "Enables real-time instantaneous detection and gentle blocking intervention when distracting apps are opened.",
            isGranted = uiState.permissions.hasAccessibilityService,
            onGrantClick = { viewModel.openAccessibilitySettings(context) },
            icon = Icons.Default.AccessibilityNew
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionStatusRow(
            title = "Persistent Notifications",
            description = "Keeps active focus timers visible and prevents Android from killing ongoing discipline sessions.",
            isGranted = uiState.permissions.hasNotificationPermission,
            onGrantClick = { viewModel.openNotificationSettings(context) },
            icon = Icons.Default.Notifications
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionStatusRow(
            title = "Display Over Apps",
            description = "Permits calm full-screen intervention overlays over blocked applications.",
            isGranted = uiState.permissions.hasOverlayPermission,
            onGrantClick = { viewModel.openOverlaySettings(context) },
            icon = Icons.Default.Layers,
            isRequired = false
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionStatusRow(
            title = "Exact Alarms",
            description = "Ensures scheduled routines trigger precisely at configured times.",
            isGranted = uiState.permissions.hasExactAlarmPermission,
            onGrantClick = { viewModel.openExactAlarmSettings(context) },
            icon = Icons.Default.Alarm,
            isRequired = false
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionStatusRow(
            title = "Anti-Uninstall Device Admin",
            description = "Prevents unauthorized uninstallation or bypass during active Strict Mode focus sessions.",
            isGranted = uiState.permissions.hasDeviceAdminPermission,
            onGrantClick = { viewModel.openDeviceAdminSettings(context) },
            icon = Icons.Default.Security,
            isRequired = false
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Focus & Strict Mode Preferences
        SectionHeader(title = "Discipline Preferences")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Emergency Exit Cooldown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emergency Exit Delay",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Friction cooldown to prevent impulsive session breaks",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15).forEach { sec ->
                            val isSelected = uiState.emergencyCooldownSeconds == sec
                            Surface(
                                modifier = Modifier.clickable { viewModel.setEmergencyCooldown(sec) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ElevatedSurface else DeepSurface,
                                border = BorderStroke(1.dp, if (isSelected) CyanAccent else SubtleBorder)
                            ) {
                                Text(
                                    text = "${sec}s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyanAccent else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Mindful Confirmation Phrase
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Require Mindful Phrase",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Requires typing \"I choose to break focus\" to exit Strict Mode",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = uiState.isStrictFrictionPhraseEnabled,
                        onCheckedChange = { viewModel.setStrictFrictionPhrase(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = CyanAccent,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DeepSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Scheduled Routines Section
        SectionHeader(
            title = "Scheduled Routines",
            actionText = "+ Add Schedule",
            onActionClick = { viewModel.openAddScheduleDialog() }
        )

        if (uiState.schedules.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Scheduled Routines",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create recurring routines like Study (09:00–13:00) or Sleep (23:00–07:00).",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AegisOutlinedButton(
                        text = "Create First Routine",
                        onClick = { viewModel.openAddScheduleDialog() }
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.schedules.forEach { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onToggle = { isChecked -> viewModel.toggleSchedule(schedule.id, isChecked) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Local-First Privacy Guarantee Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, EmeraldFocus.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = EmeraldFocus,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "100% On-Device Privacy",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aegis Focus operates completely on-device using local SQLite Room database. No analytics, no accounts, and no data ever leaves your device.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About & Version Info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Aegis Focus • v1.0.0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Digital Discipline & Focus Architecture",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    if (uiState.isAddScheduleDialogOpen) {
        AddScheduleDialog(
            onDismiss = { viewModel.closeAddScheduleDialog() },
            onSave = { schedule -> viewModel.addSchedule(schedule) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    var name by remember { mutableStateOf("Deep Work Routine") }
    var startHour by remember { mutableIntStateOf(9) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(12) }
    var endMinute by remember { mutableIntStateOf(0) }
    var isStrict by remember { mutableStateOf(false) }
    var daysBitmask by remember { mutableIntStateOf(62) } // Mon - Fri

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Create Focus Routine",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Routine Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SubtleBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Time: ${String.format("%02d:%02d", startHour, startMinute)} to ${String.format("%02d:%02d", endHour, endMinute)}",
                    fontSize = 14.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Strict Mode", fontSize = 14.sp, color = TextPrimary)
                    Switch(
                        checked = isStrict,
                        onCheckedChange = { isStrict = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = CyanAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AegisPrimaryButton(
                        text = "Save Routine",
                        onClick = {
                            onSave(
                                Schedule(
                                    name = name.ifBlank { "Focus Routine" },
                                    startHour = startHour,
                                    startMinute = startMinute,
                                    endHour = endHour,
                                    endMinute = endMinute,
                                    daysOfWeekBitmask = daysBitmask,
                                    isStrictMode = isStrict,
                                    isEnabled = true
                                )
                            )
                        },
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SuperAdminKeyGeneratorCard() {
    var generatedKey by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf("LIFE") }
    val clipboardManager = LocalClipboardManager.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, CyanAccent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔑", fontSize = 18.sp)
                Text(
                    text = "Super Admin • License Generator",
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Generate cryptographic Pro license keys for bKash / Nagad / Direct customers:",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("LIFE" to "Lifetime Pro", "YEAR" to "1-Year Pro", "MNTH" to "1-Month").forEach { (tier, label) ->
                    val isSel = selectedTier == tier
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTier = tier },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) CyanAccent.copy(alpha = 0.2f) else DeepSurface,
                        border = BorderStroke(1.dp, if (isSel) CyanAccent else SubtleBorder)
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) CyanAccent else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AegisPrimaryButton(
                text = "Generate License Key",
                onClick = {
                    generatedKey = LicenseKeyValidator.generateKey(selectedTier)
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (generatedKey.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElevatedSurface,
                    border = BorderStroke(1.dp, EmeraldFocus),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(generatedKey))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("KEY (TAP TO COPY):", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(generatedKey, fontSize = 16.sp, color = EmeraldFocus, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                        Text("Copy 📋", fontSize = 12.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
