/**
 * SalonScreenChrome.kt
 *
 * Suite of reusable UI chrome components: top bars, search fields, brand headers, and gradient background helpers.
 */
package com.maryhairsalon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fixed, non-scrolling wash for the top of a screen — gradient completes within `height`, then clamps flat. Reused by any screen that wants the "gradient starts above everything" look. */
@Composable
fun rememberTopScreenGradient(height: Dp = 340.dp): Brush {
    val colors = MaterialTheme.colorScheme
    val heightPx = with(LocalDensity.current) { height.toPx() }
    return remember(colors.primary, colors.background, heightPx) {
        Brush.verticalGradient(
            0f to colors.primary.copy(alpha = 0.22f),
            1f to colors.background,
            startY = 0f,
            endY = heightPx
        )
    }
}

/** Shared fixed top bar for simple sub-views (search results, category detail) that lost Scaffold's topBar once the main view went full-bleed. Same hamburger, arbitrary leading content. */
@Composable
fun SimpleFixedTopBar(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = content)
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.onBackground)
        }
    }
}

@Composable
fun SalonBrandRow(onMenuClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("♕", fontSize = 24.sp, color = colors.primary)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Mary", style = MaterialTheme.typography.titleLarge, color = colors.onBackground, fontWeight = FontWeight.Bold)
            Text("Hair salon", style = MaterialTheme.typography.labelSmall, color = colors.tertiary, fontSize = 10.sp)
        }
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.onBackground)
        }
    }
}

@Composable
fun SalonHeroHeader(
    title: String,
    eyebrow: String? = null,
    subtitle: String? = null,
    searchContent: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        searchContent?.let {
            it()
            Spacer(Modifier.height(24.dp))
        }
        eyebrow?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = colors.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        Text(title, style = MaterialTheme.typography.displayLarge, color = colors.onBackground)
        subtitle?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.tertiary)
        }
    }
}

@Composable
fun SalonSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search services or styles...",
    elevated: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = colors.tertiary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = colors.tertiary)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.onBackground.copy(alpha = 0.1f),
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (elevated) Modifier.shadow(6.dp, RoundedCornerShape(24.dp), clip = false) else Modifier)
    )
}

/**
 * Drives the floating pill's slide offset. `barHeightPx` is filled in from a
 * real on-screen measurement (see reportHeight) rather than guessed — a
 * wrong guess is what left a visible sliver behind in testing.
 */
@Stable
class SearchBarFloatState(
    private val revealThresholdPx: Float,
    private val extraHiddenPx: Float = 0f
) {
    var barHeightPx by mutableStateOf(0f)
        private set
    var offset by mutableStateOf(0f)
        private set
    private var upwardAccum = 0f

    private val hiddenOffset: Float get() = -(barHeightPx + extraHiddenPx)

    fun reportHeight(px: Float) {
        val firstMeasurement = barHeightPx <= 0f
        barHeightPx = px
        if (firstMeasurement) offset = hiddenOffset
    }

    fun onScroll(deltaY: Float) {
        if (barHeightPx <= 0f) return
        if (deltaY < 0f) {
            upwardAccum = 0f
            offset = (offset + deltaY).coerceIn(hiddenOffset, 0f)
        } else if (deltaY > 0f) {
            upwardAccum += deltaY
            val revealed = (upwardAccum - revealThresholdPx).coerceAtLeast(0f)
            offset = (hiddenOffset + revealed).coerceIn(hiddenOffset, 0f)
        }
    }
}
