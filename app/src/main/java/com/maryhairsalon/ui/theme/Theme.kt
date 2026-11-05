/**
 * Theme.kt
 *
 * Root Material 3 theme composable wrapper for Mary Hair Salon.
 * Integrates light and dark color schemes according to user theme preferences.
 */
package com.maryhairsalon.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.maryhairsalon.data.local.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = AccentAmber,
    secondary = SurfaceLighter,
    tertiary = TextMuted,
    background = BackgroundDeep,
    surface = SurfaceLighter,
    onPrimary = BackgroundDeep,
    onSecondary = TextMain,
    onTertiary = TextMain,
    onBackground = TextMain,
    onSurface = TextMain,
    error = Error
)

private val LightColorScheme = lightColorScheme(
    primary = AccentAmber,
    secondary = SurfaceLighterLight,
    tertiary = TextMuted,
    background = Color(0xFFFFFBFE),
    surface = SurfaceLighterLight,
    onPrimary = Color.White,
    onSecondary = Color(0xFF1C1B1F),
    onTertiary = Color(0xFF1C1B1F),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    error = Error
)

@Composable
fun MaryHairSalonTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
