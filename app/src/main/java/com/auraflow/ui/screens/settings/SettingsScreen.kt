package com.auraflow.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auraflow.BuildConfig
import com.auraflow.data.audio.Narrator
import com.auraflow.ui.components.GlassCard
import com.auraflow.ui.theme.ErrorSoft
import com.auraflow.ui.theme.Mint
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.Night
import com.auraflow.ui.theme.SurfaceGlassHigh
import com.auraflow.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onRebuildPlan: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }

    val settings = state.settings ?: return

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)

        GlassCard(Modifier.fillMaxWidth()) {
            SettingSwitch(
                title = "Озвучка фраз",
                description = "Голос ведёт практику, когда глаза закрыты. Текст на экране остаётся всегда.",
                checked = settings.ttsEnabled,
                onChange = viewModel::setTtsEnabled
            )
            if (settings.ttsEnabled) {
                val (text, color) = when (state.narratorStatus) {
                    Narrator.Status.READY -> "Русский голос готов" to Mint
                    Narrator.Status.INITIALIZING -> "Проверяем синтезатор речи…" to TextMuted
                    Narrator.Status.UNAVAILABLE -> "Русский голос не найден — фразы будут только текстом. Установите его в системных настройках синтеза речи." to ErrorSoft
                    Narrator.Status.OFF -> "Синтезатор выключен" to TextMuted
                }
                Text(text, style = MaterialTheme.typography.bodySmall, color = color)
                if (state.narratorStatus == Narrator.Status.READY) {
                    TextButton(onClick = viewModel::testVoice) { Text("Проверить голос") }
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            SettingSwitch(
                title = "Ежедневное напоминание",
                description = "Придёт, только если сегодня вы ещё не практиковали.",
                checked = settings.remindersEnabled,
                onChange = { enabled ->
                    if (enabled && needsNotificationPermission(context)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setRemindersEnabled(enabled)
                    }
                }
            )
            if (state.permissionDenied) {
                Text(
                    "Без разрешения на уведомления напоминание не придёт. Его можно выдать в настройках системы.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorSoft
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Время", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    "%02d:%02d".format(settings.reminderHour, settings.reminderMinute),
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan
                )
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("План", style = MaterialTheme.typography.titleMedium)
            Text(
                "Ответьте на 4 вопроса заново — 7-дневный план начнётся с первого дня. История и галерея сохранятся.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            OutlinedButton(onClick = onRebuildPlan, modifier = Modifier.fillMaxWidth()) { Text("Перестроить план") }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Профиль", style = MaterialTheme.typography.titleMedium)
            Text("${state.name} · ${state.email}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            OutlinedButton(
                onClick = viewModel::logout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorSoft),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Выйти") }
        }

        Text(
            "AuraFlow ${BuildConfig.VERSION_NAME} · «Мысль дня» — коллекция из 100 высказываний, " +
                "загружается из репозитория проекта и хранится на устройстве.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = SurfaceGlassHigh,
            title = { Text("Время напоминания") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReminderTime(pickerState.hour, pickerState.minute)
                    showTimePicker = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Night, checkedTrackColor = NeonCyan)
        )
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
