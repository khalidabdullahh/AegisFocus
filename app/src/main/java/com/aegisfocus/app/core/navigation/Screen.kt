package com.aegisfocus.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield
    )

    data object Focus : Screen(
        route = "focus",
        title = "Focus",
        selectedIcon = Icons.Filled.HourglassTop,
        unselectedIcon = Icons.Outlined.HourglassTop
    )

    data object Apps : Screen(
        route = "apps",
        title = "Apps",
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps
    )

    data object Stats : Screen(
        route = "stats",
        title = "Stats",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object Onboarding : Screen(
        route = "onboarding",
        title = "Onboarding",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    )

    object Paywall : Screen("paywall", "Paywall", Icons.Filled.Security, Icons.Outlined.Security)
    object Auth : Screen("auth", "Account", Icons.Filled.Security, Icons.Outlined.Security)
    object AdminPayment : Screen("admin_payment", "Admin Payments", Icons.Filled.Security, Icons.Outlined.Security)
}

val BottomNavScreens = listOf(
    Screen.Home,
    Screen.Focus,
    Screen.Apps,
    Screen.Stats,
    Screen.Settings
)
