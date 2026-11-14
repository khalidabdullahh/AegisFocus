package com.aegisfocus.app.presentation.productivity

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisCompactButton
import com.aegisfocus.app.core.components.AegisOutlinedButton
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.data.local.Reminder
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Edit
import com.aegisfocus.app.data.local.MindfulGate
import com.aegisfocus.app.data.local.MindfulGateStore
import com.aegisfocus.app.domain.model.AppInfo
import java.time.format.TextStyle
import java.util.Locale

private data class Milestone(val days: Int, val title: String, val text: String)

/**
 * Gentle, motivational milestones based on commonly self-reported experiences.
 * They are encouragement, not medical claims.
 */
private val CleanMilestones = listOf(
    Milestone(1, "Day 1", "You made it through the first day. Each urge you ride out gets weaker."),
    Milestone(3, "Day 3", "Energy and sleep often start to feel steadier. Keep evenings busy."),
    Milestone(7, "One week", "Many people report clearer focus and a calmer mood by now."),
    Milestone(14, "Two weeks", "Self-control feels easier and urges come less often."),
    Milestone(30, "One month", "The habit loop is loosening. Motivation and confidence build."),
    Milestone(60, "Two months", "Steadier mood and stronger discipline in other parts of life."),
    Milestone(90, "Three months", "A new baseline. You are the one in control of this habit.")
)

