package com.aegisfocus.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisOutlinedButton
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.components.AegisSectionTitle
import com.aegisfocus.app.core.theme.CrimsonDanger
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.domain.model.Schedule

/** Bottom sheet shown when a routine card is tapped: edit or delete it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleActionSheet(
    schedule: Schedule,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DeepSurface) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AegisSectionTitle(schedule.name)
            Spacer(Modifier.height(4.dp))
            AegisPrimaryButton(text = "Edit routine", onClick = onEdit, modifier = Modifier.fillMaxWidth())
            AegisOutlinedButton(text = "Delete routine", onClick = onDelete, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ScheduleDeleteDialog(schedule: Schedule, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text("Delete routine?", color = TextPrimary, fontSize = 16.sp) },
        text = { Text("\"${schedule.name}\" will be removed and will not start again.", color = TextSecondary, fontSize = 13.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = CrimsonDanger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep", color = TextSecondary) } }
    )
}

@Composable
fun ScheduleInfoDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text("Strict routine is running", color = TextPrimary, fontSize = 16.sp) },
        text = { Text(message, color = TextSecondary, fontSize = 13.sp) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = CyanAccent) } }
    )
}
