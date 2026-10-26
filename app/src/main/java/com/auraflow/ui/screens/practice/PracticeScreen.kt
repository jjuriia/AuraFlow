package com.auraflow.ui.screens.practice

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auraflow.domain.audio.NatureSound
import com.auraflow.domain.plan.BreathPhase
import com.auraflow.domain.plan.BreathTechnique
import com.auraflow.domain.plan.PlanDay
import com.auraflow.domain.plan.PracticeStage
import com.auraflow.domain.plan.StageSpec
import com.auraflow.domain.stats.StatsCalculator
import com.auraflow.ui.components.ChladniPlate
import com.auraflow.ui.components.GlassCard
import com.auraflow.ui.components.MOOD_LABELS
import com.auraflow.ui.components.MoodSlider
import com.auraflow.ui.components.OptionChip
import com.auraflow.ui.components.SingingBowlButton
import com.auraflow.ui.components.formatDuration
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.Outline
import com.auraflow.ui.theme.TextMuted

@Composable
fun PracticeScreen(
    onExit: () -> Unit,
    viewModel: PracticeViewModel = viewModel(factory = PracticeViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }
    BackHandler(enabled = state.phase == PracticePhase.PRACTICE) { confirmExit = true }

    val plan = state.plan ?: return

    AnimatedContent(
        targetState = state.phase,
        transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(400)) },
        label = "practicePhase"
    ) { phase ->
        when (phase) {
            PracticePhase.MOOD_BEFORE -> MoodStep(
                header = state.header,
                title = "Как вы сейчас?",
                subtitle = "Отметьте без оценки — так вы увидите, что меняет практика.",
                value = state.moodBefore,
                onChange = viewModel::setMoodBefore,
                confirmText = "Начать практику",
                onConfirm = viewModel::start,
                onBack = onExit
            ) {
                PracticeOptions(state, viewModel)
            }
            PracticePhase.PRACTICE -> PracticeContent(state, plan, viewModel)
            PracticePhase.MOOD_AFTER -> MoodStep(
                header = if (state.completed) "Сессия завершена · ${formatDuration(state.durationSec)}" else "Практика прервана · ${formatDuration(state.durationSec)}",
                title = "Как вы теперь?",
                subtitle = "Сравните с ощущением до практики. Любой ответ — нормальный.",
                value = state.moodAfter,
                onChange = viewModel::setMoodAfter,
                confirmText = "Проявить узор сессии",
                onConfirm = viewModel::complete,
                onBack = null
            )
            PracticePhase.SUMMARY -> SummaryContent(state, viewModel, onExit)
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Прервать практику?") },
            text = { Text("Сессия не будет сохранена. Чтобы сохранить её, нажмите «Завершить».") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    onExit()
                }) { Text("Прервать") }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("Продолжить") }
            }
        )
    }
}

@Composable
private fun MoodStep(
    header: String,
    title: String,
    subtitle: String,
    value: Int,
    onChange: (Int) -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    onBack: (() -> Unit)?,
    options: @Composable () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(header, style = MaterialTheme.typography.labelLarge, color = NeonCyan, textAlign = TextAlign.Center)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        MoodSlider(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth())
        options()
        Spacer(Modifier.height(16.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(confirmText) }
        onBack?.let { TextButton(onClick = it) { Text("Назад") } }
    }
}

