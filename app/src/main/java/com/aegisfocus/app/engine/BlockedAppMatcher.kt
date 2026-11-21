package com.aegisfocus.app.engine

import java.util.concurrent.CopyOnWriteArraySet

class BlockedAppMatcher {

    private val blockedPackages = CopyOnWriteArraySet<String>()
    private var isStrictModeActive = false

    private val systemWhitelistedPackages = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.server.telecom",
        "com.android.phone",
        "com.android.emergency"
    )

    // Android Settings and the package installer are not blocked as whole apps during Strict Mode:
    // SettingsTamperGuard closes only the screens that could switch Aegis Focus off.

    fun updateBlockedPackages(packages: Collection<String>, isStrict: Boolean = false) {
        blockedPackages.clear()
        blockedPackages.addAll(packages)
        isStrictModeActive = isStrict
    }

    fun isAppBlocked(packageName: String, myPackageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (packageName == myPackageName) return false
        if (systemWhitelistedPackages.contains(packageName)) return false

        return blockedPackages.contains(packageName)
    }

    fun getBlockedPackages(): Set<String> = blockedPackages.toSet()

    fun isStrictGuarded(): Boolean = isStrictModeActive

    fun clear() {
        blockedPackages.clear()
        isStrictModeActive = false
    }
}
