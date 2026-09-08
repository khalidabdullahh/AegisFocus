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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.theme.AlertStrict
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalmMindfulnessDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    onConfirm: (reason: String) -> Unit,
    onDismiss: () -> Unit,
    cooldownSeconds: Int = 5,
    requireConfirmationPhrase: Boolean = true
) {
    var countdown by remember { mutableIntStateOf(cooldownSeconds) }
    var inputPhrase by remember { mutableStateOf("") }
    var reasonNote by remember { mutableStateOf("") }

    val requiredPhrase = "I choose to break focus"

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    val isPhraseValid = !requireConfirmationPhrase || inputPhrase.trim().equals(requiredPhrase, ignoreCase = true)
    val isConfirmEnabled = countdown == 0 && isPhraseValid

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, AlertStrict.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AlertStrict,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                if (requireConfirmationPhrase) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Type below to confirm:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                    Text(
                        text = "\"$requiredPhrase\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlertStrict
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputPhrase,
                        onValueChange = { inputPhrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type confirmation phrase", color = TextMuted, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AlertStrict,
                            unfocusedBorderColor = SubtleBorder,
                            cursorColor = AlertStrict
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = reasonNote,
                    onValueChange = { reasonNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Why are you exiting? (Optional reflection)", color = TextMuted, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SubtleBorder,
                        unfocusedBorderColor = SubtleBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Stay Focused",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    AegisPrimaryButton(
                        text = if (countdown > 0) "Wait (${countdown}s)" else confirmButtonText,
                        onClick = { onConfirm(reasonNote) },
                        enabled = isConfirmEnabled,
                        isDanger = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}
