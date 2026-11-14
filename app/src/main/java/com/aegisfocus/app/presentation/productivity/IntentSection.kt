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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary

data class IntentUi(
    val enabled: Boolean,
    val topicsText: String,
    val channelsText: String,
    val blockedText: String,
    val lastCheck: String,
    val exceptions: List<String>
)

/** "YouTube Intent Mode": the user writes what they want to watch; other videos are blocked. */
@Composable
fun IntentSection(
    intent: IntentUi,
    onToggle: (Boolean) -> Unit,
    onSave: (topics: String, channels: String, blocked: String) -> Unit,
    onRefresh: () -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, if (intent.enabled) CyanAccent.copy(alpha = 0.4f) else SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("YouTube Intent Mode", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        "Write what you want to watch. Videos that don't match get blocked.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = intent.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBg,
                        checkedTrackColor = EmeraldFocus,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DeepSurface
                    )
                )
            }

            Spacer(Modifier.height(10.dp))
            Text("Your topics", fontSize = 12.sp, color = TextMuted)
            Text(
                intent.topicsText.lines().filter { it.isNotBlank() }.joinToString(" • ") { it.substringBefore(':').trim() },
                fontSize = 13.sp,
                color = CyanAccent
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip("Edit topics") { showEdit = true }
                ActionChip("Refresh check") { onRefresh() }
            }

            Spacer(Modifier.height(12.dp))
            Text("Last check", fontSize = 12.sp, color = TextMuted)
            Text(intent.lastCheck, fontSize = 11.sp, color = TextSecondary)

            if (intent.exceptions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Videos you allowed (with your reason)", fontSize = 12.sp, color = TextMuted)
                intent.exceptions.take(3).forEach { Text(it, fontSize = 11.sp, color = TextSecondary, maxLines = 2) }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Needs the accessibility service. Reads only the video title on screen, on your phone. It can make mistakes, so use \"This one is useful\" when a video is wrongly blocked.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }

    if (showEdit) {
        var topics by remember { mutableStateOf(intent.topicsText) }
        var channels by remember { mutableStateOf(intent.channelsText) }
        var blocked by remember { mutableStateOf(intent.blockedText) }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            containerColor = DeepSurface,
            title = { Text("Edit what you want to watch", color = TextPrimary, fontSize = 16.sp) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = topics,
                        onValueChange = { topics = it },
                        label = { Text("Topics (one per line: Name: word, word)", color = TextSecondary, fontSize = 12.sp) },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = channels,
                        onValueChange = { channels = it },
                        label = { Text("Always allow channels (comma separated)", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = blocked,
                        onValueChange = { blocked = it },
                        label = { Text("Always block words (comma separated)", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(topics, channels, blocked)
                    showEdit = false
                }) { Text("Save", color = CyanAccent) }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel", color = TextSecondary) } }
        )
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = CyanAccent.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, CyanAccent)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CyanAccent
        )
    }
}
