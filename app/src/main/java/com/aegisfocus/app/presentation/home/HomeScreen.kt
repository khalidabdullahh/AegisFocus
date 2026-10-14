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
        Spacer(modifier = Modifier.height(24.dp))

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
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            Surface(
                shape = CircleShape,
                color = ElevatedSurface,
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Aegis",
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Discipline Score + XP Row ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Discipline Score card
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = ElevatedSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Discipline Score", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${uiState.disciplineScore}",
                        color = when {
                            uiState.disciplineScore >= 85 -> EmeraldFocus
                            uiState.disciplineScore >= 60 -> CyanAccent
                            else -> AlertStrict
                        },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("/100", color = TextMuted, fontSize = 12.sp)
                }
            }
            // XP + Level card
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = ElevatedSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Level", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${uiState.currentLevel.emoji} ${uiState.currentLevel.displayName}",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("${uiState.currentXp} XP  •  🔥 ${uiState.currentStreak}d streak", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Monk Mode Aegis Shield Evolution Card ───────────────────────────
        val shieldTier = com.aegisfocus.app.domain.motivation.ShieldTier.fromStreak(uiState.currentStreak)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(shieldTier.accentHex).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = androidx.compose.ui.graphics.Color(shieldTier.accentHex).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(shieldTier.accentHex).copy(alpha = 0.6f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (shieldTier) {
                                    com.aegisfocus.app.domain.motivation.ShieldTier.BRONZE -> "🥉"
                                    com.aegisfocus.app.domain.motivation.ShieldTier.SILVER -> "🥈"
                                    com.aegisfocus.app.domain.motivation.ShieldTier.GOLD -> "🥇"
                                    com.aegisfocus.app.domain.motivation.ShieldTier.OBSIDIAN -> "💎"
                                },
                                fontSize = 22.sp
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = shieldTier.title,
                                color = androidx.compose.ui.graphics.Color(shieldTier.accentHex),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EmeraldFocus.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "MONK SHIELD",
                                    color = EmeraldFocus,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = shieldTier.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "100%",
                        color = EmeraldFocus,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Durability",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Pro upgrade nudge (only for Free users)
        if (!uiState.isPro) {
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPaywall() },
                shape = RoundedCornerShape(12.dp),
                color = CyanAccent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⚡ Unlock Pro — Unlimited Strict Mode + Website Blocking", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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

        // Discipline Metrics Grid
        SectionHeader(title = "Discipline Snapshot")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Focus Time",
                value = TimeUtils.formatMillisHuman(uiState.usageSummary.totalFocusTimeMs),
                subtitle = "${uiState.usageSummary.sessionsCompleted} completed",
                icon = Icons.Default.HourglassTop,
                accentColor = EmeraldFocus,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Screen Time",
                value = TimeUtils.formatMillisHuman(uiState.usageSummary.totalScreenTimeMs),
                subtitle = "Total today",
                icon = Icons.Default.PhoneAndroid,
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Productivity Score",
                value = "${uiState.usageSummary.productivityScore}%",
                subtitle = if (uiState.usageSummary.productivityScore >= 75) "Optimal focus" else "Room for growth",
                icon = Icons.Default.SelfImprovement,
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Discipline Streak",
                value = "${uiState.usageSummary.streakDays} days",
                subtitle = "Consecutive active",
                icon = Icons.Default.Bolt,
                accentColor = EmeraldFocus,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
            modifier = Modifier.padding(14.dp),
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
