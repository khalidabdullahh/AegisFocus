package com.aegisfocus.app.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisCard
import com.aegisfocus.app.core.components.AegisSectionTitle
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import java.time.LocalDate

/** "Today" on Home: tick off today's habits right here, plus which protections are on. */
@Composable
fun HomeTodayCard(modifier: Modifier = Modifier) {
    val container = ServiceLocator.getContainer(LocalContext.current.applicationContext)
    val revision = remember { mutableIntStateOf(0) }
    val refresh = revision.intValue // reading it makes this card redraw after a tick

    val today = LocalDate.now()
    val habits = container.habitStore.getHabits().filter { it.days.contains(today.dayOfWeek.value) }
    val done = habits.count { container.habitStore.isDone(it.id, today) }
    val limits = container.appLimitStore.getLimits().count { it.enabled }
    val gates = container.mindfulGateStore.getGates().count { it.enabled }
    val intentOn = container.intentStore.isEnabled()

    AegisCard(modifier = modifier) {
        AegisSectionTitle("Today")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Habits", fontSize = 13.sp, color = TextSecondary)
            Text(
                if (habits.isEmpty()) "none set" else "$done of ${habits.size} done",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (habits.isEmpty()) 0f else done.toFloat() / habits.size },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = EmeraldFocus,
            trackColor = ElevatedSurface
        )

        habits.take(5).forEach { habit ->
            val isDone = container.habitStore.isDone(habit.id, today)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        container.habitStore.setDone(habit.id, today, !isDone)
                        revision.intValue = refresh + 1
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(if (isDone) EmeraldFocus else ElevatedSurface, CircleShape)
                        .border(1.5.dp, if (isDone) EmeraldFocus else SubtleBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) Icon(Icons.Default.Check, contentDescription = "Done", tint = ObsidianBg, modifier = Modifier.size(15.dp))
                }
                Text(
                    habit.title,
                    modifier = Modifier.padding(start = 10.dp),
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TodayChip("Limits", limits.toString())
            TodayChip("Gates", gates.toString())
            TodayChip("YouTube", if (intentOn) "ON" else "OFF")
        }
    }
}

@Composable
private fun TodayChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ElevatedSurface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
        }
    }
}
