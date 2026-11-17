package com.aegisfocus.app.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisCard
import com.aegisfocus.app.core.components.AegisSectionTitle
import com.aegisfocus.app.core.theme.AegisThemeState
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.core.theme.ThemeMode

/** Settings card to switch between the dark theme, the light "paper" theme and the phone's setting. */
@Composable
fun AppearanceCard() {
    val context = LocalContext.current
    val current = AegisThemeState.mode

    AegisCard {
        AegisSectionTitle("Appearance")
        Spacer(Modifier.height(4.dp))
        Text("Choose how Aegis Focus looks.", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ThemeMode.SYSTEM to "Phone",
                ThemeMode.DARK to "Dark",
                ThemeMode.LIGHT to "Light"
            ).forEach { (mode, label) ->
                val selected = current == mode
                Surface(
                    modifier = Modifier.clickable { AegisThemeState.set(context, mode) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) CyanAccent.copy(alpha = 0.18f) else ElevatedSurface,
                    border = BorderStroke(1.dp, if (selected) CyanAccent else SubtleBorder)
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) CyanAccent else TextPrimary
                    )
                }
            }
        }
    }
}
