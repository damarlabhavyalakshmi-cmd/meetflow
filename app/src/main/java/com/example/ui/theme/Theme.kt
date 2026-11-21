package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MeetFlowLightColorScheme = lightColorScheme(
    primary = RoyalBlue600,
    onPrimary = Color.White,
    primaryContainer = RoyalBlue100,
    onPrimaryContainer = Navy900,
    secondary = SkyCyan500,
    onSecondary = Color.White,
    secondaryContainer = SkyCyan100,
    onSecondaryContainer = Navy900,
    tertiary = Indigo600,
    onTertiary = Color.White,
    tertiaryContainer = Indigo100,
    onTertiaryContainer = Navy900,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    outlineVariant = Slate200,
    error = Crimson600,
    onError = Color.White,
    errorContainer = Crimson100,
    onErrorContainer = Crimson900
)

private val MeetFlowDarkColorScheme = darkColorScheme(
    primary = RoyalBlue500,
    onPrimary = Color.White,
    primaryContainer = Navy800,
    onPrimaryContainer = RoyalBlue100,
    secondary = SkyCyan400,
    onSecondary = Navy950,
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = SkyCyan100,
    tertiary = Color(0xFF818CF8),
    onTertiary = Navy950,
    tertiaryContainer = Color(0xFF312E81),
    onTertiaryContainer = Indigo100,
    background = Navy950,
    onBackground = Slate50,
    surface = Color(0xFF111C33),
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate800,
    outlineVariant = Color(0xFF1E293B),
    error = Crimson500,
    onError = Color.White,
    errorContainer = Crimson900,
    onErrorContainer = Crimson100
)

val MeetFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MeetFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MeetFlowDarkColorScheme else MeetFlowLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MeetFlowTypography,
        shapes = MeetFlowShapes,
        content = content
    )
}
