package com.auraflow.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.auraflow.appContainer
import com.auraflow.data.audio.Narrator
import com.auraflow.data.reminder.ReminderScheduler
import com.auraflow.data.repository.UserRepository
import com.auraflow.domain.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings? = null,
    val name: String = "",
    val email: String = "",
    val narratorStatus: Narrator.Status = Narrator.Status.OFF,
    val permissionDenied: Boolean = false,
    val loggedOut: Boolean = false
)

private data class LocalFlags(val permissionDenied: Boolean = false, val loggedOut: Boolean = false)

class SettingsViewModel(
    private val users: UserRepository,
    private val scheduler: ReminderScheduler,
    private val narrator: Narrator
) : ViewModel() {
    private val flags = MutableStateFlow(LocalFlags())
    private var narratorOpened = false

    val state: StateFlow<SettingsUiState> =
        combine(users.settings, users.profile, narrator.status, flags) { settings, profile, status, local ->
            SettingsUiState(
                settings = settings,
                name = profile?.name.orEmpty(),
                email = profile?.email.orEmpty(),
                narratorStatus = status,
                permissionDenied = local.permissionDenied,
                loggedOut = local.loggedOut
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch { if (users.settings.first().ttsEnabled) openNarrator() }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch { users.setTtsEnabled(enabled) }
        if (enabled) openNarrator() else closeNarrator()
    }

    fun testVoice() = narrator.speak("Вдох… и медленный, спокойный выдох.")

    fun setRemindersEnabled(enabled: Boolean) {
        flags.update { it.copy(permissionDenied = false) }
        viewModelScope.launch {
            val s = users.settings.first()
            users.setReminder(enabled, s.reminderHour, s.reminderMinute)
            if (enabled) scheduler.schedule(s.reminderHour, s.reminderMinute) else scheduler.cancel()
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        if (granted) setRemindersEnabled(true) else flags.update { it.copy(permissionDenied = true) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val s = users.settings.first()
            users.setReminder(s.remindersEnabled, hour, minute)
            if (s.remindersEnabled) scheduler.schedule(hour, minute)
        }
    }

    fun logout() {
        viewModelScope.launch {
            users.logout()
            flags.update { it.copy(loggedOut = true) }
        }
    }

    private fun openNarrator() {
        if (narratorOpened) return
        narrator.open()
        narratorOpened = true
    }

    private fun closeNarrator() {
        if (!narratorOpened) return
        narrator.close()
        narratorOpened = false
    }

    override fun onCleared() = closeNarrator()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                SettingsViewModel(c.userRepository, c.reminderScheduler, c.narrator)
            }
        }
    }
}
