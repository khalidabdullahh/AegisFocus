package com.aegisfocus.app.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.components.SectionHeader
import com.aegisfocus.app.core.components.StatCard
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
import com.aegisfocus.app.domain.model.SessionMode

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToFocus: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val remainingMillis by viewModel.remainingMillis.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Greeting Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = uiState.displayDate.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uiState.greeting,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.3).sp
                )
            }

        }

        Spacer(modifier = Modifier.height(20.dp))

        // Discipline score and level as two small chips
        val hasActivity = uiState.currentXp > 0 ||
            uiState.currentStreak > 0 ||
            uiState.usageSummary.sessionsCompleted > 0
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip(
                label = "Discipline",
                value = if (hasActivity) "${uiState.disciplineScore}/100" else "--",
                valueColor = when {
                    !hasActivity -> TextMuted
                    uiState.disciplineScore >= 85 -> EmeraldFocus
                    uiState.disciplineScore >= 60 -> CyanAccent
                    else -> AlertStrict
                },
                modifier = Modifier.weight(1f)
            )
            InfoChip(
                label = "Level",
                value = uiState.currentLevel.displayName,
                valueColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Pro upgrade nudge (only for Free users)
        if (!uiState.isPro) {
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPaywall() },
                shape = RoundedCornerShape(10.dp),
                color = CyanAccent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.25f))
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                    Text(
                        "Unlock Pro: unlimited Strict Mode and website blocking",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Monthly goals and habit calendar shortcut
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToGoals() },
            shape = RoundedCornerShape(12.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldFocus.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = EmeraldFocus,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly goals",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Habit calendar and daily target",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open goals",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Active Session or Primary CTA
        if (activeSession != null) {
            val isStrict = activeSession?.mode == SessionMode.STRICT
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToFocus),
                shape = RoundedCornerShape(20.dp),
                color = ElevatedSurface,
                border = BorderStroke(1.5.dp, if (isStrict) AlertStrict else CyanAccent)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = (if (isStrict) AlertStrict else CyanAccent).copy(alpha = 0.2f),
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FOCUS SESSION ACTIVE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = if (isStrict) AlertStrict else CyanAccent
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DeepSurface,
                            border = BorderStroke(1.dp, SubtleBorder)
                        ) {
                            Text(
                                text = if (isStrict) "STRICT" else "NORMAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = TimeUtils.formatRemainingMillis(remainingMillis),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-1.0).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${activeSession?.name} • ${activeSession?.blockedPackageNames?.size ?: 0} apps blocked",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View Active Session",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanAccent
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            AegisPrimaryButton(
                text = "START FOCUS",
                onClick = onNavigateToFocus
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Discipline snapshot: only real values, otherwise an honest empty state
        SectionHeader(title = "Discipline Snapshot")

        val summary = uiState.usageSummary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Focus Time",
                value = if (summary.sessionsCompleted > 0) TimeUtils.formatMillisHuman(summary.totalFocusTimeMs) else "No data yet",
                subtitle = if (summary.sessionsCompleted > 0) "${summary.sessionsCompleted} completed today" else "Finish a session to start",
                icon = Icons.Default.HourglassTop,
                accentColor = EmeraldFocus,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Screen Time",
                value = if (summary.hasScreenTimeData) TimeUtils.formatMillisHuman(summary.totalScreenTimeMs) else "No data yet",
                subtitle = if (summary.hasScreenTimeData) "Total today" else "Needs Usage Access",
                icon = Icons.Default.PhoneAndroid,
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Focus Share",
                value = summary.productivityScore?.let { "$it%" } ?: "No data yet",
                subtitle = "Focus vs screen time",
                icon = Icons.Default.SelfImprovement,
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Focus Streak",
                value = if (summary.streakDays > 0) "${summary.streakDays} days" else "No streak yet",
                subtitle = "Consecutive days",
                icon = Icons.Default.Bolt,
                accentColor = EmeraldFocus,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Launch Presets
        if (activeSession == null) {
            SectionHeader(title = "Quick Focus Routines")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickPresetButton(
                    minutes = 25,
                    label = "Pomodoro",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.startQuickFocus(25, isStrict = false) }
                )
                QuickPresetButton(
                    minutes = 50,
                    label = "Deep Work",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.startQuickFocus(50, isStrict = true) }
                )
                QuickPresetButton(
                    minutes = 90,
                    label = "Immersion",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.startQuickFocus(90, isStrict = true) }
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = ElevatedSurface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun QuickPresetButton(
    minutes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${minutes}m",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
