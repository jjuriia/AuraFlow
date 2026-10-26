package com.auraflow.domain.stats

import com.auraflow.domain.model.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PracticeStats(
    val totalSessions: Int,
    val completedSessions: Int,
    val totalMinutes: Int,
    val daysThisWeek: Int,
    val averageMoodDelta: Double?,
    val totalReturns: Int
) {
    companion object {
        val EMPTY = PracticeStats(0, 0, 0, 0, null, 0)
    }
}

data class SpiritState(
    val level: Int,
    val levelName: String,
    val points: Int,
    val levelProgress: Float,
    val pointsToNext: Int?,
    val nextLevelName: String?,
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
            completedSessions = sessions.count { it.completed },
            totalMinutes = sessions.sumOf { it.durationSec } / 60,
            daysThisWeek = days.size,
            averageMoodDelta = sessions.takeIf { it.isNotEmpty() }?.map { (it.moodAfter - it.moodBefore).toDouble() }?.average(),
            totalReturns = sessions.sumOf { it.attentionReturns }
        )
    }

    fun completedPlanDays(sessions: List<Session>, planStartEpochDay: Long, zone: ZoneId): Set<Int> =
        planSessions(sessions, planStartEpochDay, zone).mapNotNull { it.planDay }.toSet()

    fun planDayDoneOn(date: LocalDate, sessions: List<Session>, planStartEpochDay: Long, zone: ZoneId): Int? =
        planSessions(sessions, planStartEpochDay, zone).filter { dateOf(it, zone) == date }.maxOfOrNull { it.planDay!! }

    private fun planSessions(sessions: List<Session>, planStartEpochDay: Long, zone: ZoneId) =
        sessions.filter { it.completed && it.planDay != null && dateOf(it, zone).toEpochDay() >= planStartEpochDay }

    val LEVELS = listOf(
        0 to "Искра",
        15 to "Огонёк",
        35 to "Росток",
        60 to "Бутон",
        100 to "Цветок",
        150 to "Сияние",
        220 to "Звезда",
        300 to "Созвездие",
        400 to "Туманность",
        520 to "Галактика"
    )

    const val POINTS_PER_SESSION = 10

    fun points(stats: PracticeStats): Int = stats.completedSessions * POINTS_PER_SESSION + stats.totalMinutes

    fun pointsFor(durationSec: Int, completed: Boolean): Int = (if (completed) POINTS_PER_SESSION else 0) + durationSec / 60

    fun spirit(stats: PracticeStats): SpiritState {
        val points = points(stats)
        val index = LEVELS.indexOfLast { points >= it.first }
        val next = LEVELS.getOrNull(index + 1)
        val progress = next?.let { (points - LEVELS[index].first).toFloat() / (it.first - LEVELS[index].first) } ?: 1f
        return SpiritState(
            level = index + 1,
            levelName = LEVELS[index].second,
            points = points,
            levelProgress = progress,
            pointsToNext = next?.let { it.first - points },
            nextLevelName = next?.second,
            brightness = 0.3f + 0.7f * stats.daysThisWeek / 7f,
            petals = 3 + index,
            particles = (6 + stats.completedSessions * 2).coerceAtMost(60),
            growth = ((index + progress) / (LEVELS.size - 1)).coerceAtMost(1f),
            title = when (stats.daysThisWeek) {
                0 -> "Дух дремлет"
                1, 2 -> "Дух просыпается"
                3, 4, 5 -> "Дух светится"
                else -> "Дух сияет"
            }
        )
    }

    private fun dateOf(session: Session, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
}
