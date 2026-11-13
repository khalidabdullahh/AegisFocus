package com.aegisfocus.app.presentation.mindful

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisOutlinedButton
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.core.theme.AegisFocusTheme
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary

/**
 * Shown before a gated app opens. The app only launches once every
 * self-written mindfulness item has been ticked.
 */
class MindfulGateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val store = ServiceLocator.getContainer(applicationContext).mindfulGateStore
        val gate = store.getGate(packageName)
        if (gate == null || !gate.enabled || gate.items.isEmpty()) {
            finish()
            return
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })

        setContent {
            AegisFocusTheme(darkTheme = true) {
                MindfulGateScreen(
                    appName = gate.appName,
                    items = gate.items,
                    graceMinutes = gate.graceMinutes,
                    onOpenApp = {
                        store.markUnlocked(packageName, gate.graceMinutes)
                        val launch = packageManager.getLaunchIntentForPackage(packageName)
                        if (launch != null) {
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launch)
                        }
                        finish()
                    },
                    onNotNow = { goHome() }
                )
            }
        }
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_gate_package"
    }
}

@Composable
private fun MindfulGateScreen(
    appName: String,
    items: List<String>,
    graceMinutes: Int,
    onOpenApp: () -> Unit,
    onNotNow: () -> Unit
) {
    val checked = remember(items) { mutableStateListOf<Boolean>().apply { items.forEach { add(false) } } }
    val allChecked = checked.isNotEmpty() && checked.all { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Pause for a moment", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Before opening \"$appName\", honestly tick each of your own reminders.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEachIndexed { index, text ->
                val isOn = checked[index]
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { checked[index] = !isOn },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isOn) ElevatedSurface else DeepSurface,
                    border = BorderStroke(1.dp, if (isOn) EmeraldFocus.copy(alpha = 0.6f) else SubtleBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isOn,
                            onCheckedChange = { checked[index] = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldFocus,
                                uncheckedColor = TextMuted,
                                checkmarkColor = ObsidianBg
                            )
                        )
                        Text(text, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            if (allChecked) "Unlocked for $graceMinutes min once you open it." else "${checked.count { it }} of ${items.size} done",
            fontSize = 12.sp,
            color = if (allChecked) EmeraldFocus else TextMuted
        )
        Spacer(Modifier.height(10.dp))
        AegisPrimaryButton(
            text = "Open $appName",
            onClick = onOpenApp,
            enabled = allChecked,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        AegisOutlinedButton(
            text = "Not now",
            onClick = onNotNow,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
