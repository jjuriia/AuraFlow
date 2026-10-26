package com.auraflow.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.auraflow.ui.theme.DeepSpace
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.NeonRose
import com.auraflow.ui.theme.NeonViolet
import com.auraflow.ui.theme.Night
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val radius: Float, val phase: Float, val speed: Float)

@Composable
fun CosmicBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val time = rememberFrameSeconds()
    val stars = remember {
        val random = Random(11)
        List(STAR_COUNT) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.6f + random.nextFloat() * 1.6f,
                phase = random.nextFloat() * 6.28f,
                speed = 0.4f + random.nextFloat() * 1.4f
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val t = time.value
                drawRect(Brush.verticalGradient(listOf(Night, DeepSpace, Night)))
                nebula(Offset(size.width * (0.2f + 0.06f * sin(t * 0.05f)), size.height * 0.22f), 0.65f, NeonViolet.copy(alpha = 0.11f))
                nebula(Offset(size.width * 0.9f, size.height * (0.68f + 0.05f * cos(t * 0.04f))), 0.55f, NeonCyan.copy(alpha = 0.07f))
                nebula(Offset(size.width * (0.45f + 0.05f * cos(t * 0.03f)), size.height * 1.02f), 0.5f, NeonRose.copy(alpha = 0.06f))
                for (s in stars) {
                    val twinkle = 0.5f + 0.5f * sin(t * s.speed + s.phase)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f + 0.6f * twinkle),
                        radius = s.radius * density,
                        center = Offset(s.x * size.width, s.y * size.height)
                    )
                }
            },
        content = content
    )
}

private fun DrawScope.nebula(center: Offset, radiusFraction: Float, color: Color) {
    val radius = size.maxDimension * radiusFraction
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center, radius), radius, center)
}

private const val STAR_COUNT = 140
