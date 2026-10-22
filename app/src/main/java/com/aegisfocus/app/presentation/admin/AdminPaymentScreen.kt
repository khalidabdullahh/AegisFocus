package com.aegisfocus.app.presentation.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import com.aegisfocus.app.data.remote.PaymentRequest

@Composable
fun AdminPaymentScreen(
    viewModel: AdminPaymentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var rejectingRequest by remember { mutableStateOf<PaymentRequest?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("") }

    if (rejectingRequest != null) {
        AlertDialog(
            onDismissRequest = { rejectingRequest = null },
            title = { Text("Reject Payment Claim", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Please provide a reason for rejecting TrxID: ${rejectingRequest?.transactionId}:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        placeholder = { Text("e.g. Transaction ID not found or amount incorrect", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        rejectingRequest?.let { req ->
                            viewModel.rejectRequest(req, rejectionReasonInput)
                        }
                        rejectingRequest = null
                        rejectionReasonInput = ""
                    }
                ) {
                    Text("Confirm Rejection", color = AlertStrict, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingRequest = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DeepSurface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Admin Payment Review", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Manual bKash/Nagad Queue", color = TextMuted, fontSize = 11.sp)
                }
            }
            IconButton(onClick = { viewModel.loadPendingRequests() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanAccent)
            }
        }

        // Action Feedback / Error Banner
        uiState.actionFeedback?.let { msg ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldFocus.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, EmeraldFocus),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = msg,
                    color = EmeraldFocus,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        uiState.errorMessage?.let { err ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AlertStrict.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, AlertStrict),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = err,
                    color = AlertStrict,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanAccent)
            }
        } else if (uiState.pendingRequests.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldFocus, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Verification Queue Clear", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("No pending payment claims waiting for review.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.pendingRequests, key = { it.id }) { request ->
                    PaymentRequestAdminCard(
                        request = request,
                        onApprove = { viewModel.approveRequest(request) },
                        onReject = {
                            rejectingRequest = request
                            rejectionReasonInput = ""
                        },
                        onCopy = { text -> clipboardManager.setText(AnnotatedString(text)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentRequestAdminCard(
    request: PaymentRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onCopy: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.planRequested,
                    color = CyanAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "৳${request.amount.toInt()} BDT",
                    color = EmeraldFocus,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElevatedSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopy(request.transactionId) }
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TRANSACTION ID (TAP TO COPY):", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text(request.transactionId, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Text("Copy", fontSize = 11.sp, color = CyanAccent)
                }
            }

            Spacer(Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElevatedSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopy(request.senderNumber) }
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SENDER PHONE:", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text(request.senderNumber, fontSize = 13.sp, color = TextSecondary)
                    }
                    Text("Copy", fontSize = 11.sp, color = CyanAccent)
                }
            }

            request.createdAt?.let {
                Spacer(Modifier.height(4.dp))
                Text("Submitted: $it", fontSize = 10.sp, color = TextMuted)
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AlertStrict.copy(alpha = 0.5f))
                ) {
                    Text("Reject", color = AlertStrict, fontWeight = FontWeight.SemiBold)
                }

                AegisPrimaryButton(
                    text = "Approve & Activate",
                    onClick = onApprove,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}
