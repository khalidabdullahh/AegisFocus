package com.aegisfocus.app.core.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { SYSTEM, DARK, LIGHT }

/** The user's choice of Dark, Light or follow-the-phone theme, saved on the device. */
object AegisThemeState {
    private const val PREFS = "aegis_theme"
    private const val KEY_MODE = "mode"

    var mode by mutableStateOf(ThemeMode.DARK)
        private set

    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE, null)
        mode = ThemeMode.values().firstOrNull { it.name == saved } ?: ThemeMode.DARK
    }

    fun set(context: Context, newMode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MODE, newMode.name).apply()
        mode = newMode
    }
}

@Composable
fun rememberAegisDarkTheme(): Boolean {
    val system = isSystemInDarkTheme()
    return when (AegisThemeState.mode) {
        ThemeMode.SYSTEM -> system
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
}
