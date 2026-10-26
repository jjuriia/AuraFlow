package com.auraflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.NeonViolet
import com.auraflow.ui.theme.SurfaceGlass

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val edge = if (highlighted) {
        Brush.linearGradient(listOf(NeonCyan, NeonViolet))
    } else {
        Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.35f), NeonViolet.copy(alpha = 0.12f), Color.Transparent))
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(SurfaceGlass.copy(alpha = if (highlighted) 0.9f else 0.7f))
            .border(if (highlighted) 1.5.dp else 1.dp, edge, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}
