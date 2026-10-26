package com.auraflow.domain

import com.auraflow.domain.stats.PracticeStats
import com.auraflow.domain.stats.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpiritLevelTest {

    private fun stats(sessions: Int, minutes: Int, days: Int = 1) = PracticeStats(sessions, minutes, days, null, 0)

    @Test
    fun everySessionMovesSpiritForward() {
        val states = (0..4).map { StatsCalculator.spirit(stats(sessions = it, minutes = it * 5)) }
        assertEquals(1, states[0].level)
        assertEquals("одна пятиминутная сессия даёт новый уровень", 2, states[1].level)
        states.zipWithNext().forEach { (before, after) ->
            assertTrue(after.points > before.points)
            assertTrue(after.particles > before.particles)
            assertTrue(after.level > before.level || after.levelProgress > before.levelProgress)
        }
    }

    @Test
    fun petalsGrowWithLevel() {
        val first = StatsCalculator.spirit(stats(0, 0))
        val third = StatsCalculator.spirit(stats(3, 15))
        assertEquals(3, first.petals)
        assertEquals(third.level + 2, third.petals)
    }

    @Test
    fun highestLevelHasNoNextStep() {
        val top = StatsCalculator.spirit(stats(sessions = 60, minutes = 900))
        assertEquals(StatsCalculator.LEVELS.size, top.level)
        assertNull(top.pointsToNext)
        assertEquals(1f, top.growth, 0f)
    }

    @Test
    fun regularityControlsBrightnessNotLevel() {
        val rare = StatsCalculator.spirit(stats(5, 50, days = 1))
        val regular = StatsCalculator.spirit(stats(5, 50, days = 7))
        assertEquals(rare.level, regular.level)
        assertTrue(regular.brightness > rare.brightness)
    }
}
