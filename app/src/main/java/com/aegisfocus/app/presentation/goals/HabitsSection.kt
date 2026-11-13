package com.aegisfocus.app.presentation.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.aegisfocus.app.data.local.Habit

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

/** "My Habits": the user's own daily goals, ticked off each day. */
@Composable
fun HabitsSection(
    habits: List<HabitUi>,
    onToggle: (Habit) -> Unit,
    onAdd: (String, Set<Int>) -> Unit,
    onDelete: (Habit) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Habit?>(null) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text("My Habits", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Add your own daily goals and tick them off", fontSize = 11.sp, color = TextSecondary)
                }
                Surface(
                    modifier = Modifier.clickable { showAdd = true },
                    shape = RoundedCornerShape(10.dp),
                    color = CyanAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CyanAccent)
                ) {
                    Text(
                        "+ Add",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanAccent
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (habits.isEmpty()) {
                Text(
                    "No habits yet. Try \"Read 20 pages\", \"Walk 30 min\" or \"No phone after 11 PM\".",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    habits.forEach { item ->
                        HabitRow(item, onToggle = { onToggle(item.habit) }, onDelete = { deleteTarget = item.habit })
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddHabitDialog(
            onDismiss = { showAdd = false },
            onSave = { title, days ->
                onAdd(title, days)
                showAdd = false
            }
        )
    }

    deleteTarget?.let { habit ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = DeepSurface,
            title = { Text("Delete habit?", color = TextPrimary, fontSize = 16.sp) },
            text = { Text("\"${habit.title}\" and its streak will be removed.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(habit)
                    deleteTarget = null
                }) { Text("Delete", color = CrimsonDanger) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Keep", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun HabitRow(item: HabitUi, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(if (item.doneToday) EmeraldFocus else ElevatedSurface, CircleShape)
                .border(1.5.dp, if (item.doneToday) EmeraldFocus else SubtleBorder, CircleShape)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (item.doneToday) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = ObsidianBg, modifier = Modifier.size(18.dp))
            }
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                item.habit.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (item.scheduledToday) TextPrimary else TextMuted,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                item.week.forEach { state ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                when (state) {
                                    true -> EmeraldFocus
                                    false -> CrimsonDanger.copy(alpha = 0.6f)
                                    null -> ElevatedSurface
                                },
                                CircleShape
                            )
                    )
                }
                Text(
                    "  streak ${item.streak}" + if (!item.scheduledToday) " • rest day" else "",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete habit", tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onSave: (String, Set<Int>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var days by remember { mutableStateOf((1..7).toSet()) }
    val canSave = title.isNotBlank() && days.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text("New habit", color = TextPrimary, fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("What do you want to do daily?", color = TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text("Repeat on", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_LETTERS.forEachIndexed { index, letter ->
                        val dayNumber = index + 1
                        val on = days.contains(dayNumber)
                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { days = if (on) days - dayNumber else days + dayNumber },
                            shape = CircleShape,
                            color = if (on) CyanAccent else ElevatedSurface,
                            border = BorderStroke(1.dp, if (on) CyanAccent else SubtleBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    letter,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (on) ObsidianBg else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, days) }, enabled = canSave) {
                Text("Add", color = if (canSave) CyanAccent else TextMuted)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}
