package com.aegisfocus.app.engine

import java.util.concurrent.CopyOnWriteArraySet

class BlockedAppMatcher {

    private val blockedPackages = CopyOnWriteArraySet<String>()
    private val systemWhitelistedPackages = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.server.telecom",
        "com.android.phone",
        "com.android.emergency"
    )

    fun updateBlockedPackages(packages: Collection<String>) {
        blockedPackages.clear()
        blockedPackages.addAll(packages)
    }

    fun isAppBlocked(packageName: String, myPackageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (packageName == myPackageName) return false
        if (systemWhitelistedPackages.contains(packageName)) return false
        return blockedPackages.contains(packageName)
    }

    fun getBlockedPackages(): Set<String> = blockedPackages.toSet()

    fun clear() {
        blockedPackages.clear()
    }
}
