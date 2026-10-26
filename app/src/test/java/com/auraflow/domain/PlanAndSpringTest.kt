package com.auraflow.domain

import com.auraflow.domain.auth.PasswordHasher
import com.auraflow.domain.chladni.PlateEngine
import com.auraflow.domain.model.DayPart
import com.auraflow.domain.model.Experience
import com.auraflow.domain.model.Goal
import com.auraflow.domain.model.OnboardingAnswers
import com.auraflow.domain.physics.Spring
import com.auraflow.domain.plan.PlanGenerator
import com.auraflow.domain.plan.PracticeStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.E
import kotlin.math.abs

class PlanAndSpringTest {
    private val novice = OnboardingAnswers(Goal.STRESS, Experience.NEVER, minutes = 15, dayPart = DayPart.EVENING)

    @Test
    fun planRampsFromStartToChosenDuration() {
        val plan = PlanGenerator.generate(novice)
        assertEquals(7, plan.size)
        assertEquals(5, plan.first().minutes)
        assertEquals(15, plan.last().minutes)
        plan.zipWithNext().forEach { (a, b) -> assertTrue("длительность не должна убывать", b.minutes >= a.minutes) }
    }

    @Test
    fun stageSecondsAddUpToPlannedMinutes() {
        for (goal in Goal.entries) for (experience in Experience.entries) for (minutes in listOf(5, 10, 15)) {
            PlanGenerator.generate(OnboardingAnswers(goal, experience, minutes, DayPart.MORNING)).forEach { day ->
                assertEquals("$goal/$experience/$minutes, день ${day.day}", day.minutes * 60, day.totalSeconds)
                assertTrue(day.stages.all { it.seconds > 0 })
            }
        }
    }

    @Test
    fun stagesAppearAccordingToAnswers() {
        val firstDay = PlanGenerator.day(novice, 1).stages.map { it.stage }
        assertFalse("новичку объект внимания — со 2-го дня", PracticeStage.ANCHOR in firstDay)
        val sleeper = novice.copy(goal = Goal.SLEEP)
        assertTrue(PracticeStage.BODY_SCAN in PlanGenerator.day(sleeper, 3).stages.map { it.stage })
        assertFalse(PracticeStage.BODY_SCAN in PlanGenerator.day(novice, 3).stages.map { it.stage })
    }

    @Test
    fun nextDayIsFirstUncompleted() {
        assertEquals(1, PlanGenerator.nextDay(emptySet()))
        assertEquals(3, PlanGenerator.nextDay(setOf(1, 2, 5)))
        assertEquals(7, PlanGenerator.nextDay((1..7).toSet()))
        assertTrue(PlanGenerator.isCompleted((1..7).toSet()))
    }

    @Test
    fun underdampedSpringOvershootsThenSettles() {
        val spring = Spring(position = 0f).apply { target = 1f }
        assertTrue(spring.dampingRatio < 1f)
        var maxPosition = 0f
        repeat(180) {
            spring.step(1f / 60f)
            maxPosition = maxOf(maxPosition, spring.position)
        }
        assertTrue("нет перелёта: максимум $maxPosition", maxPosition > 1.05f)
        assertTrue("не успокоилась: ${spring.position}", spring.isAtRest(1e-2f))
    }

    @Test
    fun plateEnergyDecaysLikeBowlEnvelope() {
        val engine = PlateEngine(particles = 10, seed = 3L)
        engine.strike(intensity = 1f, decaySeconds = 2f)
        val steps = 120
        repeat(steps) { engine.update(1f / 60f) }
        assertTrue("энергия ${engine.energy}, ожидалось 1/e", abs(engine.energy - (1 / E).toFloat()) < 1e-3f)
    }

    @Test
    fun passwordHashVerifiesOnlyCorrectPassword() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("sound2026", salt)
        assertTrue(PasswordHasher.verify("sound2026", salt, hash))
        assertFalse(PasswordHasher.verify("sound2027", salt, hash))
        assertFalse("соль должна менять хеш", hash == PasswordHasher.hash("sound2026", PasswordHasher.newSalt()))
    }
}