@Composable
fun ProductivityScreen(
    viewModel: ProductivityViewModel,
    onNavigateToGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showAddReminder by rememberSaveable { mutableStateOf(false) }
    var showStartOver by rememberSaveable { mutableStateOf(false) }
    var showGateDialog by remember { mutableStateOf(false) }
    var editingGate by remember { mutableStateOf<MindfulGate?>(null) }
    val apps by viewModel.apps.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Productivity", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Tools that build daily momentum", fontSize = 12.sp, color = TextSecondary)

        Spacer(Modifier.height(16.dp))

        // Goals
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToGoals),
            shape = RoundedCornerShape(12.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconTile(Icons.Default.Flag, CyanAccent)
                Column(Modifier.weight(1f)) {
                    Text("Daily goals", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Set a focus target and track your habit calendar", fontSize = 11.sp, color = TextSecondary)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // YouTube Intent Mode
        val intentUi by viewModel.intent.collectAsState()
        IntentSection(
            intent = intentUi,
            onToggle = { viewModel.setIntentEnabled(it) },
            onSave = { t, c, b -> viewModel.saveIntent(t, c, b) },
            onRefresh = { viewModel.refreshIntent() }
        )

        Spacer(Modifier.height(20.dp))

        // Mindful Gate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Mindful Gate", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Tick your own reminders before an app opens", fontSize = 11.sp, color = TextSecondary)
            }
            AegisCompactButton(text = "Add", onClick = {
                editingGate = null
                viewModel.loadApps()
                showGateDialog = true
            })
        }
        Spacer(Modifier.height(8.dp))
        if (state.gates.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Text(
                    "No gates yet. Pick an app and write what you want to remember before using it.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.gates.forEach { gate ->
                    GateRow(
                        gate = gate,
                        onToggle = { viewModel.setGateEnabled(gate, it) },
                        onEdit = {
                            editingGate = gate
                            showGateDialog = true
                        },
                        onDelete = { viewModel.deleteGate(gate) }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // App Limits
        val limits by viewModel.limits.collectAsState()
        AppLimitsSection(
            limits = limits,
            apps = apps,
            hasUsageAccess = viewModel.hasUsageAccess,
            onLoadApps = { viewModel.loadApps() },
            onSave = { viewModel.saveLimit(it) },
            onToggle = { l, on -> viewModel.setLimitEnabled(l, on) },
            onDelete = { viewModel.deleteLimit(it) }
        )

        Spacer(Modifier.height(20.dp))

        // Reminders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reminders", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            AegisCompactButton(text = "Add", onClick = { showAddReminder = true })
        }
        Spacer(Modifier.height(8.dp))

        if (state.reminders.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Text(
                    "No reminders yet. Add one to get a daily nudge at a time you choose.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.reminders.forEach { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onToggle = { viewModel.setReminderEnabled(reminder, it) },
                        onDelete = { viewModel.deleteReminder(reminder) }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Clean Streak
        Text("Clean Streak", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("A private daily check-in. Stored only on this device.", fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(8.dp))

        val streak = state.streak
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, if (streak.markedToday) EmeraldFocus.copy(alpha = 0.5f) else SubtleBorder)
        ) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconTile(Icons.Default.Spa, EmeraldFocus)
                    Column {
                        Text(
                            text = if (streak.currentStreak == 1) "1 day" else "${streak.currentStreak} days",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text("Best: ${streak.bestStreak}", fontSize = 11.sp, color = TextMuted)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    streak.lastSevenDays.forEach { (date, done) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(if (done) EmeraldFocus else Color.Transparent, CircleShape)
                                    .border(1.dp, if (done) EmeraldFocus else SubtleBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (done) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (streak.markedToday) {
                    AegisOutlinedButton(
                        text = "Checked in today (tap to undo)",
                        onClick = { viewModel.setTodayChecked(false) },
                        borderColor = EmeraldFocus.copy(alpha = 0.5f),
                        textColor = EmeraldFocus
                    )
                } else {
                    AegisPrimaryButton(
                        text = "I stayed clean today",
                        onClick = { viewModel.setTodayChecked(true) }
                    )
                }

                TextButton(onClick = { showStartOver = true }) {
                    Text("Start over", fontSize = 12.sp, color = TextMuted)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text("What to expect", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(
            "Based on commonly shared experiences. This is encouragement, not medical advice.",
            fontSize = 11.sp,
            color = TextMuted
        )
        Spacer(Modifier.height(8.dp))

        val nextMilestone = CleanMilestones.firstOrNull { streak.currentStreak < it.days }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CleanMilestones.forEach { m ->
                val reached = streak.currentStreak >= m.days
                val isNext = m == nextMilestone
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (reached) EmeraldFocus.copy(alpha = 0.08f) else DeepSurface,
                    border = BorderStroke(1.dp, if (reached) EmeraldFocus.copy(alpha = 0.35f) else if (isNext) CyanAccent.copy(alpha = 0.4f) else SubtleBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (reached) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (reached) EmeraldFocus else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(m.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (reached) EmeraldFocus else TextPrimary)
                            Text(m.text, fontSize = 12.sp, color = TextSecondary)
                            if (isNext) {
                                val left = m.days - streak.currentStreak
                                Text(
                                    if (left == 1) "1 day to go" else "$left days to go",
                                    fontSize = 11.sp,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showGateDialog) {
        GateDialog(
            initial = editingGate,
            apps = apps,
            onDismiss = { showGateDialog = false },
            onSave = { gate ->
                viewModel.saveGate(gate)
                showGateDialog = false
            }
        )
    }

    if (showAddReminder) {
        AddReminderDialog(
            onDismiss = { showAddReminder = false },
            onSave = { title, h, m ->
                viewModel.addReminder(title, h, m)
                showAddReminder = false
            }
        )
    }

    if (showStartOver) {
        AlertDialog(
            onDismissRequest = { showStartOver = false },
            containerColor = DeepSurface,
            title = { Text("Start over?", color = TextPrimary, fontSize = 16.sp) },
            text = {
                Text(
                    "Your current streak will reset to zero. Your best streak is kept. Setbacks happen, what matters is starting again.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startOver()
                    showStartOver = false
                }) { Text("Reset", color = CyanAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showStartOver = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun IconTile(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.15f),
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "%02d:%02d".format(reminder.hour, reminder.minute),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (reminder.enabled) TextPrimary else TextMuted
                )
                Text(reminder.title, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
            Switch(
                checked = reminder.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianBg,
                    checkedTrackColor = EmeraldFocus,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DeepSurface
                )
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete reminder", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, hour: Int, minute: Int) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var hour by rememberSaveable { mutableStateOf(9) }
    var minute by rememberSaveable { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text("New reminder", color = TextPrimary, fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("What should we remind you?", color = TextSecondary, fontSize = 12.sp) },
                    singleLine = true
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Stepper(value = hour, onChange = { hour = (it + 24) % 24 })
                    Text(":", color = TextPrimary, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    Stepper(value = minute, onChange = { minute = (it + 60) % 60 }, step = 5)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, hour, minute) }, enabled = title.isNotBlank()) {
                Text("Save", color = if (title.isNotBlank()) CyanAccent else TextMuted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun Stepper(value: Int, onChange: (Int) -> Unit, step: Int = 1) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onChange(value + step) }) {
            Icon(Icons.Default.Add, contentDescription = "Increase", tint = CyanAccent, modifier = Modifier.size(18.dp))
        }
        Text("%02d".format(value), color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        IconButton(onClick = { onChange(value - step) }) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = CyanAccent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun GateRow(
    gate: MindfulGate,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, if (gate.enabled) CyanAccent.copy(alpha = 0.35f) else SubtleBorder)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(Icons.Default.Lock, CyanAccent)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(gate.appName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (gate.enabled) TextPrimary else TextMuted, maxLines = 1)
                Text(
                    "${gate.items.size} reminder" + (if (gate.items.size == 1) "" else "s") + " · unlocked ${gate.graceMinutes} min",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Switch(
                checked = gate.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianBg,
                    checkedTrackColor = EmeraldFocus,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DeepSurface
                )
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit gate", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete gate", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun GateDialog(
    initial: MindfulGate?,
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onSave: (MindfulGate) -> Unit
) {
    var selectedPkg by remember { mutableStateOf(initial?.packageName) }
    var selectedName by remember { mutableStateOf(initial?.appName ?: "") }
    var itemsText by remember { mutableStateOf(initial?.items?.joinToString("\n") ?: "") }
    var grace by remember { mutableStateOf(initial?.graceMinutes ?: 5) }
    var query by remember { mutableStateOf("") }

    val parsedItems = itemsText.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(MindfulGateStore.MAX_ITEMS)
    val canSave = selectedPkg != null && parsedItems.isNotEmpty()
    val filtered = apps.filter { query.isBlank() || it.appName.contains(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text(if (initial == null) "New Mindful Gate" else "Edit Mindful Gate", color = TextPrimary, fontSize = 16.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (initial == null) {
                    Text(
                        if (selectedPkg == null) "Choose an app" else "App: $selectedName",
                        color = if (selectedPkg == null) TextSecondary else CyanAccent,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search apps", color = TextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            val isSel = app.packageName == selectedPkg
                            Text(
                                app.appName,
                                fontSize = 14.sp,
                                color = if (isSel) CyanAccent else TextPrimary,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPkg = app.packageName
                                        selectedName = app.appName
                                    }
                                    .padding(vertical = 9.dp, horizontal = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    Text("App: $selectedName", color = CyanAccent, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = itemsText,
                    onValueChange = { itemsText = it.take(600) },
                    label = { Text("Your reminders (one per line)", color = TextSecondary, fontSize = 12.sp) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Stay unlocked after opening", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 5, 10, 15, 30).forEach { m ->
                        val on = grace == m
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (on) CyanAccent.copy(alpha = 0.2f) else ElevatedSurface,
                            border = BorderStroke(1.dp, if (on) CyanAccent else SubtleBorder),
                            modifier = Modifier.clickable { grace = m }
                        ) {
                            Text(
                                "${m}m",
                                fontSize = 12.sp,
                                color = if (on) CyanAccent else TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pkg = selectedPkg ?: return@TextButton
                    onSave(
                        MindfulGate(
                            packageName = pkg,
                            appName = selectedName,
                            items = parsedItems,
                            graceMinutes = grace,
                            enabled = initial?.enabled ?: true
                        )
                    )
                },
                enabled = canSave
            ) { Text("Save", color = if (canSave) CyanAccent else TextMuted) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
