package com.auraflow.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.auraflow.appContainer
import com.auraflow.data.repository.SessionRepository
import com.auraflow.data.repository.UserRepository
import com.auraflow.domain.model.Session
import com.auraflow.domain.stats.PracticeStats
import com.auraflow.domain.stats.StatsCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class ProfileUiState(
    val name: String = "",
    val stats: PracticeStats = PracticeStats.EMPTY,
    val sessions: List<Session> = emptyList()
)

class ProfileViewModel(users: UserRepository, sessions: SessionRepository) : ViewModel() {
    val state: StateFlow<ProfileUiState> = combine(users.profile, sessions.sessions) { profile, list ->
        val zone = ZoneId.systemDefault()
        ProfileUiState(
            name = profile?.name.orEmpty(),
            stats = StatsCalculator.stats(list, LocalDate.now(zone), zone),
            sessions = list
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ProfileViewModel(c.userRepository, c.sessionRepository)
            }
        }
    }
}
