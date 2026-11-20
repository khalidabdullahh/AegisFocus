package com.aegisfocus.app.presentation.productivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.common.DurationInput
import com.aegisfocus.app.core.components.AegisCard
import com.aegisfocus.app.core.components.AegisSectionTitle
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.data.local.StatsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private data class WeeklyReport(
    val days: List<Pair<String, Int>>,
    val totalMinutes: Int,
    val sessions: Int,
    val bestDay: String?,
    val habitPercent: Int?,
    val stats: Map<StatsStore.Event, Int>
)

/** "Your week": focus minutes per day, habits done and how often Aegis stepped in. */
@Composable
fun WeeklyReportCard() {
    val container = ServiceLocator.getContainer(LocalContext.current.applicationContext)
    val report by produceState<WeeklyReport?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val sessions = container.focusSessionDao.getAllSessionsFlow().first()
                .filter { it.status == "COMPLETED" }
                .map { Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() to it.durationMinutes }
            val last7 = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val days = last7.map { d ->
                d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to
                    sessions.filter { it.first == d }.sumOf { it.second }
            }
            val weekSessions = sessions.count { it.first in last7 }
            val best = last7.maxByOrNull { d -> sessions.filter { it.first == d }.sumOf { it.second } }
                ?.takeIf { d -> sessions.any { it.first == d } }
                ?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.getDefault())

            var scheduled = 0
            var done = 0
            container.habitStore.getHabits().forEach { h ->
                val created = LocalDate.parse(h.createdDate)
                last7.forEach { d ->
                    if (!d.isBefore(created) && h.days.contains(d.dayOfWeek.value)) {
                        scheduled++
                        if (container.habitStore.isDone(h.id, d)) done++
                    }
                }
            }
            WeeklyReport(
                days = days,
                totalMinutes = days.sumOf { it.second },
                sessions = weekSessions,
                bestDay = best,
                habitPercent = if (scheduled == 0) null else done * 100 / scheduled,
                stats = container.statsStore.weekTotals()
            )
        }
    }

    AegisCard {
        AegisSectionTitle("Your week")
        Spacer(Modifier.height(10.dp))
        val data = report
        if (data == null) {
            Text("Loading...", fontSize = 12.sp, color = TextMuted)
            return@AegisCard
        }

        val maxMinutes = (data.days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(30)
        Row(
            modifier = Modifier.fillMaxWidth().height(86.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.days.forEach { (label, minutes) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height((58f * minutes / maxMinutes).coerceAtLeast(4f).dp)
                            .background(if (minutes > 0) EmeraldFocus else ElevatedSurface, RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        ReportLine("Focus time", DurationInput.format(data.totalMinutes) + " in ${data.sessions} sessions")
        ReportLine("Best day", data.bestDay ?: "none yet")
        ReportLine("Habits done", data.habitPercent?.let { "$it%" } ?: "no habits set")
        val shorts = data.stats[StatsStore.Event.SHORTS_BLOCKED] ?: 0
        val intent = data.stats[StatsStore.Event.INTENT_BLOCKED] ?: 0
        val limits = data.stats[StatsStore.Event.LIMIT_BLOCKED] ?: 0
        val shown = data.stats[StatsStore.Event.GATE_SHOWN] ?: 0
        val opened = data.stats[StatsStore.Event.GATE_OPENED] ?: 0
        ReportLine("Stopped", "$shorts Shorts/Reels, $intent YouTube videos, $limits limit hits")
        ReportLine("Mindful Gate", if (shown == 0) "not used" else "${shown - opened.coerceAtMost(shown)} of $shown times you chose not to open")
    }
}

@Composable
private fun ReportLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
