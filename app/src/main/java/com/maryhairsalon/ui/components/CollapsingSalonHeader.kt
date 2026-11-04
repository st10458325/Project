package com.maryhairsalon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Same collapse math BookingsScreen already used — pulled out so every tab collapses identically. */
@Composable
fun rememberHeaderCollapse(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    maxHeight: Dp = 200.dp,
    minHeight: Dp = 80.dp
): Pair<Dp, Float> {
    val maxPx = with(LocalDensity.current) { maxHeight.toPx() }
    val minPx = with(LocalDensity.current) { minHeight.toPx() }

    val heightPx by remember(firstVisibleItemIndex, firstVisibleItemScrollOffset) {
        derivedStateOf {
            if (firstVisibleItemIndex == 0) (maxPx - firstVisibleItemScrollOffset).coerceAtLeast(minPx)
            else minPx
        }
    }
    val progress = ((heightPx - minPx) / (maxPx - minPx)).coerceIn(0f, 1f)
    val heightDp = with(LocalDensity.current) { heightPx.toDp() }
    return heightDp to progress
}

/** Theme-aware collapsing header used by Services, Bookings and About alike. */
@Composable
fun CollapsingSalonHeader(
    height: Dp,
    progress: Float,
    title: String,
    eyebrow: String? = null,
    subtitle: String? = null
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.18f), colors.background))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = progress.coerceAtLeast(0.3f)
                scaleX = 0.8f + (progress * 0.2f)
                scaleY = 0.8f + (progress * 0.2f)
            }
        ) {
            if (eyebrow != null && progress > 0.5f) {
                Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = colors.primary, fontWeight = FontWeight.Bold)
            }
            Text(
                title,
                style = lerp(MaterialTheme.typography.titleLarge, MaterialTheme.typography.displayLarge, progress),
                color = colors.onBackground
            )
            if (subtitle != null && progress > 0.8f) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.tertiary)
            }
        }
    }
}