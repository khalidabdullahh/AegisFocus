package com.aegisfocus.app.domain

import com.aegisfocus.app.domain.blocker.SettingsTamperGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTamperGuardTest {

    @Test
    fun guard_closesScreensThatShowTheAppName() {
        assertTrue(SettingsTamperGuard.isTamperScreen(listOf("Accessibility", "Aegis Focus", "Off")))
        assertTrue(SettingsTamperGuard.isTamperScreen(listOf("App info", "AEGIS FOCUS")))
        assertTrue(SettingsTamperGuard.isTamperScreen(listOf("com.aegisfocus.app")))
        assertTrue(SettingsTamperGuard.isTamperScreen(listOf("Uninstall Aegis Focus?")))
    }

    @Test
    fun guard_leavesEverydaySettingsScreensAlone() {
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("Wi-Fi", "Use Wi-Fi", "Saved networks")))
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("Bluetooth", "Pair new device")))
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("Display", "Brightness level", "Dark theme")))
        assertFalse(
            SettingsTamperGuard.isTamperScreen(
                listOf("Settings", "Network & internet", "Connected devices", "Apps", "Accessibility")
            )
        )
        assertFalse(SettingsTamperGuard.isTamperScreen(emptyList()))
    }
}
