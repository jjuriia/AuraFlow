package com.auraflow.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auraflow.ui.theme.BowlBrassDark
import com.auraflow.ui.theme.BowlBrassLight
import com.auraflow.ui.theme.BowlInner
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.NeonViolet
import com.auraflow.ui.theme.SandGold
import com.auraflow.ui.theme.TextMuted
import kotlin.math.exp
import kotlin.math.sin

@Composable
fun SingingBowlButton(
    onStrike: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Ударить в чашу",
    externalStrikes: Int = 0,
    bowlSize: Dp = 112.dp
) {
    val strikes = remember { ArrayList<Long>() }
    val frame = remember { mutableLongStateOf(0L) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "bowlPress"
    )

    LaunchedEffect(Unit) {
        while (true) withFrameNanos { frame.longValue = it }
    }
    LaunchedEffect(externalStrikes) {
        if (externalStrikes > 0) strikes.add(frame.longValue)
    }

    Column(
        modifier = modifier.clickable(
            interactionSource = interaction,
            indication = null,
            role = Role.Button,
            onClickLabel = label
        ) {
            strikes.add(frame.longValue)
            onStrike()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(Modifier.size(bowlSize)) {
            drawBowl(frame.longValue, strikes, press)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
    }
}

private fun DrawScope.drawBowl(now: Long, strikes: MutableList<Long>, press: Float) {
    strikes.removeAll { (now - it) / 1_000_000_000f > RING_SECONDS }

    var wobble = 0f
    var energy = 0f
    for (s in strikes) {
        val t = ((now - s) / 1_000_000_000f).coerceAtLeast(0f)
        val envelope = exp(-GAMMA * t)
        wobble += envelope * sin(OMEGA * t)
        energy += envelope
    }
    wobble = wobble.coerceIn(-1.5f, 1.5f)
    energy = energy.coerceAtMost(1f)

    val c = center
    val r = size.minDimension * 0.30f * press

    for (s in strikes) {
        val t = ((now - s) / 1_000_000_000f).coerceAtLeast(0f)
        for (k in 0 until WAVES) {
            val tk = t - k * WAVE_GAP
            if (tk <= 0f) continue
            val radius = r + tk * WAVE_SPEED * size.minDimension
            if (radius > size.minDimension / 2f) continue
            drawCircle(
                color = NeonCyan.copy(alpha = exp(-1.4f * tk) * 0.55f),
                radius = radius,
                center = c,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }

    drawCircle(
        brush = Brush.radialGradient(listOf(NeonViolet.copy(alpha = 0.18f + 0.4f * energy), Color.Transparent), c, r * 1.9f),
        radius = r * 1.9f,
        center = c
    )

    val sx = 1f + MODE_AMPLITUDE * wobble
    val sy = 1f - MODE_AMPLITUDE * wobble
    val rimY = c.y - r * 0.15f
    val halfWidth = r * sx
    val rimHeight = r * 0.36f * sy
    val depth = r * 0.95f * sy
    val body = Path().apply {
        moveTo(c.x - halfWidth, rimY)
        cubicTo(c.x - halfWidth, rimY + depth * 0.9f, c.x - halfWidth * 0.45f, rimY + depth, c.x, rimY + depth)
        cubicTo(c.x + halfWidth * 0.45f, rimY + depth, c.x + halfWidth, rimY + depth * 0.9f, c.x + halfWidth, rimY)
        close()
    }
    drawPath(body, Brush.verticalGradient(listOf(BowlBrassLight, BowlBrassDark), startY = rimY, endY = rimY + depth))

    drawOval(
        color = Color.White.copy(alpha = 0.14f),
        topLeft = Offset(c.x - halfWidth * 0.62f, rimY + depth * 0.28f),
        size = Size(halfWidth * 0.32f, depth * 0.38f)
    )

    val rimTopLeft = Offset(c.x - halfWidth, rimY - rimHeight / 2f)
    val rimSize = Size(halfWidth * 2f, rimHeight)
    drawOval(
        brush = Brush.verticalGradient(listOf(BowlInner, BowlBrassDark), startY = rimTopLeft.y, endY = rimTopLeft.y + rimHeight),
        topLeft = rimTopLeft,
        size = rimSize
    )
    drawOval(
        color = SandGold.copy(alpha = 0.6f + 0.4f * energy),
        topLeft = rimTopLeft,
        size = rimSize,
        style = Stroke(width = 2.dp.toPx())
    )
}

private const val RING_SECONDS = 5f
private const val GAMMA = 0.9f
private const val OMEGA = 43.982297f
private const val MODE_AMPLITUDE = 0.05f
private const val WAVES = 3
private const val WAVE_GAP = 0.35f
private const val WAVE_SPEED = 0.18f
