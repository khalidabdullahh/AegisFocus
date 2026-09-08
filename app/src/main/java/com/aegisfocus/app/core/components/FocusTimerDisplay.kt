package com.aegisfocus.app.core.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.core.theme.AlertStrict
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.domain.model.SessionMode

@Composable
fun FocusTimerDisplay(
    remainingMillis: Long,
    totalDurationMinutes: Int,
    mode: SessionMode,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    strokeWidth: Dp = 10.dp
) {
    val totalMillis = totalDurationMinutes * 60 * 1000L
    val progress = if (totalMillis > 0) {
        (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "timer_progress"
    )

    val activeColor = if (mode == SessionMode.STRICT) AlertStrict else CyanAccent

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()

            // Background Track
            drawArc(
                color = DeepSurface,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress Arc
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = TimeUtils.formatRemainingMillis(remainingMillis),
                fontSize = if (remainingMillis >= 3600000L) 36.sp else 46.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-1.0).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (mode == SessionMode.STRICT) "STRICT MODE" else "FOCUS SESSION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = if (mode == SessionMode.STRICT) AlertStrict else TextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Remain mindful",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}
