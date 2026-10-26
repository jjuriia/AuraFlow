package com.auraflow.domain

import com.auraflow.domain.chladni.ChladniField
import com.auraflow.domain.chladni.SandSimulation
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChladniPhysicsTest {
    @Test
    fun diagonalIsNodalLineForEveryMode() {
        for ((n, m) in ChladniField.MODES) {
            for (i in 0..100) {
                val t = i / 100f
                assertEquals("мода ($n,$m), точка $t", 0f, ChladniField.amplitude(n, m, t, t), 1e-5f)
            }
        }
    }

    @Test
    fun fieldIsAntisymmetric() {
        val (n, m) = 3 to 7
        for (i in 0..20) for (j in 0..20) {
            val x = i / 20f
            val y = j / 20f
            assertEquals(-ChladniField.amplitude(n, m, y, x), ChladniField.amplitude(n, m, x, y), 1e-5f)
        }
    }

    @Test
    fun analyticGradientMatchesFiniteDifference() {
        val h = 1e-3f
        for ((n, m) in ChladniField.MODES) {
            val x = 0.37f
            val y = 0.81f
            val numX = (ChladniField.amplitude(n, m, x + h, y) - ChladniField.amplitude(n, m, x - h, y)) / (2 * h)
            val numY = (ChladniField.amplitude(n, m, x, y + h) - ChladniField.amplitude(n, m, x, y - h)) / (2 * h)
            assertEquals(numX, ChladniField.gradientX(n, m, x, y), 0.05f)
            assertEquals(numY, ChladniField.gradientY(n, m, x, y), 0.05f)
        }
    }

    @Test
    fun sandSettlesOnNodalLinesWhilePlateSounds() {
        val sim = SandSimulation(count = 800, seed = 42L).apply { setMode(2, 5) }
        val before = sim.meanAbsAmplitude()
        repeat(600) { sim.step(dt = 1f / 60f, energy = 1f) }
        val after = sim.meanAbsAmplitude()
        assertTrue("узор не проявился: было $before, стало $after", after < before * 0.25f)
    }

    @Test
    fun sandStaysStillWhenPlateIsSilent() {
        val sim = SandSimulation(count = 200, seed = 7L)
        val x = sim.x.copyOf()
        val y = sim.y.copyOf()
        repeat(100) { sim.step(dt = 1f / 60f, energy = 0f) }
        assertArrayEquals(x, sim.x, 0f)
        assertArrayEquals(y, sim.y, 0f)
    }

    @Test
    fun touchPushesSandAwayFromFinger() {
        val sim = SandSimulation(count = 500, seed = 1L)
        val nearBefore = (0 until sim.count).count { isNear(sim, it) }
        sim.disturb(cx = 0.5f, cy = 0.5f, radius = 0.2f, strength = 0.15f)
        val nearAfter = (0 until sim.count).count { isNear(sim, it) }
        assertTrue("песок не разлетелся: было $nearBefore, стало $nearAfter", nearAfter < nearBefore)
    }

    private fun isNear(sim: SandSimulation, i: Int): Boolean {
        val dx = sim.x[i] - 0.5f
        val dy = sim.y[i] - 0.5f
        return dx * dx + dy * dy < 0.1f * 0.1f
    }
}
