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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.R
import com.aegisfocus.app.core.theme.AegisTitleFamily
import com.aegisfocus.app.core.theme.DarkCyanAccent
import com.aegisfocus.app.core.theme.DarkDeepSurface
import com.aegisfocus.app.core.theme.DarkElevatedSurface
import com.aegisfocus.app.core.theme.DarkEmeraldFocus
import com.aegisfocus.app.core.theme.DarkObsidianBg
import com.aegisfocus.app.core.theme.DarkSubtleBorder
import com.aegisfocus.app.core.theme.DarkTextMuted
import com.aegisfocus.app.core.theme.DarkTextPrimary
import com.aegisfocus.app.core.theme.DarkTextSecondary
import com.aegisfocus.app.domain.campfire.CampfireMode
import com.aegisfocus.app.domain.campfire.CampfirePresets
import com.aegisfocus.app.domain.campfire.CampfireSound

// The Campfire screen is always dark (it sits on photos), whatever theme the rest of the app uses.

/** Every preset card and mixer row uses the same size so nothing jumps around. */
private val PresetCardWidth = 178.dp
private val PresetCardHeight = 92.dp
private val MixerRowHeight = 84.dp
private val ChipHeight = 42.dp
private val ScreenPadding = 16.dp

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

private fun CampfireMode.accentColor(): Color = when (this) {
    CampfireMode.SLEEP -> Color(0xFFF2A65A)   // campfire amber
    CampfireMode.STUDY -> Color(0xFFE9C46A)   // highlighter gold
    CampfireMode.FOCUS -> DarkEmeraldFocus    // desk-lamp green
}

private fun CampfireMode.heroRes(): Int = when (this) {
    CampfireMode.SLEEP -> R.drawable.bg_campfire_sleep
    CampfireMode.STUDY -> R.drawable.bg_study_desk
    CampfireMode.FOCUS -> R.drawable.bg_deep_focus
}

private fun CampfireMode.icon(): ImageVector = when (this) {
    CampfireMode.SLEEP -> Icons.Filled.Bedtime
    CampfireMode.STUDY -> Icons.Filled.School
    CampfireMode.FOCUS -> Icons.Filled.CenterFocusStrong
}

private fun CampfireMode.tagline(): String = when (this) {
    CampfireMode.SLEEP -> "Drift off to calming sounds"
    CampfireMode.STUDY -> "Stay calm and absorb more"
    CampfireMode.FOCUS -> "Shut out the noise and go deep"
}

private fun formatRemaining(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/**
 * Clear picture at the top of the screen (no dark overlay, no zoom). It fades into the page
 * background at the bottom. Only the Deep Sleep fire gets a soft flicker, and only while playing.
 */
@Composable
private fun CampfireHero(mode: CampfireMode, isPlaying: Boolean) {
    var flicker = 0f
    val fireOn = isPlaying && mode == CampfireMode.SLEEP
    if (fireOn) {
        val t = rememberInfiniteTransition(label = "fire")
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
        flicker = 0.5f * (f1 + f2)
    }

    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1080f / 1000f)) {
        Crossfade(targetState = mode, animationSpec = tween(450), label = "hero") { m ->
            Image(
                painter = painterResource(m.heroRes()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (fireOn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF8A3D).copy(alpha = 0.04f + 0.14f * flicker),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.55f, size.height * 0.6f),
                                radius = size.minDimension * 0.55f
                            )
                        )
                    }
            )
        }
        // soft top edge for the status bar and a long fade into the page below the picture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x66000000),
                        0.18f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to DarkObsidianBg
                    )
                )
        )
    }
}

@Composable
private fun Header(title: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkTextPrimary)
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionText, fontSize = 13.sp, color = DarkCyanAccent)
            }
        }
    }
}

