package com.aegisfocus.app.presentation.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.core.theme.CrimsonDanger
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.data.local.PinStore
import kotlinx.coroutines.delay

private const val PIN_LENGTH = 4

/** Shows [content] only after the app PIN has been entered (if a PIN is set). */
@Composable
fun PinProtected(pinStore: PinStore, content: @Composable () -> Unit) {
    var unlocked by remember { mutableStateOf(!pinStore.hasPin() || pinStore.isUnlocked()) }
    if (unlocked) {
        content()
    } else {
        PinEntryScreen(pinStore = pinStore, onUnlocked = { unlocked = true })
    }
}

@Composable
private fun PinEntryScreen(pinStore: PinStore, onUnlocked: () -> Unit) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var lockSeconds by remember { mutableIntStateOf(pinStore.lockoutSecondsLeft()) }

    // Count the lockout down
    LaunchedEffect(lockSeconds) {
        if (lockSeconds > 0) {
            delay(1000)
            lockSeconds = pinStore.lockoutSecondsLeft()
        }
    }

    fun submit(pin: String) {
        if (pinStore.verify(pin)) {
            onUnlocked()
        } else {
            error = true
            entered = ""
            lockSeconds = pinStore.lockoutSecondsLeft()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter PIN", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Protected to keep your limits and gates safe", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(PIN_LENGTH) { index ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            if (index < entered.length) CyanAccent else ElevatedSurface,
                            CircleShape
                        )
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                lockSeconds > 0 -> "Too many tries. Wait $lockSeconds s."
                error -> "Wrong PIN"
                else -> " "
            },
            fontSize = 12.sp,
            color = CrimsonDanger
        )
        Spacer(Modifier.height(16.dp))

        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "<"))
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.size(64.dp))
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(64.dp)
                                .clickable(enabled = lockSeconds == 0) {
                                    error = false
                                    if (key == "<") {
                                        entered = entered.dropLast(1)
                                    } else if (entered.length < PIN_LENGTH) {
                                        entered += key
                                        if (entered.length == PIN_LENGTH) submit(entered)
                                    }
                                },
                            shape = CircleShape,
                            color = DeepSurface,
                            border = BorderStroke(1.dp, SubtleBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    if (key == "<") "⌫" else key,
                                    fontSize = 22.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Settings card to set, change or remove the app PIN. */
@Composable
fun PinSettingsCard() {
    val context = LocalContext.current
    val pinStore = remember { ServiceLocator.getContainer(context.applicationContext).pinStore }
    var hasPin by remember { mutableStateOf(pinStore.hasPin()) }
    var dialog by remember { mutableStateOf<String?>(null) } // "set", "change", "remove"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("App PIN lock", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                if (hasPin) "Settings and Productivity ask for your PIN, so limits and gates can't be changed on impulse."
                else "Set a 4 digit PIN so you can't turn off your own limits on impulse. If you forget it you must clear app data.",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasPin) {
                    PinChip("Set PIN") { dialog = "set" }
                } else {
                    PinChip("Change PIN") { dialog = "change" }
                    PinChip("Remove PIN") { dialog = "remove" }
                }
            }
        }
    }

    dialog?.let { mode ->
        PinDialog(
            title = when (mode) {
                "set" -> "Set PIN"
                "change" -> "Change PIN"
                else -> "Remove PIN"
            },
            askCurrent = mode != "set",
            askNew = mode != "remove",
            onDismiss = { dialog = null },
            onConfirm = { current, new, confirm ->
                when {
                    mode != "set" && !pinStore.verify(current) -> "Current PIN is wrong"
                    mode == "remove" -> {
                        pinStore.clearPin()
                        hasPin = false
                        dialog = null
                        null
                    }
                    new.length != PIN_LENGTH || !new.all { it.isDigit() } -> "PIN must be $PIN_LENGTH digits"
                    new != confirm -> "PINs do not match"
                    else -> {
                        pinStore.setPin(new)
                        hasPin = true
                        dialog = null
                        null
                    }
                }
            }
        )
    }
}

@Composable
private fun PinChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = CyanAccent.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, CyanAccent)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CyanAccent
        )
    }
}

@Composable
private fun PinDialog(
    title: String,
    askCurrent: Boolean,
    askNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (current: String, new: String, confirm: String) -> String?
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    @Composable
    fun field(label: String, value: String, onChange: (String) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(PIN_LENGTH)) },
            label = { Text(label, color = TextSecondary, fontSize = 12.sp) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text(title, color = TextPrimary, fontSize = 16.sp) },
        text = {
            Column {
                if (askCurrent) field("Current PIN", current) { current = it }
                if (askNew) {
                    field("New PIN (4 digits)", new) { new = it }
                    field("Repeat new PIN", confirm) { confirm = it }
                }
                error?.let { Text(it, color = CrimsonDanger, fontSize = 12.sp, textAlign = TextAlign.Start) }
            }
        },
        confirmButton = {
            TextButton(onClick = { error = onConfirm(current, new, confirm) }) {
                Text("OK", color = CyanAccent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}
