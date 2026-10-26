package com.auraflow.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auraflow.ui.components.rememberFrameSeconds
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.NeonViolet
import com.auraflow.ui.theme.SandGold
import com.auraflow.ui.theme.TextMuted

@Composable
fun SplashScreen(
    onFinished: (String) -> Unit,
    viewModel: SplashViewModel = viewModel(factory = SplashViewModel.Factory)
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    LaunchedEffect(destination) { destination?.let(onFinished) }

    val time = rememberFrameSeconds()
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(280.dp)) {
            val t = time.value
            val a = appear.value
            val c = center
            for (i in 0 until RINGS) {
                val p = (t * 0.3f + i / RINGS.toFloat()) % 1f
                drawCircle(
                    color = NeonCyan.copy(alpha = (1f - p) * 0.5f * a),
                    radius = size.minDimension / 2f * p,
                    center = c,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            val r = size.minDimension * 0.12f * (0.6f + 0.4f * a)
            drawCircle(
                brush = Brush.radialGradient(listOf(NeonViolet.copy(alpha = 0.45f * a), Color.Transparent), c, r * 3f),
                radius = r * 3f,
                center = c
            )
            val crystal = Path().apply {
                moveTo(c.x, c.y - r * 1.4f)
                lineTo(c.x + r * 0.85f, c.y)
                lineTo(c.x, c.y + r * 1.4f)
                lineTo(c.x - r * 0.85f, c.y)
                close()
            }
            drawPath(
                crystal,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = a), SandGold.copy(alpha = a)),
                    start = Offset(c.x - r, c.y - r),
                    end = Offset(c.x + r, c.y + r)
                )
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .graphicsLayer { alpha = appear.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("AuraFlow", style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 4.sp))
            Text("звук · внимание · тишина", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
    }
}

private const val RINGS = 4
