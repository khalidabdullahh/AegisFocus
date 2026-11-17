package com.aegisfocus.app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

/** Overall layout scale (1.0 = system default). */
const val UI_SCALE = 0.88f

/** Overall text scale relative to system default. */
const val TEXT_SCALE = 1.0f

private val DarkColorScheme = darkColorScheme(
    primary = DarkCyanAccent,
    onPrimary = DarkObsidianBg,
    primaryContainer = DarkCyanAccentMuted,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkIndigoAccent,
    onSecondary = DarkTextPrimary,
    tertiary = DarkEmeraldFocus,
    onTertiary = DarkObsidianBg,
    background = DarkObsidianBg,
    onBackground = DarkTextPrimary,
    surface = DarkDeepSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkElevatedSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSubtleBorder,
    error = DarkAlertStrict,
    onError = DarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightCyanAccent,
    onPrimary = LightDeepSurface,
    secondary = LightIndigoAccent,
    onSecondary = LightDeepSurface,
    tertiary = LightEmeraldFocus,
    onTertiary = LightDeepSurface,
    background = LightObsidianBg,
    onBackground = LightTextPrimary,
    surface = LightDeepSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevatedSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightSubtleBorder,
    error = LightAlertStrict,
    onError = LightDeepSurface
)

@Composable
fun AegisFocusTheme(
    darkTheme: Boolean = rememberAegisDarkTheme(),
    content: @Composable () -> Unit
) {
    // Every colour name in the app reads this flag, so set it before anything is drawn
    if (AegisPalette.isDark != darkTheme) AegisPalette.isDark = darkTheme
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    // Central compact scale: one place to tune how large the whole UI feels.
    // dp values shrink by UI_SCALE, text shrinks slightly less so it stays readable.
    val base = LocalDensity.current
    val compactDensity = Density(
        density = base.density * UI_SCALE,
        fontScale = base.fontScale * (TEXT_SCALE / UI_SCALE)
    )

    CompositionLocalProvider(LocalDensity provides compactDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AegisTypography,
            shapes = AegisShapes,
            content = content
        )
    }
}
