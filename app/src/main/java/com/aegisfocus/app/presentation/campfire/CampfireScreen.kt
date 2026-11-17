package com.aegisfocus.app.presentation.campfire

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.R
import com.aegisfocus.app.core.components.SectionHeader
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.domain.campfire.CampfireMode
import com.aegisfocus.app.domain.campfire.CampfirePresets
import com.aegisfocus.app.domain.campfire.CampfireSound

private fun CampfireSound.icon(): ImageVector = when (this) {
    CampfireSound.CAMPFIRE -> Icons.Filled.LocalFireDepartment
    CampfireSound.CRICKETS -> Icons.Filled.Grass
    CampfireSound.OWL -> Icons.Filled.Visibility
    CampfireSound.WIND -> Icons.Filled.Air
    CampfireSound.RAIN -> Icons.Filled.WaterDrop
    CampfireSound.THUNDER -> Icons.Filled.Bolt
    CampfireSound.STREAM -> Icons.Filled.Water
    CampfireSound.BROWN -> Icons.Filled.GraphicEq
    CampfireSound.PINK -> Icons.Filled.Waves
}

/** Cards are slightly see-through so the mode background shows behind them. */
private val Glass = DeepSurface.copy(alpha = 0.76f)

private fun CampfireMode.accentColor(): Color = when (this) {
    CampfireMode.SLEEP -> Color(0xFFF2A65A)   // campfire amber
    CampfireMode.STUDY -> Color(0xFFE9C46A)   // highlighter gold
    CampfireMode.FOCUS -> EmeraldFocus        // desk-lamp green
}

private fun CampfireMode.backgroundRes(): Int = when (this) {
    CampfireMode.SLEEP -> R.drawable.bg_campfire_sleep
    CampfireMode.STUDY -> R.drawable.bg_study_desk
    CampfireMode.FOCUS -> R.drawable.bg_deep_focus
}

private fun CampfireMode.icon(): ImageVector = when (this) {
    CampfireMode.SLEEP -> Icons.Filled.Bedtime
    CampfireMode.STUDY -> Icons.Filled.School
    CampfireMode.FOCUS -> Icons.Filled.CenterFocusStrong
}

/**
 * Full-screen background that follows the selected mode (crossfade). While sound is playing it
 * gets a slow zoom, plus a flickering firelight glow in Deep Sleep. Nothing animates when paused,
 * so it costs no battery.
 */
