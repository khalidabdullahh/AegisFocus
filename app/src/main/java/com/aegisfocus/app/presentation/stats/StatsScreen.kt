package com.aegisfocus.app.presentation.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.common.TimeUtils
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

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Discipline Analytics",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Turn Time Into Progress",
                    fontSize = 12.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Streak Badge
            Surface(
                color = CyanAccent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${uiState.currentStreak} Day Streak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Horizon Switcher Segment Control
        HorizonSegmentSelector(
            selected = uiState.selectedHorizon,
            onSelect = { viewModel.selectHorizon(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = CyanAccent)
            }
        } else {
            // ─── 1. DISCIPLINE SCORE HERO CARD ────────────────────────────────
            DisciplineScoreHeroCard(score = uiState.disciplineScore)

            Spacer(modifier = Modifier.height(20.dp))

            // ─── 2. PRIMARY 4-STAT GRID ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Focus Time",
                    value = TimeUtils.formatMillisHuman(uiState.todaySummary.totalFocusTimeMs),
                    subtitle = "Quality Attention",
                    icon = Icons.Default.HourglassTop,
                    accentColor = EmeraldFocus,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Screen Time",
                    value = TimeUtils.formatMillisHuman(uiState.todaySummary.totalScreenTimeMs),
                    subtitle = "Device Usage",
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
                    title = "Saved Time",
                    value = "${uiState.savedDistractionMinutes}m",
                    subtitle = "Distraction Blocked",
                    icon = Icons.Default.Shield,
                    accentColor = EmeraldFocus,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Sessions",
                    value = "${uiState.totalFocusSessions}",
                    subtitle = "Completed",
                    icon = Icons.Default.CheckCircle,
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── 3. 7-DAY FOCUS VS DISTRACTION TREND GRAPH ────────────────────
            SectionHeader(title = "7-Day Focus & Distraction Trend")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val maxTime = uiState.weeklyHistory.maxOfOrNull {
                        it.totalFocusTimeMs.coerceAtLeast(it.totalScreenTimeMs)
                    }?.coerceAtLeast(1L) ?: 1L

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        uiState.weeklyHistory.forEach { day ->
                            val dayLabel = try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val parsed = sdf.parse(day.date)
                                java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(parsed ?: java.util.Date()).take(2)
                            } catch (e: Exception) {
                                "D"
                            }

                            val focusRatio = (day.totalFocusTimeMs.toFloat() / maxTime.toFloat()).coerceIn(0.08f, 1f)
                            val screenRatio = (day.totalScreenTimeMs.toFloat() / maxTime.toFloat()).coerceIn(0.08f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.height(105.dp)
                                ) {
                                    // Focus Bar (Emerald gradient)
                                    Box(
                                        modifier = Modifier
                                            .width(9.dp)
                                            .fillMaxHeight(focusRatio)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(EmeraldFocus, Color(0xFF059669))
                                                )
                                            )
                                    )
                                    // Screen Bar (Cyan faint)
                                    Box(
                                        modifier = Modifier
                                            .width(9.dp)
                                            .fillMaxHeight(screenRatio)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(CyanAccent.copy(alpha = 0.6f), CyanAccent.copy(alpha = 0.2f))
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = dayLabel,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(EmeraldFocus))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Focus Time", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CyanAccent.copy(alpha = 0.6f)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Screen Time", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── 4. HOURLY FOCUS TIMELINE HEATMAP ──────────────────────────────
            SectionHeader(title = "Hourly Focus Distribution (6 AM – 10 PM)")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Peak focus intensity occurs between 09:00 and 16:00.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        uiState.hourlyFocusDistribution.forEach { point ->
                            val heightRatio = (point.focusMinutes.toFloat() / 60f).coerceIn(0.1f, 1f)
                            val isPeak = point.focusMinutes >= 30

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(heightRatio)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isPeak) EmeraldFocus else CyanAccent.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("6 AM", fontSize = 10.sp, color = TextMuted)
                        Text("12 PM", fontSize = 10.sp, color = TextMuted)
                        Text("5 PM", fontSize = 10.sp, color = TextMuted)
                        Text("10 PM", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── 5. DISTRACTION APPS ANALYSIS ─────────────────────────────────
            SectionHeader(title = "Top Distracting Applications")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (uiState.todaySummary.topDistractingApps.isEmpty()) {
                        Text("No distracting apps recorded today.", fontSize = 13.sp, color = TextMuted)
                    } else {
                        val maxUsage = uiState.todaySummary.topDistractingApps.first().usageTimeMs.coerceAtLeast(1L)

                        uiState.todaySummary.topDistractingApps.forEachIndexed { index, app ->
                            val fraction = (app.usageTimeMs.toFloat() / maxUsage.toFloat()).coerceIn(0.05f, 1f)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ElevatedSurface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = app.appName.take(1).uppercase(),
                                                color = AlertStrict,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = app.appName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }

                                    Text(
                                        text = TimeUtils.formatMillisHuman(app.usageTimeMs),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AlertStrict
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AlertStrict,
                                    trackColor = ElevatedSurface
                                )

                                if (index < uiState.todaySummary.topDistractingApps.size - 1) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── 6. LOCAL PRIVACY GUARANTEE ───────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ElevatedSurface.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldFocus, modifier = Modifier.size(20.dp))
                    Text(
                        text = "100% On-Device SQLite. Your analytics never leave your device.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─── COMPONENT: HORIZON SELECTOR ─────────────────────────────────────────────
@Composable
private fun HorizonSegmentSelector(
    selected: AnalyticsHorizon,
    onSelect: (AnalyticsHorizon) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            AnalyticsHorizon.values().forEach { horizon ->
                val isSelected = horizon == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanAccent.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onSelect(horizon) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = horizon.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CyanAccent else TextSecondary
                    )
                }
            }
        }
    }
}

// ─── COMPONENT: DISCIPLINE SCORE HERO CARD ───────────────────────────────────
@Composable
private fun DisciplineScoreHeroCard(
    score: com.aegisfocus.app.domain.analytics.DisciplineScoreCalculator.ScoreBreakdown?
) {
    val totalScore = score?.total ?: 85
    val animatedProgress by animateFloatAsState(
        targetValue = totalScore / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "scoreArc"
    )

    val scoreColor = when {
        totalScore >= 80 -> EmeraldFocus
        totalScore >= 60 -> CyanAccent
        else -> AlertStrict
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Discipline Score",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Transparent Behavioral Index",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                // Score Ring
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = ElevatedSurface,
                        strokeWidth = 6.dp,
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = scoreColor,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "$totalScore",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = scoreColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Factors Breakdown
            score?.let { breakdown ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSurface.copy(alpha = 0.6f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ScoreFactorItem(label = "Base", value = "+${breakdown.base}", isPositive = true)
                    ScoreFactorItem(label = "Streak", value = "+${breakdown.streakBonus}", isPositive = true)
                    ScoreFactorItem(label = "Goal", value = "+${breakdown.goalBonus}", isPositive = true)
                    ScoreFactorItem(label = "Distractions", value = "-${breakdown.distractionPenalty}", isPositive = false)
                }
            }
        }
    }
}

@Composable
private fun ScoreFactorItem(
    label: String,
    value: String,
    isPositive: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPositive) EmeraldFocus else AlertStrict
        )
    }
}
