package com.aegisfocus.app.presentation.paywall

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.components.AegisPrimaryButton
import com.aegisfocus.app.core.theme.AlertStrict
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.EmeraldFocus
import com.aegisfocus.app.core.theme.ObsidianBg
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.domain.subscription.SubscriptionPlan

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    if (uiState.purchaseSucceeded && uiState.currentPlan != SubscriptionPlan.FREE) {
        PurchaseSuccessScreen(onDone = onDismiss)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upgrade to Pro",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
            }
        }

        // Hero badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1A1F3A), Color(0xFF0D1B2A))
                    )
                )
                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Star, contentDescription = null,
                    tint = CyanAccent, modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Unlock Full Discipline",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "No ads. No telemetry. Official in-app billing.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Feature comparison table
        FeatureComparisonTable()

        Spacer(Modifier.height(20.dp))

        // Plan selection
        Text(
            text = "Select In-App Plan (${uiState.storeName})",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(10.dp))

        PaywallOption.values().forEach { option ->
            PlanOptionCard(
                option = option,
                isSelected = uiState.selectedOption == option,
                onClick = { viewModel.selectOption(option) }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // CTA
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = CyanAccent)
                }
            } else {
                AegisPrimaryButton(
                    text = "Subscribe with ${uiState.selectedOption.label} — ${uiState.selectedOption.price}",
                    onClick = { viewModel.purchaseSelected(activity) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        uiState.errorMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = msg,
                color = AlertStrict,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Restore Purchases",
            color = CyanAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { viewModel.restorePurchases() }
                .padding(8.dp)
        )

        // Legal footer
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Purchases are securely processed by ${uiState.storeName}. Auto-renews unless cancelled 24h before renewal.",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun FeatureComparisonTable() {
    val features = listOf(
        Triple("Strict Mode Duration", "Max 2 Hours", "Unlimited"),
        Triple("Shielded Blocked Apps", "Max 3 Apps", "Unlimited"),
        Triple("Custom Focus Profiles", "Max 2 Profiles", "Unlimited"),
        Triple("Website & Domain Blocking", "Basic", "Advanced"),
        Triple("Notification Shield", "No", "Yes"),
        Triple("Adaptive 5s→30s Friction", "5s fixed", "Dynamic"),
        Triple("Automated Looping Routines", "1 Schedule", "Unlimited")
    )

    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Feature", color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("Free", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                Text("Pro", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(8.dp))
            features.forEach { (name, free, pro) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(free, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                    Text(pro, color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    option: PaywallOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) CyanAccent else SubtleBorder
    val bgColor = if (isSelected) CyanAccent.copy(alpha = 0.08f) else ElevatedSurface

    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clickable { onClick() }
            .border(BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(option.label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(option.perMonth, color = TextMuted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(option.price, color = if (isSelected) CyanAccent else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (option == PaywallOption.YEARLY) {
                    Text("BEST VALUE", color = EmeraldFocus, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PurchaseSuccessScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text("Welcome to Pro!", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Unlimited Strict Mode, unlimited app blocking, looping schedules,\nand deep analytics are now active.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))
        AegisPrimaryButton(text = "Start Focusing", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}
