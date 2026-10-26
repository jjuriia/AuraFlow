package com.auraflow.ui.screens.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auraflow.domain.audio.NatureSound
import com.auraflow.domain.plan.BreathTechnique
import com.auraflow.domain.plan.CustomPractice
import com.auraflow.domain.plan.PracticeBlock
import com.auraflow.ui.components.GlassCard
import com.auraflow.ui.components.OptionChip
import com.auraflow.ui.theme.ErrorSoft
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.TextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuilderScreen(
    onClose: () -> Unit,
    viewModel: BuilderViewModel = viewModel(factory = BuilderViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onClose() }
    if (state.loading) return
    val practice = state.practice

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.editing) "Изменить практику" else "Новая практика",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Закрыть") }
        }

        OutlinedTextField(
            value = practice.name,
            onValueChange = viewModel::setName,
            label = { Text("Название") },
            placeholder = { Text("Например, «Вечерняя тишина»") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Блоки практики", style = MaterialTheme.typography.titleMedium)
        Text(
            "Практика начинается с короткого настроя и заканчивается завершением — по ${CustomPractice.EDGE_SECONDS} секунд.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        practice.blocks.forEachIndexed { index, block ->
            BlockRow(
                block = block,
                isFirst = index == 0,
                isLast = index == practice.blocks.lastIndex,
                onMinus = { viewModel.changeMinutes(index, -1) },
                onPlus = { viewModel.changeMinutes(index, +1) },
                onUp = { viewModel.moveBlock(index, -1) },
                onDown = { viewModel.moveBlock(index, +1) },
                onRemove = { viewModel.removeBlock(index) }
            )
        }
        if (state.canAddBlock) {
            Text("Добавить блок", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (stage in CustomPractice.BLOCK_STAGES) {
                    OptionChip("+ ${stage.title}", selected = false) { viewModel.addBlock(stage) }
                }
            }
        }

        if (state.hasBreath) {
            Text("Техника дыхания", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (technique in BreathTechnique.entries) {
                    OptionChip("${technique.title} ${technique.pattern.label}", practice.technique == technique) {
                        viewModel.selectTechnique(technique)
                    }
                }
            }
            Text(practice.technique.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }

        Text("Звуки природы", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip("Без звука", practice.sound == null) { viewModel.selectSound(null) }
            for (sound in NatureSound.entries) {
                OptionChip(sound.title, practice.sound == sound) { viewModel.selectSound(sound) }
            }
        }
        Text(
            "Звук можно выключить и снова включить прямо во время практики.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        Text(
            "Всего: ${(practice.totalSeconds + 30) / 60} мин",
            style = MaterialTheme.typography.titleLarge,
            color = NeonCyan
        )
        state.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = ErrorSoft) }
        Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (state.editing) "Сохранить изменения" else "Сохранить практику")
        }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
    }
}

@Composable
private fun BlockRow(
    block: PracticeBlock,
    isFirst: Boolean,
    isLast: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(block.stage.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onUp, enabled = !isFirst) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Выше") }
            IconButton(onClick = onDown, enabled = !isLast) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Ниже") }
            IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, contentDescription = "Убрать блок", tint = TextMuted) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinus, enabled = block.minutes > 1) { Icon(Icons.Rounded.Remove, contentDescription = "Меньше") }
            Text(
                "${block.minutes} мин",
                style = MaterialTheme.typography.titleLarge,
                color = NeonCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(88.dp)
            )
            IconButton(onClick = onPlus, enabled = block.minutes < CustomPractice.MAX_BLOCK_MINUTES) {
                Icon(Icons.Rounded.Add, contentDescription = "Больше")
            }
        }
    }
}
