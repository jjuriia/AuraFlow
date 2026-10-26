package com.auraflow.domain.chladni

import kotlin.math.exp

class PlateEngine(particles: Int, seed: Long) {
    val sand = SandSimulation(particles, seed)

    var energy: Float = 0f
        private set

    private var tau = 3.5f

    fun setMode(n: Int, m: Int) = sand.setMode(n, m)

    fun strike(intensity: Float, decaySeconds: Float) {
        energy = (energy + intensity).coerceIn(0f, 1f)
        tau = decaySeconds
    }

    fun touch(x: Float, y: Float) = sand.disturb(x, y, TOUCH_RADIUS, TOUCH_STRENGTH)

    fun inhale() = sand.agitate(INHALE_AGITATION)

    fun update(dt: Float) {
        if (energy < MIN_ENERGY) {
            energy = 0f
            return
        }
        sand.step(dt, energy)
        energy *= exp(-dt / tau)
    }

    private companion object {
        const val TOUCH_RADIUS = 0.18f
        const val TOUCH_STRENGTH = 0.12f
        const val INHALE_AGITATION = 0.025f
        const val MIN_ENERGY = 0.01f
    }
}
