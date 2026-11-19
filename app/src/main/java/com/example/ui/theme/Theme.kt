package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ArkDarkColorScheme = darkColorScheme(
    primary = ArkCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1B26),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = ArkIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF232042),
    onSecondaryContainer = Color(0xFFDCD8FF),
    tertiary = ArkAmber,
    onTertiary = Color(0xFF1E1400),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    outline = DarkOutline,
    outlineVariant = Color(0xFF202533)
)

private val ArkLightColorScheme = lightColorScheme(
    primary = ArkRose,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBF0),
    onPrimaryContainer = Color(0xFF4B0721),
    secondary = ArkIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEEBFF),
    onSecondaryContainer = Color(0xFF1F1659),
    tertiary = ArkAmber,
    onTertiary = Color(0xFF2A1800),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    outline = LightOutline,
    outlineVariant = Color(0xFFECEFF5)
)

val ArkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ArkSocialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ArkDarkColorScheme else ArkLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ArkTypography,
        shapes = ArkShapes,
        content = content
    )
}
