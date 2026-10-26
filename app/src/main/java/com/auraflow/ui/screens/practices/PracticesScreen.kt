package com.auraflow.ui.screens.practices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auraflow.domain.plan.CustomPractice
import com.auraflow.domain.plan.PlanGenerator
import com.auraflow.domain.plan.PracticeStage
import com.auraflow.ui.components.GlassCard
import com.auraflow.ui.screens.practice.PracticeKind
import com.auraflow.ui.theme.ErrorSoft
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.NeonRose
import com.auraflow.ui.theme.Night
import com.auraflow.ui.theme.SurfaceGlassHigh
import com.auraflow.ui.theme.TextMuted

@Composable
fun PracticesScreen(
    onStart: (PracticeKind, Long) -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: PracticesViewModel = viewModel(factory = PracticesViewModel.Factory)
) {
    val practices by viewModel.practices.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<CustomPractice?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Практики", style = MaterialTheme.typography.headlineMedium) }
        item { SosCard(onStart = { onStart(PracticeKind.SOS, 0) }) }
        item {
            val pause = PlanGenerator.pause()
            GlassCard(Modifier.fillMaxWidth()) {
                Text("${pause.title} · ${pause.minutes} мин", style = MaterialTheme.typography.titleMedium)
                Text(pause.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                OutlinedButton(onClick = { onStart(PracticeKind.PAUSE, 0) }, modifier = Modifier.fillMaxWidth()) { Text("Начать паузу") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Мои практики", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Соберите сессию из блоков: дыхание, тишина, объект внимания, сканирование тела, заземление.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
        if (practices.isEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Своих практик пока нет.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        }
        items(practices, key = { it.id }) { practice ->
            PracticeCard(
                practice,
                onStart = { onStart(PracticeKind.CUSTOM, practice.id) },
                onEdit = { onEdit(practice.id) },
                onDelete = { toDelete = practice }
            )
        }
        item {
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("  Собрать практику")
            }
        }
    }

    toDelete?.let { practice ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = SurfaceGlassHigh,
            title = { Text("Удалить «${practice.name}»?") },
            text = { Text("История уже пройденных сессий сохранится.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(practice.id)
                    toDelete = null
                }) { Text("Удалить", color = ErrorSoft) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun SosCard(onStart: () -> Unit) {
    val sos = PlanGenerator.sos()
    GlassCard(Modifier.fillMaxWidth(), highlighted = true) {
        Text("Тревожно прямо сейчас?", style = MaterialTheme.typography.titleLarge, color = NeonRose)
        Text(
            "${sos.minutes} минуты: ${sos.description.replaceFirstChar { it.lowercase() }}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Night),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Экстренная помощь") }
    }
}

@Composable
private fun PracticeCard(practice: CustomPractice, onStart: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(practice.name, style = MaterialTheme.typography.titleMedium)
                Text("${(practice.totalSeconds + 30) / 60} мин", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Изменить", tint = TextMuted) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Удалить", tint = TextMuted) }
        }
        Text(practice.toPlanDay().description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        val extras = buildList {
            if (practice.blocks.any { it.stage == PracticeStage.BREATH }) add("дыхание ${practice.technique.title.lowercase()} ${practice.technique.pattern.label}")
            practice.sound?.let { add("звук: ${it.title.lowercase()}") }
        }
        if (extras.isNotEmpty()) Text(extras.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        OutlinedButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Начать") }
    }
}
