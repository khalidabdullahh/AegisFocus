package com.aegisfocus.app.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.theme.AegisTitleFamily
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary

/** Big screen title in Cabin Sketch with a small hand-drawn wavy underline. */
@Composable
fun AegisScreenTitle(text: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier = modifier) {
        Text(
            text = text,
            style = TextStyle(fontFamily = AegisTitleFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp),
            color = TextPrimary
        )
        WavyUnderline(color = CyanAccent)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, fontSize = 13.sp, color = TextSecondary)
        }
    }
}

/** Section heading in Cabin Sketch (18sp and up, so the sketch letters stay readable). */
@Composable
fun AegisSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(fontFamily = AegisTitleFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        color = TextPrimary
    )
}

@Composable
private fun WavyUnderline(color: Color) {
    Canvas(modifier = Modifier.padding(top = 2.dp).width(92.dp).height(9.dp)) {
        val waves = 6
        val step = size.width / waves
        val mid = size.height / 2f
        val path = Path().apply {
            moveTo(0f, mid)
            var x = 0f
            for (i in 0 until waves) {
                quadraticBezierTo(x + step / 2f, if (i % 2 == 0) 0f else size.height, x + step, mid)
                x += step
            }
        }
        drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** The standard rounded card used by every screen. */
@Composable
fun AegisCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val base = modifier.fillMaxWidth()
    Surface(
        modifier = if (onClick != null) base.clickable { onClick() } else base,
        shape = shape,
        color = DeepSurface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/** One line of a settings list: icon, title, short description and a switch/arrow on the right. */
@Composable
fun AegisSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val base = modifier.fillMaxWidth()
    Row(
        modifier = (if (onClick != null) base.clickable { onClick() } else base).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = TextMuted)
            }
        }
        if (trailing != null) trailing()
    }
}
