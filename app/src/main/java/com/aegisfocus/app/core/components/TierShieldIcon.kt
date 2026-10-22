package com.aegisfocus.app.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aegisfocus.app.domain.motivation.ShieldProgress

/**
 * A shield whose colour is the current tier (Bronze, Silver, Gold, Platinum, Diamond)
 * and whose ring shows progress towards the next tier.
 */
@Composable
fun TierShieldIcon(
    progress: ShieldProgress,
    selected: Boolean,
    modifier: Modifier = Modifier,
    ringSize: Dp = 28.dp
) {
    val color = Color(progress.tier.accentHex)

    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.5.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = color.copy(alpha = 0.22f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            // Always show a small sliver so a fresh shield still reads as a ring
            val sweep = 360f * progress.fraction.coerceIn(0.04f, 1f)
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Icon(
            imageVector = if (selected) Icons.Filled.Shield else Icons.Outlined.Shield,
            contentDescription = "${progress.tier.title} shield",
            tint = color,
            modifier = Modifier.size(ringSize * 0.55f)
        )
    }
}
