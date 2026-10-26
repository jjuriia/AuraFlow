package com.auraflow.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auraflow.domain.model.Session
import com.auraflow.ui.components.GlassCard
import com.auraflow.ui.components.MOOD_LABELS
import com.auraflow.ui.components.PatternThumbnail
import com.auraflow.ui.components.formatDate
import com.auraflow.ui.components.formatDuration
import com.auraflow.ui.components.formatMoodDelta
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.NeonViolet
import com.auraflow.ui.theme.Outline
import com.auraflow.ui.theme.SurfaceGlassHigh
import com.auraflow.ui.theme.TextMuted

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Session?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("Личный кабинет", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Text(state.name, style = MaterialTheme.typography.headlineMedium)
            }
        }
        item { StatTile("${state.stats.daysThisWeek} из 7", "дней практики за неделю") }
        item { StatTile("${state.stats.totalMinutes}", "минут практики всего") }
        item { StatTile(formatMoodDelta(state.stats.averageMoodDelta), "среднее изменение настроения за сессию") }
        item { StatTile("${state.stats.totalReturns}", "возвращений внимания — каждое это повторение") }

        if (state.sessions.size >= 2) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MoodChartCard(state.sessions.take(CHART_SESSIONS).reversed())
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth()) {
                Text("Галерея узоров", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Каждая сессия оставляет свой узор: форма зависит от длительности, возвращений внимания и изменения настроения.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        if (state.sessions.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Здесь появятся узоры ваших сессий. Начните первую практику на главном экране.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }

        items(state.sessions, key = { it.id }) { session ->
            Column(
                Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { selected = session }
            ) {
                PatternThumbnail(session.pattern, Modifier.fillMaxWidth())
                Text(formatDate(session.startedAt), style = MaterialTheme.typography.labelMedium)
                Text(
                    "${formatDuration(session.durationSec)}${if (session.completed) "" else " · прервана"} · " +
                        "${MOOD_LABELS[session.moodBefore - 1]} → ${MOOD_LABELS[session.moodAfter - 1]}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }

    selected?.let { session -> SessionDialog(session, onDismiss = { selected = null }) }
}

@Composable
private fun StatTile(value: String, label: String) {
    GlassCard(Modifier.fillMaxWidth().height(116.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
    }
}

@Composable
private fun MoodChartCard(sessions: List<Session>) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Настроение до и после", style = MaterialTheme.typography.titleMedium)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val stepX = size.width / (sessions.size - 1)
            fun y(mood: Int) = size.height * (5 - mood) / 4f

            for (level in 1..5) {
                drawLine(Outline.copy(alpha = 0.5f), Offset(0f, y(level)), Offset(size.width, y(level)), strokeWidth = 1f)
            }
            fun series(values: List<Int>, color: Color) {
                val path = Path()
                values.forEachIndexed { i, v ->
                    if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * stepX, y(v))
                }
                drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                values.forEachIndexed { i, v -> drawCircle(color, radius = 3.5.dp.toPx(), center = Offset(i * stepX, y(v))) }
            }
            series(sessions.map { it.moodBefore }, NeonViolet.copy(alpha = 0.8f))
            series(sessions.map { it.moodAfter }, NeonCyan)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(NeonViolet, "до практики")
            Legend(NeonCyan, "после практики")
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
private fun SessionDialog(session: Session, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceGlassHigh,
        title = { Text(formatDate(session.startedAt)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PatternThumbnail(session.pattern, Modifier.fillMaxWidth())
                Text(
                    if (session.planDay != null) "День плана ${session.planDay}" else "Пауза",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
                Text("Длительность: ${formatDuration(session.durationSec)}" + if (session.completed) "" else " (прервана)")
                Text("Возвращений внимания: ${session.attentionReturns}")
                Text("Настроение: ${MOOD_LABELS[session.moodBefore - 1]} → ${MOOD_LABELS[session.moodAfter - 1]}")
                Text(
                    "Узор (${session.pattern.n}, ${session.pattern.m})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

private const val CHART_SESSIONS = 10
