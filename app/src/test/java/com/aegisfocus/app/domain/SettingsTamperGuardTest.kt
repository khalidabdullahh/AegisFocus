package com.aegisfocus.app.domain

import com.aegisfocus.app.domain.blocker.SettingsTamperGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTamperGuardTest {

    @Test
    fun guard_closesTheScreensThatCanSwitchTheAppOffOrWipeIt() {
        // App info page: Force stop / Uninstall
        assertTrue(SettingsTamperGuard.isTamperScreen(listOf("App info", "Aegis Focus", "Force stop", "Uninstall")))
        // Storage page: Clear data
        assertTrue(SettingsTamperGuard.isTamperScreen(listOf("Aegis Focus", "Storage & cache", "Clear storage", "Clear cache")))
        // Accessibility service page
        assertTrue(SettingsTamperGuard.isTamperScreen(listOf("Aegis Focus", "Use Aegis Focus")))
        assertTrue(SettingsTamperGuard.isTamperScreen(listOf("Stop Aegis Focus?", "OK", "Cancel")))
        // Device admin page
        assertTrue(SettingsTamperGuard.isTamperScreen(listOf("Aegis Focus", "Deactivate this device admin app")))
    }

    @Test
    fun guard_letsYouBrowseListsThatOnlyMentionTheApp() {
        // Apps list, Accessibility list, search results and notification settings all just show the name
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("Apps", "Chrome", "Aegis Focus", "Instagram")))
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("Accessibility", "Installed apps", "Aegis Focus", "Off")))
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("Battery", "Aegis Focus", "Unrestricted")))
    }

    @Test
    fun guard_leavesEverydaySettingsScreensAlone() {
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("Wi-Fi", "Use Wi-Fi", "Saved networks")))
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("Bluetooth", "Pair new device")))
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("Display", "Brightness level", "Dark theme")))
        // Other apps' info pages are fine, only Aegis Focus is protected
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("App info", "Chrome", "Force stop", "Uninstall")))
        assertFalse(SettingsTamperGuard.isTamperScreen(emptyList()))
    }

    @Test
    fun guard_staysStrictOnPhonesNotSetToEnglish() {
        // We cannot read the action words, so any screen showing the app name is closed
        assertTrue(SettingsTamperGuard.isTamperScreen(listOf("Aegis Focus", "Zwangsstopp"), englishUi = false))
        assertFalse(SettingsTamperGuard.isTamperScreen(listOf("WLAN", "Netzwerk"), englishUi = false))
    }

    @Test
    fun installer_installsAndUpdatesStayFree() {
        assertFalse(SettingsTamperGuard.isAegisUninstallScreen("com.android.packageinstaller.InstallStart", listOf("Aegis Focus", "Do you want to install an update to this existing app?", "Update")))
        assertFalse(SettingsTamperGuard.isAegisUninstallScreen(null, listOf("Some New App", "Install")))
    }

    @Test
    fun installer_closesOnlyTheAegisFocusUninstallDialog() {
        assertTrue(SettingsTamperGuard.isAegisUninstallScreen("com.google.android.packageinstaller.UninstallerActivity", listOf("Aegis Focus")))
        assertTrue(SettingsTamperGuard.isAegisUninstallScreen(null, listOf("Do you want to uninstall this app?", "Aegis Focus")))
        // another app's uninstall dialog is allowed
        assertFalse(SettingsTamperGuard.isAegisUninstallScreen("com.google.android.packageinstaller.UninstallerActivity", listOf("Instagram", "Do you want to uninstall this app?")))
    }

    @Test
    fun installer_packageNamesFromDifferentPhoneMakersAreRecognised() {
        assertTrue(SettingsTamperGuard.isInstallerPackage("com.google.android.packageinstaller"))
        assertTrue(SettingsTamperGuard.isInstallerPackage("com.android.packageinstaller"))
        assertTrue(SettingsTamperGuard.isInstallerPackage("com.miui.packageinstaller"))
        assertTrue(SettingsTamperGuard.isInstallerPackage("com.samsung.android.packageinstaller"))
        assertFalse(SettingsTamperGuard.isInstallerPackage("com.android.settings"))
    }
}
