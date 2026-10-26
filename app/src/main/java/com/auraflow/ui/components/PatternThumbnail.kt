package com.auraflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.auraflow.domain.chladni.ChladniField
import com.auraflow.domain.model.ChladniPattern
import com.auraflow.ui.theme.Mint
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.NeonRose
import com.auraflow.ui.theme.NeonViolet
import com.auraflow.ui.theme.PlateDark
import com.auraflow.ui.theme.PlateLight
import com.auraflow.ui.theme.SandGold

@Composable
fun PatternThumbnail(pattern: ChladniPattern, modifier: Modifier = Modifier) {
    val points = remember(pattern) { ChladniField.sampleNodalPoints(pattern.n, pattern.m, pattern.seed, POINTS) }
    val tint = remember(pattern) { patternTint(pattern.seed) }
    Canvas(modifier.aspectRatio(1f).semantics { contentDescription = "Узор Хладни (${pattern.n}, ${pattern.m})" }) {
        val corner = CornerRadius(size.minDimension * 0.1f)
        drawRoundRect(
            brush = Brush.radialGradient(listOf(PlateLight, PlateDark), center = center, radius = size.minDimension * 0.8f),
            cornerRadius = corner
        )
        val inset = size.width * 0.06f
        val spanX = size.width - 2 * inset
        val spanY = size.height - 2 * inset
        val offsets = List(points.size / 2) { i -> Offset(inset + points[2 * i] * spanX, inset + points[2 * i + 1] * spanY) }
        drawPoints(offsets, PointMode.Points, tint.copy(alpha = 0.25f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawPoints(offsets, PointMode.Points, tint, strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
        drawRoundRect(tint.copy(alpha = 0.25f), cornerRadius = corner, style = Stroke(width = 1.dp.toPx()))
    }
}

private val TINTS = listOf(SandGold, NeonCyan, NeonViolet, NeonRose, Mint)

private fun patternTint(seed: Long): Color = TINTS[Math.floorMod(seed, TINTS.size.toLong()).toInt()]

private const val POINTS = 900
