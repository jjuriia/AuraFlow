package com.auraflow.domain.chladni

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class SandSimulation(val count: Int, seed: Long) {
    val x = FloatArray(count)
    val y = FloatArray(count)

    var n = 1
        private set
    var m = 4
        private set

    private val random = Random(seed)

    init {
        scatter()
    }

    fun setMode(n: Int, m: Int) {
        require(n > 0 && m > 0 && n != m) { "Моды должны быть положительными и различными: n=$n, m=$m" }
        this.n = n
        this.m = m
    }

    fun scatter() {
        for (i in 0 until count) {
            x[i] = random.nextFloat()
            y[i] = random.nextFloat()
        }
    }

    fun step(dt: Float, energy: Float) {
        if (energy <= 0f) return
        val k = energy * dt
        for (i in 0 until count) {
            val a = ChladniField.amplitude(n, m, x[i], y[i])
            val gx = ChladniField.gradientX(n, m, x[i], y[i])
            val gy = ChladniField.gradientY(n, m, x[i], y[i])
            val shake = SHAKE * abs(a)
            val dx = k * (-PULL * a * gx + shake * (random.nextFloat() * 2f - 1f))
            val dy = k * (-PULL * a * gy + shake * (random.nextFloat() * 2f - 1f))
            x[i] = (x[i] + dx.coerceIn(-MAX_STEP, MAX_STEP)).coerceIn(0f, 1f)
            y[i] = (y[i] + dy.coerceIn(-MAX_STEP, MAX_STEP)).coerceIn(0f, 1f)
        }
    }

    fun disturb(cx: Float, cy: Float, radius: Float, strength: Float) {
        for (i in 0 until count) {
            var dx = x[i] - cx
            var dy = y[i] - cy
            var d = sqrt(dx * dx + dy * dy)
            if (d >= radius) continue
            if (d < 1e-6f) {
                dx = random.nextFloat() * 2f - 1f
                dy = random.nextFloat() * 2f - 1f
                d = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-6f)
            }
            val push = strength * (1f - d / radius)
            x[i] = (x[i] + dx / d * push).coerceIn(0f, 1f)
            y[i] = (y[i] + dy / d * push).coerceIn(0f, 1f)
        }
    }

    fun agitate(strength: Float) {
        for (i in 0 until count) {
            x[i] = (x[i] + (random.nextFloat() * 2f - 1f) * strength).coerceIn(0f, 1f)
            y[i] = (y[i] + (random.nextFloat() * 2f - 1f) * strength).coerceIn(0f, 1f)
        }
    }

    fun meanAbsAmplitude(): Float {
        var sum = 0f
        for (i in 0 until count) sum += abs(ChladniField.amplitude(n, m, x[i], y[i]))
        return sum / count
    }

    companion object {
        const val PULL = 0.004f

        const val SHAKE = 0.6f

        const val MAX_STEP = 0.02f
    }
}