@Composable
private fun PracticeContent(state: PracticeUiState, plan: PlanDay, viewModel: PracticeViewModel) {
    val spec = state.stage ?: return
    val eyesClosed = spec.stage.eyesClosed
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Этап ${state.stageIndex + 1} из ${plan.stages.size}", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Text(spec.stage.title, style = MaterialTheme.typography.titleLarge)
            }
            state.sound?.let { sound ->
                IconButton(onClick = viewModel::toggleSound) {
                    Icon(
                        if (state.soundOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = if (state.soundOn) "Выключить звук «${sound.title}»" else "Включить звук «${sound.title}»",
                        tint = if (state.soundOn) NeonCyan else TextMuted
                    )
                }
            }
            TextButton(onClick = viewModel::finishEarly) { Text("Завершить") }
        }
        StageProgress(plan.stages, state.stageIndex, state.stageElapsedSec, Modifier.fillMaxWidth().height(6.dp))

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ChladniPlate(
                engine = viewModel.engine,
                onTouch = viewModel::onPlateTouch,
                modifier = Modifier.padding(4.dp),
                glow = { breathGlow(state.breath) },
                dimmed = eyesClosed
            )
            if (eyesClosed) {
                Text("Глаза можно закрыть", style = MaterialTheme.typography.titleMedium, color = TextMuted)
            }
        }

        Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
            state.breath?.let { breath ->
                AnimatedContent(targetState = breath.phase, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "breath") { phase ->
                    Text(phase.title, style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
                }
            }
        }
        AnimatedContent(
            targetState = state.cue,
            transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(400)) },
            label = "cue",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
        ) { cue ->
            Text(cue, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SingingBowlButton(onStrike = viewModel::onBowlTap, externalStrikes = state.bowlStrikes)
            if (plan.stages.any { it.stage == PracticeStage.ANCHOR }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.attentionReturns}", style = MaterialTheme.typography.displaySmall, color = NeonCyan)
                    Text(
                        "возвращений\nвнимания",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun breathGlow(breath: BreathUi?): Float {
    if (breath == null) return 0f
    val progress = ((System.nanoTime() - breath.startedAtNanos) / 1_000_000_000f / breath.phaseSeconds).coerceIn(0f, 1f)
    return when (breath.phase) {
        BreathPhase.INHALE -> progress
        BreathPhase.HOLD_IN -> 1f
        BreathPhase.EXHALE -> 1f - progress
        BreathPhase.HOLD_OUT -> 0f
    }
}

@Composable
private fun StageProgress(stages: List<StageSpec>, current: Int, elapsedSec: Int, modifier: Modifier) {
    Canvas(modifier) {
        val gap = 4.dp.toPx()
        val total = stages.sumOf { it.seconds }.toFloat()
        val available = size.width - gap * (stages.size - 1)
        val corner = CornerRadius(size.height / 2f)
        var x = 0f
        stages.forEachIndexed { i, spec ->
            val width = available * spec.seconds / total
            drawRoundRect(Outline, topLeft = Offset(x, 0f), size = Size(width, size.height), cornerRadius = corner)
            val fill = when {
                i < current -> 1f
                i == current -> (elapsedSec.toFloat() / spec.seconds).coerceIn(0f, 1f)
                else -> 0f
            }
            if (fill > 0f) {
                drawRoundRect(NeonCyan, topLeft = Offset(x, 0f), size = Size(width * fill, size.height), cornerRadius = corner)
            }
            x += width + gap
        }
    }
}

@Composable
private fun SummaryContent(state: PracticeUiState, viewModel: PracticeViewModel, onExit: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Узор вашей сессии", style = MaterialTheme.typography.headlineMedium)
        Text("Сохранён в галерею личного кабинета", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        if (!state.completed) {
            Text(
                "Практика прервана раньше 80 % — засчитаны только минуты, без бонуса за завершение" +
                    if (state.plan?.day ?: 0 > 0) " и без отметки дня плана." else ".",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
        Text(
            "Дух-хранитель получил +${StatsCalculator.pointsFor(state.durationSec, state.completed)} света",
            style = MaterialTheme.typography.labelLarge,
            color = NeonCyan
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ChladniPlate(
                engine = viewModel.engine,
                onTouch = viewModel::onPlateTouch,
                modifier = Modifier.padding(4.dp),
                description = "Итоговый узор сессии"
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SummaryValue(formatDuration(state.durationSec), "длительность")
            SummaryValue("${state.attentionReturns}", "возвращений")
            SummaryValue("${MOOD_LABELS[state.moodBefore - 1]} → ${MOOD_LABELS[state.moodAfter - 1]}", "настроение")
        }
        if (state.kind == PracticeKind.SOS) HelpCard()
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Готово") }
    }
}

@Composable
private fun HelpCard() {
    val context = LocalContext.current
    GlassCard(Modifier.fillMaxWidth()) {
        Text(
            "Если тревога не отступает или есть угроза жизни — не оставайтесь с этим одни. " +
                "Позвоните близкому человеку или в экстренную службу.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        OutlinedButton(
            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Позвонить 112") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeOptions(state: PracticeUiState, viewModel: PracticeViewModel) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.canChooseTechnique) {
            Text("Техника дыхания", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val planPattern = state.plan?.breath
                if (state.kind == PracticeKind.PLAN && planPattern != null) {
                    OptionChip("По плану ${planPattern.label}", state.technique == null) { viewModel.selectTechnique(null) }
                }
                for (technique in BreathTechnique.entries) {
                    OptionChip("${technique.title} ${technique.pattern.label}", state.technique == technique) {
                        viewModel.selectTechnique(technique)
                    }
                }
            }
            state.technique?.let { Text(it.description, style = MaterialTheme.typography.bodySmall, color = TextMuted) }
        }
        Text("Звуки природы", style = MaterialTheme.typography.labelLarge, color = TextMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip("Без звука", state.sound == null) { viewModel.selectSound(null) }
            for (sound in NatureSound.entries) {
                OptionChip(sound.title, state.sound == sound) { viewModel.selectSound(sound) }
            }
        }
    }
}

@Composable
private fun SummaryValue(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = NeonCyan, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
