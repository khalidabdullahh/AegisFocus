package com.aegisfocus.app.presentation.limits

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.theme.AegisFocusTheme
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary

/** Shown when a per-app daily limit (time or number of opens) has been reached. */
class LimitReachedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra(EXTRA_APP_NAME).orEmpty()
        val reason = intent.getStringExtra(EXTRA_REASON).orEmpty()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })

        setContent {
            AegisFocusTheme(darkTheme = true) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBg)
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Daily limit reached",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(reason, fontSize = 15.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$appName opens again tomorrow. Take a break, your future self will thank you.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    AegisPrimaryButton(
                        text = "Go home",
                        onClick = { goHome() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }

    companion object {
        const val EXTRA_APP_NAME = "extra_limit_app_name"
        const val EXTRA_REASON = "extra_limit_reason"
    }
}
