/*
 * ==============================================================================
 * 📌 প্রজেক্ট: ASR / Aegis-Focus
 * 📄 ফাইল: AegisNavHost.kt
 * 🎯 উদ্দেশ্য: জেটপ্যাক কম্পোজ (Jetpack Compose) নেভিগেশন গ্রাফ ও বটম নেভিগেশন বার।
 * 💡 ভূমিকা: হোম (Home), ফোকাস (Focus), অ্যাপস ব্লকার (Apps), স্ট্যাটস (Stats) ও সেটিংসের 
 *            মধ্যে স্ক্রিন ট্রানজিশন এবং রুট হ্যান্ডলিং নিয়ন্ত্রণ করে।
 * ==============================================================================
 */
package com.aegisfocus.app.core.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aegisfocus.app.core.di.AppContainer
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.presentation.apps.AppsScreen
import com.aegisfocus.app.presentation.apps.AppsViewModel
import com.aegisfocus.app.presentation.focus.FocusScreen
import com.aegisfocus.app.presentation.focus.FocusViewModel
import com.aegisfocus.app.presentation.home.HomeScreen
import com.aegisfocus.app.presentation.home.HomeViewModel
import com.aegisfocus.app.presentation.onboarding.OnboardingScreen
import com.aegisfocus.app.presentation.onboarding.OnboardingViewModel
import com.aegisfocus.app.presentation.settings.SettingsScreen
import com.aegisfocus.app.presentation.settings.SettingsViewModel
import com.aegisfocus.app.presentation.stats.StatsScreen
import com.aegisfocus.app.presentation.stats.StatsViewModel
import com.aegisfocus.app.presentation.paywall.PaywallScreen
import com.aegisfocus.app.presentation.paywall.PaywallViewModel

/**
 * -----------------------------------------------------------------------------
 * 📌 কম্পোজেবল: AegisNavHost
 * 🎯 উদ্দেশ্য: অ্যাপ্লিকেশনের মূল ফ্রেম এবং সমস্ত স্ক্রিনের নেভিগেশন রাউটিং ডিফাইন করে।
 * -----------------------------------------------------------------------------
 */
@Composable
fun AegisNavHost(
    navController: NavHostController,
    container: AppContainer,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    // বর্তমান একটিভ রুটের পাথ ট্র্যাক করা
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // অনবোর্ডিং বা পে-ওয়াল স্ক্রিনে বটম বার হাইড রাখা
    val showBottomBar = currentRoute != Screen.Onboarding.route &&
            currentRoute != Screen.Paywall.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = DeepSurface,
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    NavigationBar(
                        containerColor = DeepSurface,
                        tonalElevation = 0.dp
                    ) {
                        BottomNavScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanAccent,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = DeepSurface,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel {
                    OnboardingViewModel(container.settingsRepository)
                }
                OnboardingScreen(
                    viewModel = vm,
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel {
                    HomeViewModel(
                        focusSessionManager = container.focusSessionManager,
                        usageStatsRepository = container.usageStatsRepository,
                        disciplineScoreCalculator = container.disciplineScoreCalculator,
                        xpManager = container.xpManager,
                        entitlementManager = container.entitlementManager
                    )
                }
                HomeScreen(
                    viewModel = vm,
                    onNavigateToFocus = {
                        navController.navigate(Screen.Focus.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    }
                )
            }

            composable(Screen.Focus.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel {
                    FocusViewModel(
                        focusSessionManager = container.focusSessionManager,
                        profileRepository = container.profileRepository,
                        appDiscoveryRepository = container.appDiscoveryRepository,
                        settingsRepository = container.settingsRepository
                    )
                }
                FocusScreen(
                    viewModel = vm,
                    onNavigateToApps = {
                        navController.navigate(Screen.Apps.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Apps.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel {
                    AppsViewModel(
                        appDiscoveryRepository = container.appDiscoveryRepository,
                        focusSessionManager = container.focusSessionManager
                    )
                }
                AppsScreen(viewModel = vm)
            }

            composable(Screen.Stats.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel {
                    StatsViewModel(
                        getUsageAnalyticsUseCase = container.getUsageAnalyticsUseCase,
                        disciplineScoreCalculator = container.disciplineScoreCalculator,
                        xpManager = container.xpManager
                    )
                }
                StatsScreen(viewModel = vm)
            }

            composable(Screen.Settings.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel {
                    SettingsViewModel(
                        settingsRepository = container.settingsRepository,
                        scheduleRepository = container.scheduleRepository
                    )
                }
                SettingsScreen(
                    viewModel = vm,
                    onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                    onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) }
                )
            }

            composable(Screen.Paywall.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel {
                    PaywallViewModel(
                        entitlementManager = container.entitlementManager,
                        playBillingManager = container.playBillingManager
                    )
                }
                PaywallScreen(
                    viewModel = vm,
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.Auth.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel {
                    com.aegisfocus.app.presentation.auth.AuthViewModel(
                        authService = container.supabaseAuthService
                    )
                }
                com.aegisfocus.app.presentation.auth.AuthScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
