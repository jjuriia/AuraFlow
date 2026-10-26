package com.auraflow.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auraflow.domain.model.Quote
import com.auraflow.domain.plan.PlanDay
import com.auraflow.domain.plan.PlanGenerator
import com.auraflow.ui.components.GlassCard
import com.auraflow.ui.components.SpiritView
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.NeonViolet
import com.auraflow.ui.theme.Outline
import com.auraflow.ui.theme.TextMuted

@Composable
fun HomeScreen(
    onStartPractice: (day: Int) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) return

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(state.greeting, style = MaterialTheme.typography.bodyLarge, color = TextMuted)
            Text(state.name, style = MaterialTheme.typography.headlineMedium)
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            SpiritView(state.spirit, Modifier.fillMaxWidth(0.78f))
            Text(state.spirit.title, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
            Text(
                "Яркость — дни практики за неделю (${state.stats.daysThisWeek} из 7), " +
                    "лепестки — общее время (${state.stats.totalMinutes} мин). Коснитесь духа.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        state.today?.let { day ->
            TodayCard(day, state.completedDays, state.planCompleted, onStart = { onStartPractice(day.day) })
        }

        OutlinedButton(onClick = { onStartPractice(0) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Пауза · 2 минуты с пластиной")
        }

        state.quote?.let { QuoteCard(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayCard(day: PlanDay, completed: Set<Int>, planCompleted: Boolean, onStart: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), highlighted = true) {
        PlanDots(completed, current = day.day)
        Text(
            if (planCompleted) "План пройден — можно повторить заключительный день" else "День ${day.day} из ${PlanGenerator.PLAN_DAYS}",
            style = MaterialTheme.typography.labelLarge,
            color = NeonCyan
        )
        Text(day.title, style = MaterialTheme.typography.titleLarge)
        Text(day.description, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (spec in day.stages) StageChip("${spec.stage.title} · ${(spec.seconds + 30) / 60} мин")
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 4.dp)) {
            Text("Начать практику · ${day.minutes} мин")
        }
    }
}

@Composable
private fun PlanDots(completed: Set<Int>, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        for (d in 1..PlanGenerator.PLAN_DAYS) {
            val done = d in completed
            Box(
                Modifier
                    .size(if (d == current && !done) 12.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (done) NeonCyan else NeonCyan.copy(alpha = 0f))
                    .border(1.5.dp, if (done || d == current) NeonCyan else Outline, CircleShape)
            )
        }
    }
}

@Composable
private fun StageChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted,
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, Outline, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun QuoteCard(quote: Quote) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Мысль дня", style = MaterialTheme.typography.labelLarge, color = NeonViolet)
        Text("«${quote.text}»", style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic))
        if (quote.author.isNotBlank()) {
            Text("— ${quote.author}", style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
    }
}
