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

    private val antiTamperProtectedPackages = setOf(
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    fun updateBlockedPackages(packages: Collection<String>, isStrict: Boolean = false) {
        blockedPackages.clear()
        blockedPackages.addAll(packages)
        isStrictModeActive = isStrict
    }

    fun isAppBlocked(packageName: String, myPackageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (packageName == myPackageName) return false
        if (systemWhitelistedPackages.contains(packageName)) return false

        // During Strict Mode, guard system settings and package installer from tampering
        if (isStrictModeActive && antiTamperProtectedPackages.contains(packageName)) {
            return true
        }

        return blockedPackages.contains(packageName)
    }

    fun getBlockedPackages(): Set<String> = blockedPackages.toSet()

    fun isStrictGuarded(): Boolean = isStrictModeActive

    fun clear() {
        blockedPackages.clear()
        isStrictModeActive = false
    }
}
