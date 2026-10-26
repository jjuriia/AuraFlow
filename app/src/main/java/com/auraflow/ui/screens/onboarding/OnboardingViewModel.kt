package com.auraflow.ui.screens.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.auraflow.appContainer
import com.auraflow.data.repository.UserRepository
import com.auraflow.domain.auth.CredentialsValidator
import com.auraflow.domain.model.DayPart
import com.auraflow.domain.model.Experience
import com.auraflow.domain.model.Goal
import com.auraflow.domain.model.OnboardingAnswers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OnboardingUiState(
    val rebuild: Boolean = false,
    val step: Int = 0,
    val goal: Goal? = null,
    val experience: Experience? = null,
    val minutes: Int? = null,
    val dayPart: DayPart? = null,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmationError: String? = null,
    val busy: Boolean = false,
    val done: Boolean = false
) {
    val answers: OnboardingAnswers?
        get() = if (goal != null && experience != null && minutes != null && dayPart != null) {
            OnboardingAnswers(goal, experience, minutes, dayPart)
        } else null

    val totalSteps: Int get() = if (rebuild) 5 else 6
}

class OnboardingViewModel(
    savedStateHandle: SavedStateHandle,
    private val users: UserRepository
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState(rebuild = savedStateHandle.get<Boolean>("rebuild") ?: false))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun selectGoal(goal: Goal) = _state.update { it.copy(goal = goal, step = 1) }
    fun selectExperience(experience: Experience) = _state.update { it.copy(experience = experience, step = 2) }
    fun selectMinutes(minutes: Int) = _state.update { it.copy(minutes = minutes, step = 3) }
    fun selectDayPart(dayPart: DayPart) = _state.update { it.copy(dayPart = dayPart, step = 4) }

    fun back() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun confirmPlan() {
        val current = _state.value
        val answers = current.answers ?: return
        if (!current.rebuild) {
            _state.update { it.copy(step = 5) }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            users.rebuildPlan(answers, LocalDate.now())
            _state.update { it.copy(busy = false, done = true) }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null) }
    fun onConfirmationChange(value: String) = _state.update { it.copy(confirmation = value, confirmationError = null) }

    fun register() {
        val s = _state.value
        val answers = s.answers ?: return
        if (s.busy) return
        val checked = s.copy(
            nameError = CredentialsValidator.name(s.name),
            emailError = CredentialsValidator.email(s.email),
            passwordError = CredentialsValidator.password(s.password),
            confirmationError = CredentialsValidator.confirmation(s.password, s.confirmation)
        )
        if (listOf(checked.nameError, checked.emailError, checked.passwordError, checked.confirmationError).any { it != null }) {
            _state.value = checked
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            users.register(s.name, s.email, s.password, answers, LocalDate.now())
            _state.update { it.copy(busy = false, done = true, password = "", confirmation = "") }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                OnboardingViewModel(createSavedStateHandle(), appContainer().userRepository)
            }
        }
    }
}
