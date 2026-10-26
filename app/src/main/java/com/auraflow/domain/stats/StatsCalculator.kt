package com.auraflow.domain.stats

import com.auraflow.domain.model.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PracticeStats(
    val totalSessions: Int,
    val totalMinutes: Int,

    val daysThisWeek: Int,

    val averageMoodDelta: Double?,
    val totalReturns: Int
) {
    companion object {
        val EMPTY = PracticeStats(0, 0, 0, null, 0)
    }
}

data class SpiritState(
    val brightness: Float,
    val petals: Int,
    val particles: Int,
    val growth: Float,
    val title: String
)

object StatsCalculator {
    fun stats(sessions: List<Session>, today: LocalDate, zone: ZoneId): PracticeStats {
        val weekStart = today.minusDays(6)
        val days = sessions
            .map { dateOf(it, zone) }
            .filter { !it.isBefore(weekStart) && !it.isAfter(today) }
            .toSet()
        return PracticeStats(
            totalSessions = sessions.size,
            totalMinutes = sessions.sumOf { it.durationSec } / 60,
            daysThisWeek = days.size,
            averageMoodDelta = sessions.takeIf { it.isNotEmpty() }?.map { (it.moodAfter - it.moodBefore).toDouble() }?.average(),
            totalReturns = sessions.sumOf { it.attentionReturns }
        )
    }

    fun completedPlanDays(sessions: List<Session>, planStartEpochDay: Long, zone: ZoneId): Set<Int> =
        sessions
            .filter { it.planDay != null && dateOf(it, zone).toEpochDay() >= planStartEpochDay }
            .mapNotNull { it.planDay }
            .toSet()

    fun spirit(stats: PracticeStats): SpiritState = SpiritState(
        brightness = 0.3f + 0.7f * stats.daysThisWeek / 7f,
        petals = (3 + stats.totalMinutes / 15).coerceAtMost(12),
        particles = (8 + stats.totalSessions * 3).coerceAtMost(60),
        growth = (stats.totalMinutes / 300f).coerceAtMost(1f),
        title = when (stats.daysThisWeek) {
            0 -> "Дух дремлет"
            1, 2 -> "Дух просыпается"
            3, 4, 5 -> "Дух светится"
            else -> "Дух сияет"
        }
    )

    private fun dateOf(session: Session, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
}
