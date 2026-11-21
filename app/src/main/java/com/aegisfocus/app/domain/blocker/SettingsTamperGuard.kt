package com.aegisfocus.app.domain.blocker

/**
 * What Strict Mode protects, and nothing more. Android Settings and the package installer stay
 * fully usable (Wi-Fi, display, sound, installing and updating apps, uninstalling other apps).
 * Only screens that could be used to switch Aegis Focus itself off are closed:
 *  - Settings screens for Aegis Focus that offer Force stop, Uninstall, Clear data/storage,
 *    the Accessibility service switch, or Device admin deactivation
 *  - the uninstall dialog for Aegis Focus
 *
 * Pure Kotlin so it can be unit tested.
 */
object SettingsTamperGuard {

    const val SETTINGS_PACKAGE = "com.android.settings"

    private val APP_NAMES = listOf("aegis focus", "aegisfocus")

    /** Words that only appear on a screen where the app can actually be switched off or wiped. */
    private val DANGEROUS_ACTIONS = listOf(
        "force stop", "force-stop",
        "uninstall",
        "clear data", "clear storage", "manage storage",
        "storage & cache", "storage and cache",
        "deactivate",
        "use aegis focus", "use aegisfocus",
        "stop aegis focus", "turn off aegis focus"
    )

    fun mentionsApp(texts: Collection<String>): Boolean =
        texts.any { text ->
            val lower = text.lowercase()
            APP_NAMES.any { lower.contains(it) }
        }

    /**
     * True for a Settings screen that could switch Aegis Focus off or wipe it. The action words above
     * are English, so on a phone set to another language we cannot read them and stay strict instead:
     * any screen that shows the app's name is closed.
     */
    fun isTamperScreen(
        texts: Collection<String>,
        englishUi: Boolean = true,
        serviceDescription: String? = null
    ): Boolean {
        // The Accessibility settings page for our service always shows the service description we
        // wrote, in every phone brand and language, so it is the most reliable way to spot that page.
        if (showsServiceDescription(texts, serviceDescription)) return true
        if (!mentionsApp(texts)) return false
        if (!englishUi) return true
        return texts.any { text ->
            val lower = text.lowercase()
            DANGEROUS_ACTIONS.any { lower.contains(it) }
        }
    }

    /** True when the screen shows our accessibility service description (the service's own settings page). */
    fun showsServiceDescription(texts: Collection<String>, serviceDescription: String?): Boolean {
        val marker = serviceDescription?.trim()?.take(DESCRIPTION_MARKER_LENGTH)?.lowercase()
        if (marker.isNullOrBlank()) return false
        return texts.any { it.lowercase().contains(marker) }
    }

    private const val DESCRIPTION_MARKER_LENGTH = 30

    /** Any package installer (Google, AOSP, MIUI, Samsung and others share this name). */
    fun isInstallerPackage(packageName: String): Boolean = packageName.contains("packageinstaller")

    /**
     * True for the dialog that would uninstall Aegis Focus. Other apps' uninstall dialogs, and
     * install / update screens, are left alone. The class name check works in any language.
     */
    fun isAegisUninstallScreen(className: String?, texts: Collection<String>): Boolean {
        if (!mentionsApp(texts)) return false
        val byClass = className?.lowercase()?.contains("uninstall") == true
        val byText = texts.any { it.lowercase().contains("uninstall") }
        return byClass || byText
    }
}
