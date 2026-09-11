package com.aegisfocus.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.aegisfocus.app.AegisApplication
import com.aegisfocus.app.core.navigation.AegisNavHost
import com.aegisfocus.app.core.navigation.Screen
import com.aegisfocus.app.core.theme.AegisFocusTheme
import com.aegisfocus.app.core.theme.ObsidianBg

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as AegisApplication
        val container = app.container

        val isOnboardingDone = container.settingsRepository.isOnboardingCompleted()
        val startDestination = if (isOnboardingDone) Screen.Home.route else Screen.Onboarding.route

        setContent {
            AegisFocusTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBg)
                ) {
                    val navController = rememberNavController()
                    AegisNavHost(
                        navController = navController,
                        container = container,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
