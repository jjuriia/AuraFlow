package com.auraflow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.auraflow.domain.stats.SpiritState
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.NeonViolet
import com.auraflow.ui.theme.SandGold
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Orbit(val radius: Float, val speed: Float, val phase: Float, val size: Float)

@Composable
fun SpiritView(state: SpiritState, modifier: Modifier = Modifier) {
    val time = rememberFrameSeconds()
    val pulse = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val orbits = remember(state.particles) {
        val random = Random(state.particles)
        List(state.particles) {
            Orbit(
                radius = 1.7f + random.nextFloat() * 1.3f,
                speed = (0.15f + random.nextFloat() * 0.35f) * if (random.nextBoolean()) 1f else -1f,
                phase = random.nextFloat() * 6.28f,
                size = 1.2f + random.nextFloat() * 2.2f
            )
        }
    }
    val color = spiritColor(state.growth)

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = "${state.title}. Лепестков: ${state.petals}." }
            .pointerInput(Unit) {
                detectTapGestures {
                    scope.launch {
                        pulse.snapTo(1f)
                        pulse.animateTo(0f, tween(durationMillis = 1600))
                    }
                }
            }
    ) {
        val t = time.value
        val c = center
        val r = size.minDimension * 0.15f
        val breath = 1f + 0.06f * sin(t * 0.9f) + 0.18f * pulse.value
        val glow = (state.brightness + 0.4f * pulse.value).coerceAtMost(1f)

        val auraRadius = r * 3.3f * breath
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.38f * glow), Color.Transparent), c, auraRadius),
            radius = auraRadius,
            center = c
        )

        rotate(degrees = t * 6f, pivot = c) {
            for (i in 0 until state.petals) {
                rotate(degrees = 360f / state.petals * i, pivot = c) {
                    val length = r * (1.7f + 0.2f * sin(t * 1.3f + i)) * breath
                    drawPath(
                        path = petal(c, r * 0.3f, length, r * 0.55f),
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, color.copy(alpha = 0.7f * glow)),
                            startY = c.y - length,
                            endY = c.y
                        )
                    )
                }
            }
        }

        for ((i, o) in orbits.withIndex()) {
            val angle = o.phase + t * o.speed
            val twinkle = 0.5f + 0.5f * sin(t * 2f + i)
            drawCircle(
                color = color.copy(alpha = (0.25f + 0.6f * twinkle) * glow),
                radius = o.size * density,
                center = Offset(c.x + cos(angle) * r * o.radius * breath, c.y + sin(angle) * r * o.radius * breath)
            )
        }

        val core = Path().apply {
            moveTo(c.x, c.y - r * 0.9f * breath)
            lineTo(c.x + r * 0.55f * breath, c.y)
            lineTo(c.x, c.y + r * 0.9f * breath)
            lineTo(c.x - r * 0.55f * breath, c.y)
            close()
        }
        drawPath(
            core,
            Brush.linearGradient(
                listOf(Color.White, color, color.copy(alpha = 0.6f)),
                start = Offset(c.x - r, c.y - r),
                end = Offset(c.x + r, c.y + r)
            )
        )
        drawCircle(Color.White.copy(alpha = 0.5f + 0.4f * pulse.value), radius = r * 0.16f, center = Offset(c.x - r * 0.1f, c.y - r * 0.25f))
    }
}

private fun petal(c: Offset, base: Float, length: Float, width: Float): Path {
    val bottom = c.y - base
    val tip = c.y - length
    return Path().apply {
        moveTo(c.x, bottom)
        cubicTo(c.x + width, bottom - (bottom - tip) * 0.3f, c.x + width * 0.5f, tip + (bottom - tip) * 0.15f, c.x, tip)
        cubicTo(c.x - width * 0.5f, tip + (bottom - tip) * 0.15f, c.x - width, bottom - (bottom - tip) * 0.3f, c.x, bottom)
        close()
    }
}

fun spiritColor(growth: Float): Color = if (growth < 0.5f) {
    lerp(NeonCyan, NeonViolet, growth * 2f)
} else {
    lerp(NeonViolet, SandGold, (growth - 0.5f) * 2f)
}
