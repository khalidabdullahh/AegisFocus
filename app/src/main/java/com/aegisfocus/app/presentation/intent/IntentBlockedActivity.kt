package com.aegisfocus.app.presentation.intent

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisOutlinedButton
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.core.theme.AegisFocusTheme
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary

/** Shown when a YouTube video does not match the user's own "what I want to watch" list. */
class IntentBlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val reason = intent.getStringExtra(EXTRA_REASON).orEmpty()
        val store = ServiceLocator.getContainer(applicationContext).intentStore

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })

        setContent {
            AegisFocusTheme(darkTheme = true) {
                var asking by remember { mutableStateOf(false) }
                var why by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBg)
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Not on your list",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(title, fontSize = 15.sp, color = CyanAccent, textAlign = TextAlign.Center, maxLines = 3)
                    Spacer(Modifier.height(8.dp))
                    Text(reason, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "You said you want to watch trading, investing, CSE, English and business. Is this really it?",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))

                    AegisPrimaryButton(
                        text = "Leave YouTube",
                        onClick = { goHome() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    if (!asking) {
                        AegisOutlinedButton(
                            text = "This one is useful",
                            onClick = { asking = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = why,
                            onValueChange = { why = it.take(120) },
                            label = { Text("Why is it useful? (10+ letters)", color = TextSecondary, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        AegisOutlinedButton(
                            text = "Allow this video",
                            onClick = {
                                if (why.trim().length >= 10) {
                                    store.allowOnce(title, why.trim())
                                    finish()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
        const val EXTRA_TITLE = "extra_intent_title"
        const val EXTRA_REASON = "extra_intent_reason"
    }
}
