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

    fun points(stats: PracticeStats): Int = stats.totalSessions * POINTS_PER_SESSION + stats.totalMinutes

    fun pointsFor(durationSec: Int): Int = POINTS_PER_SESSION + durationSec / 60

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
            particles = (6 + stats.totalSessions * 2).coerceAtMost(60),
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
