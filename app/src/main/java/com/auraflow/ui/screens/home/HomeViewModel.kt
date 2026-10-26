package com.auraflow.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.auraflow.appContainer
import com.auraflow.data.repository.QuoteRepository
import com.auraflow.data.repository.SessionRepository
import com.auraflow.data.repository.UserRepository
import com.auraflow.domain.model.Quote
import com.auraflow.domain.plan.PlanDay
import com.auraflow.domain.plan.PlanGenerator
import com.auraflow.domain.stats.PracticeStats
import com.auraflow.domain.stats.SpiritState
import com.auraflow.domain.stats.StatsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

data class HomeUiState(
    val loading: Boolean = true,
    val name: String = "",
    val greeting: String = "",
    val stats: PracticeStats = PracticeStats.EMPTY,
    val spirit: SpiritState = StatsCalculator.spirit(PracticeStats.EMPTY),
    val today: PlanDay? = null,
    val completedDays: Set<Int> = emptySet(),
    val planCompleted: Boolean = false,
    val quote: Quote? = null
)

class HomeViewModel(
    users: UserRepository,
    sessions: SessionRepository,
    private val quotes: QuoteRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {
    private val quote = MutableStateFlow<Quote?>(null)

    val state: StateFlow<HomeUiState> = combine(users.profile.filterNotNull(), sessions.sessions, quote) { profile, list, q ->
        val zone = clock.zone
        val stats = StatsCalculator.stats(list, LocalDate.now(clock), zone)
        val completed = StatsCalculator.completedPlanDays(list, profile.planStartEpochDay, zone)
        HomeUiState(
            loading = false,
            name = profile.name,
            greeting = greeting(LocalTime.now(clock)),
            stats = stats,
            spirit = StatsCalculator.spirit(stats),
            today = PlanGenerator.day(profile.answers, PlanGenerator.nextDay(completed)),
            completedDays = completed,
            planCompleted = PlanGenerator.isCompleted(completed),
            quote = q
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            val day = LocalDate.now(clock).toEpochDay()

            quote.value = quotes.quoteForDay(day) ?: run {
                quotes.sync()
                quotes.quoteForDay(day)
            }
        }
    }

    private fun greeting(time: LocalTime): String = when (time.hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        in 18..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                HomeViewModel(c.userRepository, c.sessionRepository, c.quoteRepository)
            }
        }
    }
}
