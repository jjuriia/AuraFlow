package com.auraflow.domain

import com.auraflow.domain.model.ChladniPattern
import com.auraflow.domain.model.Session
import com.auraflow.domain.model.isCompleted
import com.auraflow.domain.plan.PlanGenerator
import com.auraflow.domain.stats.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class PlanCalendarTest {

    private val zone = ZoneOffset.UTC
    private val start = LocalDate.of(2026, 10, 20)

    private fun session(date: LocalDate, planDay: Int?, minutes: Int = 5, completed: Boolean = true) = Session(
        startedAt = date.atTime(20, 0).toInstant(zone).toEpochMilli(),
        durationSec = minutes * 60,
        planDay = planDay,
        attentionReturns = 0,
        moodBefore = 3,
        moodAfter = 4,
        pattern = ChladniPattern(1, 4, 0),
        completed = completed
    )

    private fun todayDay(sessions: List<Session>, today: LocalDate): Int {
        val completed = StatsCalculator.completedPlanDays(sessions, start.toEpochDay(), zone)
        return PlanGenerator.today(completed, StatsCalculator.planDayDoneOn(today, sessions, start.toEpochDay(), zone))
    }

    @Test
    fun repeatingPracticeTheSameDayDoesNotAdvancePlan() {
        val sessions = List(3) { session(start, planDay = 1) }
        assertEquals(1, todayDay(sessions, start))
        assertEquals(2, todayDay(sessions, start.plusDays(1)))
    }

    @Test
    fun missedDaysDoNotSkipPlan() {
        val sessions = listOf(session(start, 1), session(start.plusDays(1), 2))
        assertEquals(3, todayDay(sessions, start.plusDays(5)))
        assertNull(StatsCalculator.planDayDoneOn(start.plusDays(5), sessions, start.toEpochDay(), zone))
    }

    @Test
    fun interruptedSessionDoesNotCompletePlanDay() {
        val sessions = listOf(session(start, planDay = 1, minutes = 2, completed = false))
        assertTrue(StatsCalculator.completedPlanDays(sessions, start.toEpochDay(), zone).isEmpty())
        assertEquals(1, todayDay(sessions, start.plusDays(1)))
    }

    @Test
    fun completionNeedsEightyPercent() {
        assertTrue(isCompleted(durationSec = 240, plannedSec = 300))
        assertFalse(isCompleted(durationSec = 239, plannedSec = 300))
    }

    @Test
    fun interruptedSessionGivesOnlyMinutes() {
        assertEquals(12, StatsCalculator.pointsFor(durationSec = 120, completed = true))
        assertEquals(2, StatsCalculator.pointsFor(durationSec = 120, completed = false))
        val stats = StatsCalculator.stats(
            listOf(session(start, 1, minutes = 5), session(start, null, minutes = 3, completed = false)),
            start,
            zone
        )
        assertEquals(1, stats.completedSessions)
        assertEquals(10 + 8, StatsCalculator.points(stats))
    }
}
