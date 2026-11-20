package com.aegisfocus.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisCard
import com.aegisfocus.app.core.components.AegisOutlinedButton
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.components.AegisSectionTitle
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.core.theme.CrimsonDanger
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Back up or restore habits, gates, limits, YouTube topics, routines and theme to the user's account. */
@Composable
fun BackupCard() {
    val context = LocalContext.current
    val backup = remember { ServiceLocator.getContainer(context.applicationContext).settingsBackupService }
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var lastMs by remember { mutableStateOf(backup.lastBackupMs()) }

    AegisCard {
        AegisSectionTitle("Cloud backup")
        Spacer(Modifier.height(4.dp))
        Text(
            "Saves your habits, gates, limits, YouTube topics and routines to your account. It also runs by itself when you leave the app.",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (lastMs == 0L) "No backup yet" else "Last backup: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastMs)),
            fontSize = 11.sp,
            color = TextMuted
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AegisPrimaryButton(
                text = if (busy) "Working..." else "Back up now",
                onClick = {
                    if (!busy) {
                        busy = true
                        scope.launch {
                            message = backup.backupNow()
                            lastMs = backup.lastBackupMs()
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            AegisOutlinedButton(
                text = "Restore from cloud",
                onClick = { if (!busy) confirmRestore = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = if (it.startsWith("Backed") || it.startsWith("Restored")) CyanAccent else CrimsonDanger)
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            containerColor = DeepSurface,
            title = { Text("Restore from cloud?", color = TextPrimary, fontSize = 16.sp) },
            text = {
                Text(
                    "This replaces your habits, gates, limits, topics and routines on this phone with the saved copy.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    busy = true
                    scope.launch {
                        message = backup.restoreNow()
                        lastMs = backup.lastBackupMs()
                        busy = false
                    }
                }) { Text("Restore", color = CrimsonDanger) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel", color = TextSecondary) } }
        )
    }
}
