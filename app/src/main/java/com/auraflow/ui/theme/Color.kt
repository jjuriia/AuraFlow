package com.auraflow.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val Night = Color(0xFF05060F)
val DeepSpace = Color(0xFF0B0E24)
val SurfaceGlass = Color(0xFF14183A)
val SurfaceGlassHigh = Color(0xFF1E2350)
val Outline = Color(0xFF2E3466)

val NeonCyan = Color(0xFF5CF2E6)
val NeonViolet = Color(0xFFA78BFA)
val NeonRose = Color(0xFFFF7AB6)
val Mint = Color(0xFF7CF5A8)
val SandGold = Color(0xFFFFD27A)

val TextPrimary = Color(0xFFE9EAF8)
val TextMuted = Color(0xFF9CA1C8)
val ErrorSoft = Color(0xFFFF8A8A)

val PlateLight = Color(0xFF1E2452)
val PlateDark = Color(0xFF0A0D22)
val BowlBrassLight = Color(0xFFE9B864)
val BowlBrassDark = Color(0xFF6E4A1C)
val BowlInner = Color(0xFF2A1C0C)

val MoodColors = listOf(Color(0xFF6C7BFF), NeonViolet, NeonCyan, Mint, SandGold)

fun moodColorAt(fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f) * (MoodColors.size - 1)
    val i = f.toInt().coerceAtMost(MoodColors.size - 2)
    return lerp(MoodColors[i], MoodColors[i + 1], f - i)
}