@Composable
private fun CampfireBackground(mode: CampfireMode, isPlaying: Boolean) {
    var zoom = 1f
    var flicker = 0f
    if (isPlaying) {
        val t = rememberInfiniteTransition(label = "campfire-motion")
        zoom = 1f + 0.05f * t.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Reverse),
            label = "zoom"
        ).value
        val f1 = t.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1_700, easing = LinearEasing), RepeatMode.Reverse),
            label = "flicker-a"
        ).value
        val f2 = t.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2_300, easing = LinearEasing), RepeatMode.Reverse),
            label = "flicker-b"
        ).value
        flicker = 0.5f * (f1 + f2)   // two periods mixed => uneven, fire-like
    }

    Crossfade(targetState = mode, animationSpec = tween(450), label = "campfire-bg") { m ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(m.backgroundRes()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                    }
            )
            if (m == CampfireMode.SLEEP && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFF8A3D).copy(alpha = 0.10f + 0.16f * flicker),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.58f, size.height * 0.62f),
                                    radius = size.minDimension * 0.7f
                                )
                            )
                        }
                )
            }
            // dark scrim so text and sliders stay readable on any photo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x66000000),
                            0.35f to Color(0x99060D10),
                            1f to Color(0xEE060D10)
                        )
                    )
            )
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
fun CampfireScreen(
    viewModel: CampfireViewModel,
    onNavigateToPaywall: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    val accent by animateColorAsState(state.mode.accentColor(), tween(450), label = "accent")

    Box(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
    CampfireBackground(mode = state.mode, isPlaying = state.isPlaying)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ----- Header -----
        item {
            Column {
                Text("Campfire", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "Layer calming sounds for deep sleep or a calm study session.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        // ----- Mode toggle -----
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Glass)
                    .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CampfireMode.values().forEach { mode ->
                    val selected = state.mode == mode
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) ElevatedSurface else Color.Transparent)
                            .clickable { viewModel.selectMode(mode) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = mode.icon(),
                            contentDescription = null,
                            tint = if (selected) accent else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            mode.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) TextPrimary else TextMuted
                        )
                    }
                }
            }
        }

        // ----- Player card -----
        item {
            val active = state.levels.size
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Glass)
                    .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (active > 0) accent else SubtleBorder)
                        .clickable(enabled = active > 0) { viewModel.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = ObsidianBg,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = when {
                        active == 0 -> "Pick a sound or preset to begin"
                        state.isPlaying -> "Playing • $active ${if (active == 1) "sound" else "sounds"}"
                        else -> "Paused • $active ${if (active == 1) "sound" else "sounds"} ready"
                    },
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                if (state.timerRemainingMs > 0) {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "Fades out in ${formatRemaining(state.timerRemainingMs)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
            }
        }

        // ----- Timer -----
        item {
            Column {
                SectionHeader(
                    title = when (state.mode) {
                        CampfireMode.SLEEP -> "Sleep timer"
                        CampfireMode.STUDY -> "Study timer"
                        CampfireMode.FOCUS -> "Focus timer"
                    }
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.mode.timerMinutes) { minutes ->
                        Chip(label = if (minutes >= 60 && minutes % 60 == 0) "${minutes / 60} h" else "$minutes min", tint = accent) {
                            viewModel.startTimer(minutes)
                        }
                    }
                    if (state.timerRemainingMs > 0) {
                        item { Chip(label = "Cancel timer", tint = accent, highlight = true) { viewModel.cancelTimer() } }
                    }
                }
            }
        }

        // ----- Presets -----
        item {
            Column {
                SectionHeader(title = "Presets")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(CampfirePresets.forMode(state.mode)) { preset ->
                        val locked = !preset.isFree && !state.isPro
                        Column(
                            modifier = Modifier
                                .width(158.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Glass)
                                .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(16.dp))
                                .clickable {
                                    if (locked) onNavigateToPaywall() else viewModel.applyPreset(preset)
                                }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = if (locked) TextMuted else accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    preset.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.size(4.dp))
                            Text(preset.tagline, fontSize = 12.sp, color = TextMuted, maxLines = 2)
                        }
                    }
                }
            }
        }

        // ----- Mixer -----
        item { SectionHeader(title = "Mixer") }
        items(state.mode.sounds) { sound ->
            val locked = !sound.isFree && !state.isPro
            val level = state.levels[sound] ?: 0f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Glass)
                    .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(14.dp))
                    .then(if (locked) Modifier.clickable { onNavigateToPaywall() } else Modifier)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = sound.icon(),
                    contentDescription = null,
                    tint = if (level > 0f) accent else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(sound.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    if (locked) {
                        Text("Pro sound", fontSize = 12.sp, color = TextMuted)
                    } else {
                        Slider(
                            value = level,
                            onValueChange = { viewModel.setLevel(sound, it) },
                            onValueChangeFinished = { viewModel.persist() },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = SubtleBorder
                            )
                        )
                    }
                }
                if (locked) {
                    Icon(Icons.Filled.Lock, contentDescription = "Pro", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ----- Saved mixes -----
        item {
            Column {
                SectionHeader(
                    title = "My mixes",
                    actionText = if (state.levels.isNotEmpty()) "Save current" else null,
                    onActionClick = {
                        if (!state.isPro && state.savedMixes.isNotEmpty()) onNavigateToPaywall()
                        else showSaveDialog = true
                    }
                )
                if (state.savedMixes.isEmpty()) {
                    Text(
                        "Dial in a mix you love, then tap Save current.",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                } else {
                    state.savedMixes.forEach { mix ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Glass)
                                .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(12.dp))
                                .clickable { viewModel.applySaved(mix) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mix.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    "${mix.mode.label} • ${mix.levels.size} sounds",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Delete mix",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.deleteMix(mix.id) }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    } // end Box

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = DeepSurface,
            title = { Text("Name your mix", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    placeholder = { Text("e.g. Exam night") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveCurrentMix(name)
                    showSaveDialog = false
                }) { Text("Save", color = CyanAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

@Composable
private fun Chip(label: String, tint: Color, highlight: Boolean = false, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = if (highlight) tint else TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Glass)
            .border(BorderStroke(1.dp, if (highlight) tint else SubtleBorder), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    )
}
