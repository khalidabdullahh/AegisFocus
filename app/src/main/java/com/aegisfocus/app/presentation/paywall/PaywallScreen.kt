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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.aegisfocus.app.core.common.ContactLinks
import com.aegisfocus.app.core.components.AegisOutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
    onNavigateToAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showContactDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

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
                    text = "No ads. 100% On-Device Privacy. Instant Activation.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Redeem License Key Section (Direct Activation)
        Card(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DeepSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Redeem License Code",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Enter the license key you received from the seller. Each key works only once.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.licenseKeyInput,
                    onValueChange = { viewModel.onLicenseKeyChange(it.uppercase()) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("AEGIS-XXXX-XXXX-XXXX-XXXX", color = TextMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let {
                                viewModel.onLicenseKeyChange(it.trim().uppercase())
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = CyanAccent)
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SubtleBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyanAccent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.redeemLicenseKey()
                        }
                    )
                )

                if (uiState.redeemErrorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${uiState.redeemErrorMessage}",
                        color = AlertStrict,
                        fontSize = 12.sp
                    )
                }

                if (uiState.redeemSuccessMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${uiState.redeemSuccessMessage}",
                        color = EmeraldFocus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (!uiState.isSignedIn) {
                    Text(
                        text = "Sign in first so the key is tied to your account.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    AegisPrimaryButton(
                        text = "Sign in to redeem",
                        onClick = onNavigateToAuth,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (uiState.userEmail != null) {
                        Text(
                            text = "Signed in as ${uiState.userEmail}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    AegisPrimaryButton(
                        text = if (uiState.isRedeeming) "Activating..." else "Activate Pro Key",
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.redeemLicenseKey()
                        },
                        enabled = uiState.licenseKeyInput.isNotBlank() && !uiState.isRedeeming,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Subscription Plans
        Text(
            text = "Subscription Packages",
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

        // Feature comparison table
        FeatureComparisonTable()

        Spacer(Modifier.height(20.dp))

        // How to buy license guide
        Card(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DeepSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "How to get a license key",
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "1. Choose a plan above.\n2. Tap Get your key and message the owner on WhatsApp, Facebook or Instagram.\n3. Pay as agreed and receive your one-time key.\n4. Sign in, paste the key above and Pro activates instantly.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        AegisPrimaryButton(
            text = "Get your key: ${uiState.selectedOption.label}",
            onClick = { showContactDialog = true },
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Privacy note
        Text(
            text = "Keys are verified on our server and can be used only once.",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // Official Links: Download Latest APK & Website
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { ContactLinks.openDownloadPage(context) }
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Download APK",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "•",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            TextButton(
                onClick = { ContactLinks.openWebsite(context) }
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Website",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showContactDialog) {
        val option = uiState.selectedOption
        val message = "Hi, I want to buy Aegis Focus ${option.label} (${option.price})." +
            (uiState.userEmail?.let { " My account email: $it" } ?: "")
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            containerColor = DeepSurface,
            title = { Text("Get your key", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Message the owner for ${option.label} (${option.price}). You will receive a one-time key.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    AegisPrimaryButton(
                        text = "WhatsApp",
                        onClick = { ContactLinks.open(context, ContactLinks.whatsappUrl(message)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AegisOutlinedButton(
                        text = "Facebook",
                        onClick = { ContactLinks.open(context, ContactLinks.FACEBOOK_URL) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AegisOutlinedButton(
                        text = "Instagram",
                        onClick = { ContactLinks.open(context, ContactLinks.INSTAGRAM_URL) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactDialog = false }) { Text("Close", color = CyanAccent) }
            }
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
                Text(option.price, color = if (isSelected) CyanAccent else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (option == PaywallOption.MONTH_12) {
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
