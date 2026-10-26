package com.auraflow.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.auraflow.domain.chladni.PlateEngine
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.Night
import com.auraflow.ui.theme.PlateDark
import com.auraflow.ui.theme.PlateLight
import com.auraflow.ui.theme.SandGold
import kotlin.math.max

private class Ripple(val x: Float, val y: Float, val startNanos: Long)

@Composable
fun ChladniPlate(
    engine: PlateEngine,
    onTouch: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
    glow: () -> Float = { 0f },
    dimmed: Boolean = false,
    description: String = "Пластина с песком. Коснитесь, когда заметите, что отвлеклись."
) {
    val frame = remember { mutableLongStateOf(0L) }
    val ripples = remember { ArrayList<Ripple>() }
    val points = remember(engine) { FloatArray(engine.sand.count * 2) }
    val glowPaint = remember {
        Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            color = SandGold.copy(alpha = 0.16f).toArgb()
        }
    }
    val grainPaint = remember {
        Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            color = SandGold.toArgb()
        }
    }
    val currentOnTouch = rememberUpdatedState(onTouch)

    LaunchedEffect(engine) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                engine.update(((now - last) / 1_000_000_000f).coerceAtMost(MAX_DT))
                last = now
                frame.longValue = now
            }
        }
    }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = description }
            .pointerInput(engine) {
                detectTapGestures { offset ->
                    val inset = size.width * INSET
                    val x = ((offset.x - inset) / (size.width - 2 * inset)).coerceIn(0f, 1f)
                    val y = ((offset.y - inset) / (size.height - 2 * inset)).coerceIn(0f, 1f)
                    ripples.add(Ripple(x, y, frame.longValue))
                    currentOnTouch.value(x, y)
                }
            }
    ) {
        val now = frame.longValue
        val inset = size.width * INSET
        val spanX = size.width - 2 * inset
        val spanY = size.height - 2 * inset
        val corner = CornerRadius(size.minDimension * 0.07f)
        val light = max(engine.energy, glow().coerceIn(0f, 1f))

        drawRoundRect(
            brush = Brush.radialGradient(listOf(PlateLight, PlateDark), center = center, radius = size.minDimension * 0.8f),
            cornerRadius = corner
        )
        drawRoundRect(
            color = NeonCyan.copy(alpha = 0.12f + 0.45f * light),
            cornerRadius = corner,
            style = Stroke(width = 1.5.dp.toPx() + 5.dp.toPx() * light)
        )

        val sand = engine.sand
        for (i in 0 until sand.count) {
            points[2 * i] = inset + sand.x[i] * spanX
            points[2 * i + 1] = inset + sand.y[i] * spanY
        }
        glowPaint.strokeWidth = 5.dp.toPx()
        grainPaint.strokeWidth = 1.8.dp.toPx()
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPoints(points, glowPaint)
            canvas.nativeCanvas.drawPoints(points, grainPaint)
        }

        ripples.removeAll { (now - it.startNanos) / 1_000_000_000f > RIPPLE_SECONDS }
        for (r in ripples) {
            val age = ((now - r.startNanos) / 1_000_000_000f).coerceAtLeast(0f)
            val fade = 1f - age / RIPPLE_SECONDS
            drawCircle(
                color = NeonCyan.copy(alpha = 0.6f * fade),
                radius = size.minDimension * (0.03f + 0.3f * age / RIPPLE_SECONDS),
                center = Offset(inset + r.x * spanX, inset + r.y * spanY),
                style = Stroke(width = 2.dp.toPx() * fade + 0.5f)
            )
        }

        if (dimmed) drawRoundRect(color = Night.copy(alpha = 0.78f), cornerRadius = corner)
    }
}

private const val INSET = 0.04f
private const val MAX_DT = 0.05f
private const val RIPPLE_SECONDS = 1.2f
