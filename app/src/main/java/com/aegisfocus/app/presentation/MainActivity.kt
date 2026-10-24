package com.aegisfocus.app.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.aegisfocus.app.data.remote.AuthResult
import kotlinx.coroutines.launch
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
        val currentPermissions = container.settingsRepository.refreshPermissionState()
        val hasEssentialPermissions = currentPermissions.hasAccessibilityService && 
                                      currentPermissions.hasUsageAccess && 
                                      currentPermissions.hasNotificationPermission

        val startDestination = if (isOnboardingDone && hasEssentialPermissions) {
            Screen.Home.route
        } else {
            Screen.Onboarding.route
        }

        handleAuthIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    /** Completes Google sign-in and email confirmation, which come back as aegisfocus://auth-callback. */
    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "aegisfocus" || uri.host != "auth-callback") return
        intent.data = null
        val authService = (application as AegisApplication).container.supabaseAuthService
        lifecycleScope.launch {
            when (val result = authService.handleOAuthCallback(uri)) {
                is AuthResult.Success -> Toast.makeText(this@MainActivity, "Signed in as ${result.user.email}", Toast.LENGTH_LONG).show()
                is AuthResult.Error -> Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as AegisApplication
        app.container.settingsRepository.refreshPermissionState()
    }
}
