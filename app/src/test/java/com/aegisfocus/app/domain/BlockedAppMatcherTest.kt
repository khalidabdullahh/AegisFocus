package com.aegisfocus.app.domain

import com.aegisfocus.app.engine.BlockedAppMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockedAppMatcherTest {

    private lateinit var matcher: BlockedAppMatcher
    private val myPackage = "com.aegisfocus.app"

    @Before
    fun setup() {
        matcher = BlockedAppMatcher()
    }

    @Test
    fun matcher_returnsTrue_forBlockedApps() {
        matcher.updateBlockedPackages(listOf("com.instagram.android", "com.google.android.youtube"))

        assertTrue(matcher.isAppBlocked("com.instagram.android", myPackage))
        assertTrue(matcher.isAppBlocked("com.google.android.youtube", myPackage))
        assertFalse(matcher.isAppBlocked("com.slack", myPackage))
    }

    @Test
    fun matcher_neverBlocksSelfOrCriticalSystemPhonePackages() {
        matcher.updateBlockedPackages(
            listOf(
                "com.aegisfocus.app",
                "com.android.dialer",
                "com.google.android.dialer",
                "com.android.emergency",
                "com.android.systemui"
            )
        )

        assertFalse(matcher.isAppBlocked("com.aegisfocus.app", myPackage))
        assertFalse(matcher.isAppBlocked("com.android.dialer", myPackage))
        assertFalse(matcher.isAppBlocked("com.android.emergency", myPackage))
        assertFalse(matcher.isAppBlocked("com.android.systemui", myPackage))
    }

    @Test
    fun matcher_guardsSettingsAndInstaller_whenStrictModeActive() {
        // Normal mode (isStrict = false) -> settings not blocked
        matcher.updateBlockedPackages(listOf("com.instagram.android"), isStrict = false)
        assertFalse(matcher.isAppBlocked("com.android.settings", myPackage))
        assertFalse(matcher.isAppBlocked("com.google.android.packageinstaller", myPackage))

        // Strict mode (isStrict = true) -> settings & installer guarded
        matcher.updateBlockedPackages(listOf("com.instagram.android"), isStrict = true)
        assertTrue(matcher.isAppBlocked("com.android.settings", myPackage))
        assertTrue(matcher.isAppBlocked("com.google.android.packageinstaller", myPackage))
        assertTrue(matcher.isStrictGuarded())
    }

    @Test
    fun matcher_clearsProperly() {
        matcher.updateBlockedPackages(listOf("com.instagram.android"), isStrict = true)
        assertTrue(matcher.isAppBlocked("com.instagram.android", myPackage))
        assertTrue(matcher.isAppBlocked("com.android.settings", myPackage))

        matcher.clear()
        assertFalse(matcher.isAppBlocked("com.instagram.android", myPackage))
        assertFalse(matcher.isAppBlocked("com.android.settings", myPackage))
        assertFalse(matcher.isStrictGuarded())
    }
}
