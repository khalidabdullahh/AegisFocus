package com.aegisfocus.app.presentation.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.theme.AmberWarning
import com.aegisfocus.app.core.theme.CrimsonDanger
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val monthYearTitle = uiState.currentYearMonth.format(
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    )

    val selectedDayModel = uiState.days.find { it.date == uiState.selectedDate }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // ─── Header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(38.dp)
                    .background(DeepSurface, RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Goals & Habit Calendar",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Track your daily focus wins & consistency",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Metric Overview Cards ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Consistency",
                value = "${uiState.consistencyPercentage}%",
                subtitle = "Monthly rate",
                color = CyanAccent,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Days Won",
                value = "${uiState.daysWon}",
                subtitle = "Goal met 🏆",
                color = EmeraldFocus,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Missed",
                value = "${uiState.daysMissed}",
                subtitle = "Needs push",
                color = CrimsonDanger,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ─── Calendar Surface ─────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Month Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousMonth() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = monthYearTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(
                        onClick = { viewModel.nextMonth() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Day of Week Labels (Mon - Sun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
                    daysOfWeek.forEach { dayName ->
                        Text(
                            text = dayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 7-Column Grid Layout for the Calendar
                val totalSlots = uiState.firstDayOffset + uiState.days.size
                val rows = (totalSlots + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val slotIndex = row * 7 + col
                                val dayIndex = slotIndex - uiState.firstDayOffset

                                if (dayIndex in uiState.days.indices) {
                                    val dayModel = uiState.days[dayIndex]
                                    CalendarDayTile(
                                        dayModel = dayModel,
                                        onClick = { viewModel.selectDate(dayModel.date) },
                                        modifier = Modifier.size(38.dp)
                                    )
                                } else {
                                    // Empty padding space
                                    Box(modifier = Modifier.size(38.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = EmeraldFocus, label = "Goal Met")
                    LegendItem(color = CrimsonDanger, label = "Missed")
                    LegendItem(color = AmberWarning, label = "In Progress")
                    LegendItem(color = TextMuted, label = "Upcoming")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Selected Day Detail Card ─────────────────────────────────────────
        if (selectedDayModel != null) {
            val formattedDate = selectedDayModel.date.format(
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = formattedDate,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (selectedDayModel.isToday) "Today's Target Status" else "Daily Focus Record",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        StatusPill(status = selectedDayModel.status)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    val progress = (selectedDayModel.achievedMinutes.toFloat() / selectedDayModel.targetMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when (selectedDayModel.status) {
                            DayGoalStatus.SUCCESS_MET -> EmeraldFocus
                            DayGoalStatus.MISSED -> CrimsonDanger
                            DayGoalStatus.TODAY_IN_PROGRESS -> AmberWarning
                            DayGoalStatus.FUTURE -> TextMuted
                        },
                        trackColor = ElevatedSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Focus: ${selectedDayModel.achievedMinutes}m of ${selectedDayModel.targetMinutes}m target",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${selectedDayModel.sessionsCompleted} session(s)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Daily Target Goal Selector ───────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Daily Focus Target",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Set your daily target to automatically color calendar days green or red.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 60, 90, 120, 180, 240).forEach { mins ->
                        val isSelected = uiState.targetMinutes == mins
                        val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setTargetMinutes(mins) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyanAccent else ElevatedSurface,
                            border = BorderStroke(1.dp, if (isSelected) CyanAccent else SubtleBorder)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ObsidianBg else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun CalendarDayTile(
    dayModel: GoalDayUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, textColor) = when (dayModel.status) {
        DayGoalStatus.SUCCESS_MET -> Triple(
            EmeraldFocus.copy(alpha = 0.22f),
            EmeraldFocus,
            EmeraldFocus
        )
        DayGoalStatus.MISSED -> Triple(
            CrimsonDanger.copy(alpha = 0.16f),
            CrimsonDanger.copy(alpha = 0.6f),
            CrimsonDanger
        )
        DayGoalStatus.TODAY_IN_PROGRESS -> Triple(
            AmberWarning.copy(alpha = 0.16f),
            AmberWarning,
            AmberWarning
        )
        DayGoalStatus.FUTURE -> Triple(
            ElevatedSurface.copy(alpha = 0.3f),
            Color.Transparent,
            TextMuted.copy(alpha = 0.6f)
        )
    }

    val selectionBorder = if (dayModel.isSelected) {
        BorderStroke(2.dp, CyanAccent)
    } else {
        BorderStroke(1.dp, borderColor)
    }

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = selectionBorder
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dayModel.dayNumber.toString(),
                    fontSize = 12.sp,
                    fontWeight = if (dayModel.isToday || dayModel.status == DayGoalStatus.SUCCESS_MET) FontWeight.Bold else FontWeight.Normal,
                    color = if (dayModel.isSelected) CyanAccent else textColor
                )
                if (dayModel.status == DayGoalStatus.SUCCESS_MET) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(EmeraldFocus, CircleShape)
                    )
                } else if (dayModel.status == DayGoalStatus.MISSED) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(CrimsonDanger, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: DayGoalStatus) {
    val (label, bgColor, textColor) = when (status) {
        DayGoalStatus.SUCCESS_MET -> Triple("🟢 GOAL MET", EmeraldFocus.copy(alpha = 0.2f), EmeraldFocus)
        DayGoalStatus.MISSED -> Triple("🔴 MISSED", CrimsonDanger.copy(alpha = 0.2f), CrimsonDanger)
        DayGoalStatus.TODAY_IN_PROGRESS -> Triple("🟡 IN PROGRESS", AmberWarning.copy(alpha = 0.2f), AmberWarning)
        DayGoalStatus.FUTURE -> Triple("⚪ UPCOMING", ElevatedSurface, TextMuted)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = TextMuted
            )
        }
    }
}
