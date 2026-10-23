package com.aegisfocus.app.presentation.settings

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.aegisfocus.app.core.common.ContactLinks
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.aegisfocus.app.R
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
import com.aegisfocus.app.core.theme.AlertStrict
import com.aegisfocus.app.data.remote.SupabaseAuthService
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import com.aegisfocus.app.domain.model.Schedule
import com.aegisfocus.app.domain.subscription.SubscriptionPlan

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Remembers which switch asked for the VPN permission ("ad" or "adult")
    var pendingVpnFor by remember { mutableStateOf("ad") }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (pendingVpnFor == "adult") {
                viewModel.toggleAdultBlock(true, context)
                android.widget.Toast.makeText(context, "18+ content blocking is on", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                viewModel.toggleAdBlockerVpn(true, context)
                android.widget.Toast.makeText(context, "AdBlocker is on", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "VPN permission is required for the system-wide blocker", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

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
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = (-0.3).sp
        )
        Text(
            text = "Permissions, focus habits, and discipline rules.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Aegis Focus Pro Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToPaywall),
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, if (uiState.activePlan != SubscriptionPlan.FREE) EmeraldFocus.copy(alpha = 0.6f) else CyanAccent.copy(alpha = 0.5f))
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
                            .background(if (uiState.activePlan != SubscriptionPlan.FREE) EmeraldFocus.copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (uiState.activePlan != SubscriptionPlan.FREE) EmeraldFocus else CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        val isProActive = uiState.activePlan != SubscriptionPlan.FREE
                        Text(
                            text = if (isProActive) "${uiState.activePlan.displayName}" else "Aegis Focus Pro",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isProActive) {
                                if (uiState.activePlan == SubscriptionPlan.LIFETIME) "Permanent Lifetime Access • Active"
                                else "${uiState.daysRemaining} days remaining • Active"
                            } else {
                                "Unlimited Strict Mode, Website Blocking & Analytics"
                            },
                            color = if (isProActive) EmeraldFocus else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isProActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                Surface(
                    onClick = onNavigateToPaywall,
                    shape = RoundedCornerShape(20.dp),
                    color = if (uiState.activePlan != SubscriptionPlan.FREE) EmeraldFocus.copy(alpha = 0.15f) else CyanAccent,
                    border = BorderStroke(1.dp, if (uiState.activePlan != SubscriptionPlan.FREE) EmeraldFocus else CyanAccent)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (uiState.activePlan != SubscriptionPlan.FREE) "Manage" else "Upgrade",
                            color = if (uiState.activePlan != SubscriptionPlan.FREE) EmeraldFocus else ObsidianBg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Redeem License Button Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToPaywall),
            shape = RoundedCornerShape(14.dp),
            color = ElevatedSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Have a License Code? Redeem Here",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Redeem",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
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

                Spacer(modifier = Modifier.height(18.dp))

                // Surgical Shorts / Reels Blocker Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Block Shorts & Reels",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EmeraldFocus.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "SURGICAL",
                                    color = EmeraldFocus,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Auto-blocks YouTube Shorts & Instagram Reels without blocking the full app",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = uiState.isShortsBlockerEnabled,
                        onCheckedChange = { viewModel.toggleShortsBlocker(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = EmeraldFocus,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DeepSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Scroll Fatigue & Grayscale Detox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Scroll Fatigue Grayscale",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyanAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "DETOX",
                                    color = CyanAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Turn screen monochrome during excessive scrolling to kill dopamine attraction",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = uiState.isGrayscaleFatigueEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.toggleGrayscaleFatigue(isChecked)
                            if (isChecked) {
                                try {
                                    val intent = android.content.Intent("android.settings.COLOR_CORRECTION_SETTINGS")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        android.widget.Toast.makeText(context, "Open Accessibility Settings to enable Grayscale", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
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

        Spacer(modifier = Modifier.height(16.dp))

        // 1-Tap Automatic AdBlocker Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, if (uiState.isAdBlockerVpnEnabled) EmeraldFocus.copy(alpha = 0.4f) else SubtleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (uiState.isAdBlockerVpnEnabled) EmeraldFocus.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (uiState.isAdBlockerVpnEnabled) EmeraldFocus else CyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "System-Wide AdBlocker",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (uiState.isAdBlockerVpnEnabled) EmeraldFocus.copy(alpha = 0.2f) else DeepSurface
                                ) {
                                    Text(
                                        text = if (uiState.isAdBlockerVpnEnabled) "ACTIVE" else "1-TAP DNS",
                                        color = if (uiState.isAdBlockerVpnEnabled) EmeraldFocus else TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Blocks most in-app ads, popups and trackers system-wide with one tap.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = uiState.isAdBlockerVpnEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                val vpnIntent = android.net.VpnService.prepare(context)
                                if (vpnIntent != null) {
                                    pendingVpnFor = "ad"
                                    vpnLauncher.launch(vpnIntent)
                                } else {
                                    viewModel.toggleAdBlockerVpn(true, context)
                                    android.widget.Toast.makeText(context, "AdBlocker is on", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.toggleAdBlockerVpn(false, context)
                                android.widget.Toast.makeText(context, "AdBlocker Deactivated", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = EmeraldFocus,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DeepSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Block 18+ content",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Blocks known adult sites, video ads and trackers using a family-safe DNS filter.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = uiState.isAdultBlockEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                val vpnIntent = android.net.VpnService.prepare(context)
                                if (vpnIntent != null) {
                                    pendingVpnFor = "adult"
                                    vpnLauncher.launch(vpnIntent)
                                } else {
                                    viewModel.toggleAdultBlock(true, context)
                                    android.widget.Toast.makeText(context, "18+ content blocking is on", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.toggleAdultBlock(false, context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = EmeraldFocus,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DeepSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElevatedSurface,
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (uiState.isAdultBlockEnabled) "Protected via AdGuard Family DNS" else "Protected via AdGuard Secure DNS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uiState.isAdBlockerVpnEnabled) EmeraldFocus else TextPrimary
                                )
                                Text(
                                    text = if (uiState.isAdBlockerVpnEnabled) "Status: Filtering active in real-time" else "Flip switch above for instant 1-tap activation",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }

                            Surface(
                                modifier = Modifier.clickable {
                                    // Copy DNS hostname to clipboard & open Private DNS settings as fallback
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Private DNS", if (uiState.isAdultBlockEnabled) "family.adguard-dns.com" else "dns.adguard-dns.com")
                                    clipboard?.setPrimaryClip(clip)

                                    android.widget.Toast.makeText(context, "DNS hostname copied. Opening network settings...", android.widget.Toast.LENGTH_SHORT).show()

                                    try {
                                        val intent = android.content.Intent("android.settings.NETWORK_OPERATOR_SETTINGS")
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e2: Exception) {
                                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = DeepSurface,
                                border = BorderStroke(1.dp, SubtleBorder)
                            ) {
                                Text(
                                    text = "Manual Private DNS ↗",
                                    color = CyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
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

        // About & Version Info + Website / Download Link
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onVersionTapped(context) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aegis Focus • v1.4.0",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.about_subtitle),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { ContactLinks.openDownloadPage(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CyanAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download APK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = { ContactLinks.openWebsite(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SubtleBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Website",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (uiState.isAdminMode) {
            Spacer(modifier = Modifier.height(16.dp))
            val adminState by viewModel.adminKeys.collectAsState()
            SuperAdminKeyGeneratorCard(
                state = adminState,
                onGenerate = { tier, note -> viewModel.generateKey(tier, note) },
                onRefresh = { viewModel.refreshKeys() },
                onRevoke = { viewModel.revokeKey(it) }
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    if (uiState.isAddScheduleDialogOpen) {
        AddScheduleDialog(
            onDismiss = { viewModel.closeAddScheduleDialog() },
            onSave = { schedule -> viewModel.addSchedule(schedule) }
        )
    }

    if (uiState.isPasswordPromptOpen) {
        AdminPasswordPromptDialog(
            errorMessage = uiState.adminPasswordError,
            onDismiss = { viewModel.closeAdminPasswordPrompt() },
            onVerify = { pin -> viewModel.verifyAdminPassword(pin, context) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    var name by remember { mutableStateOf("Daily Deep Work") }
    var startHour by remember { mutableIntStateOf(9) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(11) }
    var endMinute by remember { mutableIntStateOf(0) }
    var isStrict by remember { mutableStateOf(true) }
    var daysBitmask by remember { mutableIntStateOf(62) } // Mon - Fri

    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Create Recurring Routine",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Runs automatically in background on scheduled days.",
                    fontSize = 12.sp,
                    color = TextSecondary
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

                Spacer(modifier = Modifier.height(16.dp))

                // Start Time Stepper
                Text(text = "Start Time", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElevatedSurface,
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = com.aegisfocus.app.core.common.TimeUtils.formatTimeOfDay(startHour, startMinute),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (startHour > 0) startHour-- else startHour = 23
                            }) {
                                Icon(Icons.Default.Remove, contentDescription = "-1h", tint = CyanAccent, modifier = Modifier.size(16.dp))
                            }
                            Text("${String.format("%02d", startHour)}h", fontSize = 13.sp, color = TextPrimary)
                            IconButton(onClick = {
                                if (startHour < 23) startHour++ else startHour = 0
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "+1h", tint = CyanAccent, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = {
                                startMinute = (startMinute + 15) % 60
                            }) {
                                Text("+15m", fontSize = 11.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // End Time Stepper
                Text(text = "End Time", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElevatedSurface,
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = com.aegisfocus.app.core.common.TimeUtils.formatTimeOfDay(endHour, endMinute),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (endHour > 0) endHour-- else endHour = 23
                            }) {
                                Icon(Icons.Default.Remove, contentDescription = "-1h", tint = CyanAccent, modifier = Modifier.size(16.dp))
                            }
                            Text("${String.format("%02d", endHour)}h", fontSize = 13.sp, color = TextPrimary)
                            IconButton(onClick = {
                                if (endHour < 23) endHour++ else endHour = 0
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "+1h", tint = CyanAccent, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = {
                                endMinute = (endMinute + 15) % 60
                            }) {
                                Text("+15m", fontSize = 11.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Days Selector
                Text(text = "Repeating Days", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                // Quick preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (daysBitmask == 127) CyanAccent.copy(alpha = 0.2f) else ElevatedSurface,
                        border = BorderStroke(1.dp, if (daysBitmask == 127) CyanAccent else SubtleBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { daysBitmask = 127 }
                    ) {
                        Text("Everyday", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp), color = TextPrimary)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (daysBitmask == 62) CyanAccent.copy(alpha = 0.2f) else ElevatedSurface,
                        border = BorderStroke(1.dp, if (daysBitmask == 62) CyanAccent else SubtleBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { daysBitmask = 62 }
                    ) {
                        Text("Mon–Fri", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp), color = TextPrimary)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (daysBitmask == 65) CyanAccent.copy(alpha = 0.2f) else ElevatedSurface,
                        border = BorderStroke(1.dp, if (daysBitmask == 65) CyanAccent else SubtleBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { daysBitmask = 65 }
                    ) {
                        Text("Weekends", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp), color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Individual day toggle chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0..6) {
                        val isSelected = (daysBitmask and (1 shl i)) != 0
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) CyanAccent else ElevatedSurface,
                            border = BorderStroke(1.dp, if (isSelected) CyanAccent else SubtleBorder),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    daysBitmask = daysBitmask xor (1 shl i)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayNames[i].take(1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ObsidianBg else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Strict Mode Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElevatedSurface,
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Strict Mode Protection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Automatically locks distracting apps and blocks cancellation during routine.", fontSize = 11.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isStrict,
                            onCheckedChange = { isStrict = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = CyanAccent
                            )
                        )
                    }
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
                                    daysOfWeekBitmask = if (daysBitmask == 0) 127 else daysBitmask,
                                    isStrictMode = isStrict,
                                    isEnabled = true
                                )
                            )
                        },
                        modifier = Modifier.width(150.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SuperAdminKeyGeneratorCard(
    state: AdminKeyState,
    onGenerate: (String, String?) -> Unit,
    onRefresh: () -> Unit,
    onRevoke: (String) -> Unit
) {
    var selectedTier by remember { mutableStateOf("YEAR") }
    var note by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) { onRefresh() }

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
                Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                Text(
                    text = "Admin - One-time License Keys",
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Keys are created on the server and work exactly once. You must be signed in as super_admin.",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            val tiers = listOf("MNTH" to "1 Mo", "3M" to "3 Mo", "6M" to "6 Mo", "YEAR" to "1 Yr", "STUD" to "Student", "LIFE" to "Life")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                tiers.forEach { (tier, label) ->
                    val isSel = selectedTier == tier
                    Surface(
                        modifier = Modifier.weight(1f).clickable { selectedTier = tier },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) CyanAccent.copy(alpha = 0.2f) else DeepSurface,
                        border = BorderStroke(1.dp, if (isSel) CyanAccent else SubtleBorder)
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) CyanAccent else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buyer note (optional)", color = TextMuted, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = SubtleBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanAccent
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            AegisPrimaryButton(
                text = if (state.isBusy) "Working..." else "Generate Key",
                onClick = { onGenerate(selectedTier, note) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            )

            state.message?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = AlertStrict, fontSize = 12.sp)
            }

            state.lastKey?.let { key ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElevatedSurface,
                    border = BorderStroke(1.dp, EmeraldFocus),
                    modifier = Modifier.fillMaxWidth().clickable { clipboardManager.setText(AnnotatedString(key)) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("NEW KEY (TAP TO COPY)", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(key, fontSize = 14.sp, color = EmeraldFocus, fontWeight = FontWeight.Black)
                        }
                        Text("Copy", fontSize = 12.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (state.keys.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("Recent keys", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                state.keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                row.keyCode,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(row.keyCode)) }
                            )
                            val who = row.redeemedByEmail?.let { " by $it" } ?: ""
                            Text("${row.tier} - ${row.status}$who", color = TextMuted, fontSize = 10.sp)
                        }
                        if (row.status.equals("unused", true) || row.status.equals("active", true)) {
                            TextButton(onClick = { onRevoke(row.keyCode) }) {
                                Text("Revoke", color = AlertStrict, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPasswordPromptDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CyanAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Super Admin Security",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter the 10-digit master password to unlock the offline license generator.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Master Password", color = TextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$errorMessage",
                        color = AlertStrict,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    AegisPrimaryButton(
                        text = "Unlock",
                        onClick = { onVerify(password) },
                        enabled = password.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

