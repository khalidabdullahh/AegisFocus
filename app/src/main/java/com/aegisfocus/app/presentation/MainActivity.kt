package com.aegisfocus.app.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.aegisfocus.app.AegisApplication
import com.aegisfocus.app.core.navigation.AegisNavHost
import com.aegisfocus.app.core.navigation.Screen
import com.aegisfocus.app.core.theme.AegisFocusTheme
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.data.remote.AuthResult
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as AegisApplication
        val container = app.container

        handleIncomingAuthIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingAuthIntent(intent)
    }

    private fun handleIncomingAuthIntent(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null && uri.scheme == "aegisfocus" && uri.host == "auth-callback") {
            Log.d(tag, "Intercepted OAuth deep link: $uri")
            val app = application as AegisApplication
            val authService = app.container.supabaseAuthService

            lifecycleScope.launch {
                val result = authService.handleOAuthCallback(uri)
                when (result) {
                    is AuthResult.Success -> {
                        Log.i(tag, "OAuth authentication succeeded for user: ${result.user.email}")
                    }
                    is AuthResult.Error -> {
                        Log.e(tag, "OAuth authentication failed: ${result.message}")
                    }
                    is AuthResult.EmailVerificationRequired -> {
                        Log.i(tag, "Email verification required: ${result.email}")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as AegisApplication
        app.container.settingsRepository.refreshPermissionState()
    }
}
