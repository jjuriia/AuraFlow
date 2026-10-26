package com.auraflow.domain.chladni

import com.auraflow.domain.model.ChladniPattern
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object ChladniField {
    val MODES: List<Pair<Int, Int>> = listOf(
        1 to 4, 2 to 5, 1 to 6, 3 to 5, 2 to 7, 3 to 7, 4 to 7, 5 to 8
    )

    fun patternFor(durationSec: Int, attentionReturns: Int, moodDelta: Int, seed: Long): ChladniPattern {
        val n = 1 + Math.floorMod(durationSec / 60 + attentionReturns, 6)
        val m = n + 1 + Math.floorMod(attentionReturns + moodDelta + (seed % 6).toInt(), 6)
        return ChladniPattern(n, m, seed)
    }

    fun sampleNodalPoints(n: Int, m: Int, seed: Long, count: Int, tolerance: Float = 0.06f): FloatArray {
        val random = Random(seed)
        val out = FloatArray(count * 2)
        var found = 0
        var tries = 0
        while (found < count && tries < count * MAX_TRIES_PER_POINT) {
            tries++
            val x = random.nextFloat()
            val y = random.nextFloat()
            if (abs(amplitude(n, m, x, y)) < tolerance) {
                out[2 * found] = x
                out[2 * found + 1] = y
                found++
            }
        }
        return out.copyOf(found * 2)
    }

    private const val MAX_TRIES_PER_POINT = 60

    fun amplitude(n: Int, m: Int, x: Float, y: Float): Float {
        val px = PI * x
        val py = PI * y
        return (cos(n * px) * cos(m * py) - cos(m * px) * cos(n * py)).toFloat()
    }

    fun gradientX(n: Int, m: Int, x: Float, y: Float): Float {
        val px = PI * x
        val py = PI * y
        return (PI * (-n * sin(n * px) * cos(m * py) + m * sin(m * px) * cos(n * py))).toFloat()
    }

    fun gradientY(n: Int, m: Int, x: Float, y: Float): Float {
        val px = PI * x
        val py = PI * y
        return (PI * (-m * cos(n * px) * sin(m * py) + n * cos(m * px) * sin(n * py))).toFloat()
    }
}
