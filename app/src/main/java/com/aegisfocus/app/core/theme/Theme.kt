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
const val TEXT_SCALE = 0.93f

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = ObsidianBg,
    primaryContainer = CyanAccentMuted,
    onPrimaryContainer = TextPrimary,
    secondary = IndigoAccent,
    onSecondary = TextPrimary,
    tertiary = EmeraldFocus,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = DeepSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondary,
    outline = SubtleBorder,
    error = AlertStrict,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = CyanAccentMuted,
    onPrimary = LightSurface,
    secondary = IndigoAccent,
    onSecondary = LightSurface,
    tertiary = EmeraldFocus,
    onTertiary = LightSurface,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevatedSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = AlertStrict,
    onError = LightSurface
)

@Composable
fun AegisFocusTheme(
    darkTheme: Boolean = true, // Dark mode is primary experience
    content: @Composable () -> Unit
) {
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
