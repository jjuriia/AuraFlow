package com.auraflow.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.auraflow.appContainer
import com.auraflow.data.repository.QuoteRepository
import com.auraflow.data.repository.UserRepository
import com.auraflow.ui.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SplashViewModel(
    private val users: UserRepository,
    private val quotes: QuoteRepository
) : ViewModel() {
    private val _destination = MutableStateFlow<String?>(null)
    val destination: StateFlow<String?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val minimumShow = launch { delay(MIN_SHOW_MS) }

            withTimeoutOrNull(SYNC_TIMEOUT_MS) { quotes.sync() }
            val profile = users.profile.first()
            val loggedIn = users.isLoggedIn.first()
            minimumShow.join()
            _destination.value = when {
                profile == null -> Routes.onboarding(rebuild = false)
                !loggedIn -> Routes.LOGIN
                else -> Routes.HOME
            }
        }
    }

    companion object {
        private const val MIN_SHOW_MS = 1_800L
        private const val SYNC_TIMEOUT_MS = 3_000L

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SplashViewModel(container.userRepository, container.quoteRepository)
            }
        }
    }
}
