package com.auraflow.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.auraflow.domain.physics.Spring
import com.auraflow.ui.theme.MoodColors
import com.auraflow.ui.theme.TextMuted
import com.auraflow.ui.theme.moodColorAt
import kotlin.math.abs
import kotlin.math.roundToInt

val MOOD_LABELS = listOf("Тяжело", "Тревожно", "Ровно", "Спокойно", "Светло")

@Composable
fun MoodSlider(value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val spring = remember { Spring(position = (value - 1) / 4f) }
    val dragging = remember { booleanArrayOf(false) }
    val frame = remember { mutableLongStateOf(0L) }
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onValueChange)
    val labelColor by animateColorAsState(MoodColors[value - 1], label = "moodColor")

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                spring.step(((now - last) / 1_000_000_000f).coerceAtMost(MAX_DT))
                last = now
                frame.longValue = now
            }
        }
    }
    LaunchedEffect(value) {
        if (!dragging[0]) spring.target = (value - 1) / 4f
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "moodLabel"
        ) { v ->
            Text(MOOD_LABELS[v - 1], style = MaterialTheme.typography.headlineMedium, color = labelColor)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .semantics {
                    stateDescription = MOOD_LABELS[value - 1]
                    progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 1f..5f, steps = 3)
                    setProgress { target ->
                        currentOnChange(target.roundToInt().coerceIn(1, 5))
                        true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val index = (fractionAt(offset.x, size.width.toFloat(), PAD.toPx()) * 4).roundToInt()
                        spring.target = index / 4f
                        currentOnChange(index + 1)
                    }
                }
                .pointerInput(Unit) {
                    val pad = PAD.toPx()
                    fun release() {
                        dragging[0] = false
                        val index = (spring.target * 4).roundToInt()
                        spring.target = index / 4f
                        currentOnChange(index + 1)
                    }
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging[0] = true
                            spring.target = fractionAt(offset.x, size.width.toFloat(), pad)
                        },
                        onDragEnd = { release() },
                        onDragCancel = { release() },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            spring.target = fractionAt(change.position.x, size.width.toFloat(), pad)
                            val index = (spring.target * 4).roundToInt()
                            if (index + 1 != currentValue) currentOnChange(index + 1)
                        }
                    )
                }
        ) {
            frame.longValue
            val pad = PAD.toPx()
            val y = size.height / 2f
            val width = size.width - 2 * pad

            drawLine(
                brush = Brush.horizontalGradient(MoodColors.map { it.copy(alpha = 0.5f) }, startX = pad, endX = size.width - pad),
                start = Offset(pad, y),
                end = Offset(size.width - pad, y),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            for (i in 0..4) {
                drawCircle(MoodColors[i].copy(alpha = 0.7f), radius = 4.dp.toPx(), center = Offset(pad + width * i / 4f, y))
            }

            val position = spring.position.coerceIn(-0.1f, 1.1f)
            val x = pad + width * position
            val color = moodColorAt(position)

            val targetX = pad + width * spring.target
            val band = Path().apply {
                moveTo(targetX, y)
                quadraticTo((targetX + x) / 2f, y + abs(targetX - x) * 0.25f, x, y)
            }
            drawPath(band, color.copy(alpha = 0.6f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

            val radius = 14.dp.toPx()
            val stretch = (1f + abs(spring.velocity) * 0.12f).coerceAtMost(1.45f)
            drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), Offset(x, y), radius * 2.6f),
                radius = radius * 2.6f,
                center = Offset(x, y)
            )
            drawOval(
                color = color,
                topLeft = Offset(x - radius * stretch, y - radius / stretch),
                size = Size(2 * radius * stretch, 2 * radius / stretch)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = radius * 0.3f,
                center = Offset(x - radius * 0.3f * stretch, y - radius * 0.35f / stretch)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(MOOD_LABELS.first(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(MOOD_LABELS.last(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

private fun fractionAt(x: Float, width: Float, pad: Float): Float =
    ((x - pad) / (width - 2 * pad)).coerceIn(0f, 1f)

private val PAD = 24.dp
private const val MAX_DT = 0.033f
