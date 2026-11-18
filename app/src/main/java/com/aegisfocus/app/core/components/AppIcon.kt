package com.aegisfocus.app.core.components

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val iconCache = LruCache<String, ImageBitmap>(96)

/** The real launcher icon of an installed app, with a letter circle as a fallback. */
@Composable
fun AppIcon(packageName: String, appName: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState<ImageBitmap?>(initialValue = iconCache.get(packageName), packageName, px) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val bitmap = context.packageManager.getApplicationIcon(packageName)
                        .toBitmap(px, px)
                        .asImageBitmap()
                    iconCache.put(packageName, bitmap)
                    bitmap
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    val image = icon
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.24f))
        )
    } else {
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(CyanAccent.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.take(1).uppercase(),
                fontSize = (size.value * 0.45f).sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}
