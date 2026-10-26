package com.auraflow.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.auraflow.appContainer
import com.auraflow.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val error: String? = null,
    val busy: Boolean = false,
    val done: Boolean = false
)

class LoginViewModel(private val users: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun login() {
        val s = _state.value
        if (s.busy) return
        if (s.email.isBlank() || s.password.isEmpty()) {
            _state.update { it.copy(error = "Введите email и пароль") }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val error = when {
                !users.hasProfile() -> "На этом устройстве ещё нет профиля — создайте его"
                users.login(s.email, s.password) -> null
                else -> "Неверный email или пароль"
            }
            _state.update { it.copy(busy = false, error = error, done = error == null, password = "") }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { LoginViewModel(appContainer().userRepository) }
        }
    }
}
