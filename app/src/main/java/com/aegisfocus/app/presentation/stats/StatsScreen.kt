package com.aegisfocus.app.presentation.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

        // Header
        Text(
            text = "Discipline Analytics",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Local, private behavioral tracking.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = CyanAccent)
            }
        } else {
            // Metrics Overview Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Focus Time",
                    value = TimeUtils.formatMillisHuman(uiState.todaySummary.totalFocusTimeMs),
                    subtitle = "Today",
                    icon = Icons.Default.HourglassTop,
                    accentColor = EmeraldFocus,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Screen Time",
                    value = TimeUtils.formatMillisHuman(uiState.todaySummary.totalScreenTimeMs),
                    subtitle = "Today",
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
                    title = "Sessions",
                    value = "${uiState.todaySummary.sessionsCompleted}",
                    subtitle = "Completed today",
                    icon = Icons.Default.CheckCircle,
                    accentColor = EmeraldFocus,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Streak",
                    value = "${uiState.todaySummary.streakDays} days",
                    subtitle = "Current run",
                    icon = Icons.Default.Bolt,
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Weekly Discipline Breakdown Bar Chart
            SectionHeader(title = "7-Day Discipline Pattern")

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
                            .height(120.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxTime = uiState.weeklyHistory.maxOfOrNull { it.totalFocusTimeMs.coerceAtLeast(it.totalScreenTimeMs) }?.coerceAtLeast(1L) ?: 1L

                        uiState.weeklyHistory.forEach { day ->
                            val dayLabel = try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val parsed = sdf.parse(day.date)
                                val dayName = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(parsed ?: java.util.Date())
                                dayName.take(2)
                            } catch (e: Exception) {
                                "D"
                            }

                            val focusRatio = (day.totalFocusTimeMs.toFloat() / maxTime.toFloat()).coerceIn(0.05f, 1f)
                            val screenRatio = (day.totalScreenTimeMs.toFloat() / maxTime.toFloat()).coerceIn(0.05f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.height(90.dp)
                                ) {
                                    // Focus Bar
                                    Box(
                                        modifier = Modifier
                                            .width(8.dp)
                                            .fillMaxHeight(focusRatio)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(EmeraldFocus)
                                    )
                                    // Screen Bar
                                    Box(
                                        modifier = Modifier
                                            .width(8.dp)
                                            .fillMaxHeight(screenRatio)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyanAccent.copy(alpha = 0.35f))
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = dayLabel,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldFocus))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Focus Time", fontSize = 11.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyanAccent.copy(alpha = 0.5f)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Screen Time", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Top Distracting Applications
            SectionHeader(title = "Distraction Analysis")

            if (uiState.todaySummary.topDistractingApps.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = DeepSurface,
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        uiState.todaySummary.topDistractingApps.forEachIndexed { index, app ->
                            val maxUsage = uiState.todaySummary.topDistractingApps.first().usageTimeMs.coerceAtLeast(1L)
                            val fraction = (app.usageTimeMs.toFloat() / maxUsage.toFloat()).coerceIn(0f, 1f)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = app.appName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = TimeUtils.formatMillisHuman(app.usageTimeMs),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AlertStrict
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AlertStrict.copy(alpha = 0.8f),
                                    trackColor = ElevatedSurface
                                )

                                if (index < uiState.todaySummary.topDistractingApps.size - 1) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
