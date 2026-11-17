package com.aegisfocus.app.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens for the dark ("calm slate") and light ("paper") themes.
 *
 * Every colour name used across the app (ObsidianBg, TextPrimary, CyanAccent ...) is a property that
 * returns the dark or light value depending on [AegisPalette.isDark]. [AegisFocusTheme] sets that flag,
 * and because it is Compose state, screens redraw when the theme is switched.
 * The fixed DarkXxx and LightXxx values below are used where a colour must not change (for example Material schemes).
 */
object AegisPalette {
    var isDark by mutableStateOf(true)
}

internal val DarkObsidianBg = Color(0xFF0E181D)
internal val LightObsidianBg = Color(0xFFF7F3EA)
internal val DarkDeepSurface = Color(0xFF15242B)
internal val LightDeepSurface = Color(0xFFFFFCF6)
internal val DarkElevatedSurface = Color(0xFF1C2F38)
internal val LightElevatedSurface = Color(0xFFEFE8DA)
internal val DarkSubtleBorder = Color(0xFF2B434D)
internal val LightSubtleBorder = Color(0xFFD8CFBC)
internal val DarkTextPrimary = Color(0xFFEEF2EE)
internal val LightTextPrimary = Color(0xFF1D2A2E)
internal val DarkTextSecondary = Color(0xFFA8B9BC)
internal val LightTextSecondary = Color(0xFF4F6166)
internal val DarkTextMuted = Color(0xFF7A8E93)
internal val LightTextMuted = Color(0xFF74868A)
internal val DarkCyanAccent = Color(0xFF86C5B4)
internal val LightCyanAccent = Color(0xFF2E8B78)
internal val DarkCyanAccentMuted = Color(0xFF4F8A7E)
internal val LightCyanAccentMuted = Color(0xFF7DB5A8)
internal val DarkIndigoAccent = Color(0xFFA9A0D6)
internal val LightIndigoAccent = Color(0xFF6C62B5)
internal val DarkEmeraldFocus = Color(0xFF9CC5A1)
internal val LightEmeraldFocus = Color(0xFF3F9A58)
internal val DarkAmberWarning = Color(0xFFD8B574)
internal val LightAmberWarning = Color(0xFFB5802A)
internal val DarkAlertStrict = Color(0xFFC77D7D)
internal val LightAlertStrict = Color(0xFFB24B4B)
internal val DarkAlertStrictDark = Color(0xFF7A3F3F)
internal val LightAlertStrictDark = Color(0xFFE9B8B8)
internal val DarkCrimsonDanger = Color(0xFFC77D7D)
internal val LightCrimsonDanger = Color(0xFFB24B4B)
internal val DarkCardGlow = Color(0x1A86C5B4)
internal val LightCardGlow = Color(0x1A2E8B78)
internal val DarkStrictCardGlow = Color(0x1AC77D7D)
internal val LightStrictCardGlow = Color(0x1AB24B4B)

val ObsidianBg: Color get() = if (AegisPalette.isDark) DarkObsidianBg else LightObsidianBg
val DeepSurface: Color get() = if (AegisPalette.isDark) DarkDeepSurface else LightDeepSurface
val ElevatedSurface: Color get() = if (AegisPalette.isDark) DarkElevatedSurface else LightElevatedSurface
val SubtleBorder: Color get() = if (AegisPalette.isDark) DarkSubtleBorder else LightSubtleBorder
val TextPrimary: Color get() = if (AegisPalette.isDark) DarkTextPrimary else LightTextPrimary
val TextSecondary: Color get() = if (AegisPalette.isDark) DarkTextSecondary else LightTextSecondary
val TextMuted: Color get() = if (AegisPalette.isDark) DarkTextMuted else LightTextMuted
val CyanAccent: Color get() = if (AegisPalette.isDark) DarkCyanAccent else LightCyanAccent
val CyanAccentMuted: Color get() = if (AegisPalette.isDark) DarkCyanAccentMuted else LightCyanAccentMuted
val IndigoAccent: Color get() = if (AegisPalette.isDark) DarkIndigoAccent else LightIndigoAccent
val EmeraldFocus: Color get() = if (AegisPalette.isDark) DarkEmeraldFocus else LightEmeraldFocus
val AmberWarning: Color get() = if (AegisPalette.isDark) DarkAmberWarning else LightAmberWarning
val AlertStrict: Color get() = if (AegisPalette.isDark) DarkAlertStrict else LightAlertStrict
val AlertStrictDark: Color get() = if (AegisPalette.isDark) DarkAlertStrictDark else LightAlertStrictDark
val CrimsonDanger: Color get() = if (AegisPalette.isDark) DarkCrimsonDanger else LightCrimsonDanger
val CardGlow: Color get() = if (AegisPalette.isDark) DarkCardGlow else LightCardGlow
val StrictCardGlow: Color get() = if (AegisPalette.isDark) DarkStrictCardGlow else LightStrictCardGlow

// Light-theme names kept for the Material light colour scheme
val LightBg = LightObsidianBg
val LightSurface = LightDeepSurface
val LightBorder = LightSubtleBorder
