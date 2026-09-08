package com.aegisfocus.app.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.core.theme.CyanAccent
import com.aegisfocus.app.core.theme.DeepSurface
import com.aegisfocus.app.core.theme.ElevatedSurface
import com.aegisfocus.app.core.theme.SubtleBorder
import com.aegisfocus.app.core.theme.TextMuted
import com.aegisfocus.app.core.theme.TextPrimary
import com.aegisfocus.app.core.theme.TextSecondary
import com.aegisfocus.app.domain.model.AppInfo

@Composable
fun AppSelectionRow(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked) { onToggle(!isSelected) },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) ElevatedSurface else DeepSurface,
        border = BorderStroke(1.dp, if (isSelected) CyanAccent.copy(alpha = 0.5f) else SubtleBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // App Icon Placeholder
                Surface(
                    shape = CircleShape,
                    color = ElevatedSurface,
                    modifier = Modifier.size(40.dp),
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = if (isSelected) CyanAccent else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = app.appName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.category.displayName,
                            fontSize = 11.sp,
                            color = CyanAccent
                        )
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = app.packageName.substringAfterLast("."),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = if (isLocked) null else onToggle,
                enabled = !isLocked,
                colors = CheckboxDefaults.colors(
                    checkedColor = CyanAccent,
                    uncheckedColor = TextMuted,
                    checkmarkColor = DeepSurface
                )
            )
        }
    }
}
