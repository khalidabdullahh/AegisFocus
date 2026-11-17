package com.aegisfocus.app.domain.blocker

/**
 * Strict Mode used to block the whole Android Settings app, which also took away Wi-Fi,
 * Bluetooth, display and sound. Now Settings stays usable and only the screens that could be
 * used to switch Aegis Focus off are closed: any Settings screen that shows the app's own name
 * (Accessibility services, Device admin apps, App info, the Apps list, Special app access).
 *
 * Pure Kotlin so it can be unit tested.
 */
object SettingsTamperGuard {

    const val SETTINGS_PACKAGE = "com.android.settings"

    private val APP_NAMES = listOf("aegis focus", "aegisfocus")

    fun isTamperScreen(texts: Collection<String>): Boolean =
        texts.any { text ->
            val lower = text.lowercase()
            APP_NAMES.any { lower.contains(it) }
        }
}
