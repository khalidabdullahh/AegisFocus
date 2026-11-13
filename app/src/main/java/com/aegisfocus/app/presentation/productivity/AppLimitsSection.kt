package com.aegisfocus.app.presentation.productivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.data.local.AppLimit
import com.aegisfocus.app.domain.model.AppInfo

/** Per-app daily limits: maximum minutes and/or maximum number of opens. */
@Composable
fun AppLimitsSection(
    limits: List<AppLimitUi>,
    apps: List<AppInfo>,
    hasUsageAccess: Boolean,
    onLoadApps: () -> Unit,
    onSave: (AppLimit) -> Unit,
    onToggle: (AppLimit, Boolean) -> Unit,
    onDelete: (AppLimit) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AppLimit?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("App Limits", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Daily time and open-count limits per app", fontSize = 11.sp, color = TextSecondary)
        }
        Surface(
            modifier = Modifier.clickable {
                editing = null
                onLoadApps()
                showDialog = true
            },
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
    Spacer(Modifier.height(8.dp))

    if (limits.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Text(
                "No limits yet. Example: Instagram, 30 min and 5 opens per day.",
                modifier = Modifier.padding(14.dp),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            limits.forEach { item ->
                val limit = item.limit
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editing = limit
                            showDialog = true
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = DeepSurface,
                    border = BorderStroke(1.dp, if (limit.enabled) CyanAccent.copy(alpha = 0.35f) else SubtleBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                limit.appName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (limit.enabled) TextPrimary else TextMuted,
                                maxLines = 1
                            )
                            Text(limitSummary(limit), fontSize = 11.sp, color = TextSecondary)
                            Text(
                                "Today: " + (item.usedMinutes?.let { "$it min" } ?: "-") + " • ${item.opensToday} opens",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = limit.enabled,
                            onCheckedChange = { onToggle(limit, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = EmeraldFocus,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DeepSurface
                            )
                        )
                        IconButton(onClick = { onDelete(limit) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete limit", tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        if (!hasUsageAccess && limits.any { it.limit.dailyMinutes > 0 }) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Time limits need Usage Access (Android Settings > Special access > Usage access > Aegis Focus). Open limits work without it.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }

    if (showDialog) {
        LimitDialog(
            initial = editing,
            apps = apps,
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            }
        )
    }
}

private fun limitSummary(limit: AppLimit): String {
    val parts = mutableListOf<String>()
    if (limit.dailyMinutes > 0) parts.add("${limit.dailyMinutes} min/day")
    if (limit.maxOpens > 0) parts.add("${limit.maxOpens} opens/day")
    return parts.joinToString(" • ")
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) CyanAccent.copy(alpha = 0.2f) else ElevatedSurface,
        border = BorderStroke(1.dp, if (selected) CyanAccent else SubtleBorder)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (selected) CyanAccent else TextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun LimitDialog(
    initial: AppLimit?,
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onSave: (AppLimit) -> Unit
) {
    var selectedPkg by remember { mutableStateOf(initial?.packageName) }
    var selectedName by remember { mutableStateOf(initial?.appName ?: "") }
    var minutes by remember { mutableStateOf(initial?.dailyMinutes ?: 30) }
    var opens by remember { mutableStateOf(initial?.maxOpens ?: 0) }
    var query by remember { mutableStateOf("") }

    val canSave = selectedPkg != null && (minutes > 0 || opens > 0)
    val filtered = apps.filter { query.isBlank() || it.appName.contains(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text(if (initial == null) "New app limit" else "Edit app limit", color = TextPrimary, fontSize = 16.sp) },
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
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(140.dp)) {
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

                Text("Max time per day", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 15, 30, 60, 120).forEach { m ->
                        Choice(if (m == 0) "None" else "${m}m", minutes == m) { minutes = m }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Max opens per day", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 3, 5, 10, 20).forEach { o ->
                        Choice(if (o == 0) "None" else "$o", opens == o) { opens = o }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pkg = selectedPkg ?: return@TextButton
                    onSave(
                        AppLimit(
                            packageName = pkg,
                            appName = selectedName,
                            dailyMinutes = minutes,
                            maxOpens = opens,
                            enabled = initial?.enabled ?: true
                        )
                    )
                },
                enabled = canSave
            ) { Text("Save", color = if (canSave) CyanAccent else TextMuted) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}