@Composable
fun CampfireScreen(
    viewModel: CampfireViewModel,
    onNavigateToPaywall: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    val accent by animateColorAsState(state.mode.accentColor(), tween(450), label = "accent")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidianBg),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ----- Hero picture -----
        item { CampfireHero(mode = state.mode, isPlaying = state.isPlaying) }

        // ----- Title (sits on the faded part of the picture) -----
        item {
            Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
                Text(
                    "Campfire",
                    style = TextStyle(
                        fontFamily = AegisTitleFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    ),
                    color = DarkTextPrimary
                )
                Text(state.mode.tagline(), fontSize = 13.sp, color = DarkTextSecondary)
            }
        }

        // ----- Mode toggle -----
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = ScreenPadding)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkDeepSurface)
                    .border(BorderStroke(1.dp, DarkSubtleBorder), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CampfireMode.values().forEach { mode ->
                    val selected = state.mode == mode
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) DarkElevatedSurface else Color.Transparent)
                            .clickable { viewModel.selectMode(mode) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = mode.icon(),
                            contentDescription = null,
                            tint = if (selected) accent else DarkTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            mode.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) DarkTextPrimary else DarkTextMuted
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
                    .padding(horizontal = ScreenPadding)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkDeepSurface)
                    .border(BorderStroke(1.dp, DarkSubtleBorder), RoundedCornerShape(20.dp))
                    .padding(vertical = 18.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (active > 0) accent else DarkSubtleBorder)
                        .clickable(enabled = active > 0) { viewModel.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = DarkObsidianBg,
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
                    color = DarkTextSecondary
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

        // ----- Timer: four equal buttons -----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Header(
                    title = when (state.mode) {
                        CampfireMode.SLEEP -> "Sleep timer"
                        CampfireMode.STUDY -> "Study timer"
                        CampfireMode.FOCUS -> "Focus timer"
                    },
                    actionText = if (state.timerRemainingMs > 0) "Cancel timer" else null,
                    onAction = { viewModel.cancelTimer() }
                )
                Row(
                    modifier = Modifier
                        .padding(horizontal = ScreenPadding)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.mode.timerMinutes.forEach { minutes ->
                        TimerChip(
                            label = "$minutes min",
                            modifier = Modifier.weight(1f)
                        ) { viewModel.startTimer(minutes) }
                    }
                }
            }
        }

        // ----- Presets: all the same size -----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Header(title = "Presets")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(CampfirePresets.forMode(state.mode)) { preset ->
                        val locked = !preset.isFree && !state.isPro
                        Column(
                            modifier = Modifier
                                .width(PresetCardWidth)
                                .height(PresetCardHeight)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkDeepSurface)
                                .border(BorderStroke(1.dp, DarkSubtleBorder), RoundedCornerShape(16.dp))
                                .clickable {
                                    if (locked) onNavigateToPaywall() else viewModel.applyPreset(preset)
                                }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = if (locked) DarkTextMuted else accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    preset.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.size(6.dp))
                            Text(
                                preset.tagline,
                                fontSize = 12.sp,
                                color = DarkTextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // ----- Mixer -----
        item { Header(title = "Mixer") }
        items(state.mode.sounds) { sound ->
            val locked = !sound.isFree && !state.isPro
            val level = state.levels[sound] ?: 0f
            Row(
                modifier = Modifier
                    .padding(horizontal = ScreenPadding)
                    .fillMaxWidth()
                    .height(MixerRowHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkDeepSurface)
                    .border(BorderStroke(1.dp, DarkSubtleBorder), RoundedCornerShape(14.dp))
                    .then(if (locked) Modifier.clickable { onNavigateToPaywall() } else Modifier)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = sound.icon(),
                    contentDescription = null,
                    tint = if (level > 0f) accent else DarkTextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(sound.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkTextPrimary)
                    if (locked) {
                        Text("Pro sound • tap to unlock", fontSize = 12.sp, color = DarkTextMuted)
                    } else {
                        Slider(
                            value = level,
                            onValueChange = { viewModel.setLevel(sound, it) },
                            onValueChangeFinished = { viewModel.persist() },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = DarkSubtleBorder
                            )
                        )
                    }
                }
                if (locked) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Pro",
                        tint = DarkTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ----- Saved mixes -----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Header(
                    title = "My mixes",
                    actionText = if (state.levels.isNotEmpty()) "Save current" else null,
                    onAction = {
                        if (!state.isPro && state.savedMixes.isNotEmpty()) onNavigateToPaywall()
                        else showSaveDialog = true
                    }
                )
                if (state.savedMixes.isEmpty()) {
                    Text(
                        "Dial in a mix you love, then tap Save current.",
                        fontSize = 13.sp,
                        color = DarkTextMuted,
                        modifier = Modifier.padding(horizontal = ScreenPadding)
                    )
                } else {
                    state.savedMixes.forEach { mix ->
                        Row(
                            modifier = Modifier
                                .padding(horizontal = ScreenPadding)
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkDeepSurface)
                                .border(BorderStroke(1.dp, DarkSubtleBorder), RoundedCornerShape(12.dp))
                                .clickable { viewModel.applySaved(mix) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mix.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary)
                                Text(
                                    "${mix.mode.label} • ${mix.levels.size} sounds",
                                    fontSize = 12.sp,
                                    color = DarkTextMuted
                                )
                            }
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Delete mix",
                                tint = DarkTextMuted,
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

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = DarkDeepSurface,
            title = { Text("Name your mix", color = DarkTextPrimary) },
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
                }) { Text("Save", color = DarkCyanAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel", color = DarkTextMuted) }
            }
        )
    }
}

/** Same height everywhere; width comes from the caller (weight) so all four are equal. */
@Composable
private fun TimerChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(ChipHeight)
            .clip(RoundedCornerShape(50))
            .background(DarkDeepSurface)
            .border(BorderStroke(1.dp, DarkSubtleBorder), RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = DarkTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
