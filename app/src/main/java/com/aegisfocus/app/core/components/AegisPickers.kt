package com.aegisfocus.app.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.domain.model.AppInfo

/** Time picker where the time can be typed (default) or set on a clock dial. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AegisTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = false)
    var useDial by remember { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AegisSectionTitle(title)
                Spacer(Modifier.height(14.dp))
                if (useDial) TimePicker(state = state) else TimeInput(state = state)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { useDial = !useDial }) {
                        Text(if (useDial) "Type time" else "Use clock", color = CyanAccent, fontSize = 13.sp)
                    }
                    Row {
                        TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                        TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                            Text("OK", color = CyanAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Pick several apps from the installed list, with search. */
@Composable
fun AegisAppPickerDialog(
    title: String,
    apps: List<AppInfo>,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var chosen by remember { mutableStateOf(selected) }
    var query by remember { mutableStateOf("") }
    val filtered = apps.filter { query.isBlank() || it.appName.contains(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { AegisSectionTitle(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search apps", color = TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        val on = app.packageName in chosen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosen = if (on) chosen - app.packageName else chosen + app.packageName }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = on,
                                onCheckedChange = { chosen = if (it) chosen + app.packageName else chosen - app.packageName },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CyanAccent,
                                    uncheckedColor = TextMuted,
                                    checkmarkColor = ObsidianBg
                                )
                            )
                            Text(app.appName, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(chosen) }) {
                Text("Done (${chosen.size})", color = CyanAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}
