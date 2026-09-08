package com.aegisfocus.app.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.core.theme.AlertStrict
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.domain.model.Schedule

@Composable
fun ScheduleCard(
    schedule: Schedule,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val startTimeFormatted = TimeUtils.formatTimeOfDay(schedule.startHour, schedule.startMinute)
    val endTimeFormatted = TimeUtils.formatTimeOfDay(schedule.endHour, schedule.endMinute)
    val daysFormatted = TimeUtils.formatDaysOfWeek(schedule.daysOfWeekBitmask)

    Surface(
        modifier = modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = schedule.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (schedule.isStrictMode) {
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Strict Schedule",
                            tint = AlertStrict,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$startTimeFormatted – $endTimeFormatted",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanAccent
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$daysFormatted • ${schedule.durationMinutes} min",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = onToggle,
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
