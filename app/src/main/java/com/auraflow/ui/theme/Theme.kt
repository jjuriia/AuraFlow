package com.auraflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AuraColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Night,
    primaryContainer = SurfaceGlassHigh,
    onPrimaryContainer = NeonCyan,
    secondary = NeonViolet,
    onSecondary = Night,
    secondaryContainer = SurfaceGlassHigh,
    onSecondaryContainer = NeonViolet,
    tertiary = NeonRose,
    onTertiary = Night,
    background = Night,
    onBackground = TextPrimary,
    surface = SurfaceGlass,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGlassHigh,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = Night,
    surfaceContainerLow = DeepSpace,
    surfaceContainer = SurfaceGlass,
    surfaceContainerHigh = SurfaceGlassHigh,
    surfaceContainerHighest = SurfaceGlassHigh,
    outline = Outline,
    outlineVariant = Outline,
    error = ErrorSoft,
    onError = Night
)

private val AuraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun AuraFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuraColors,
        typography = AuraTypography,
        shapes = AuraShapes,
        content = content
    )
}
