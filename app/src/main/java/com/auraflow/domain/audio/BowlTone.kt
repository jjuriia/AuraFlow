package com.auraflow.domain.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

data class BowlTone(
    val fundamentalHz: Double,

    val decaySeconds: Double = 3.5
) {
    data class Partial(val ratio: Double, val gain: Double, val decayFactor: Double)

    fun energyAt(t: Double): Double = envelope(t, decaySeconds)

    fun ringSeconds(): Double = timeToLevel(decaySeconds, SILENCE_DB)

    fun render(sampleRate: Int, durationSec: Double = ringSeconds()): FloatArray {
        val total = (sampleRate * durationSec).toInt()
        val norm = PARTIALS.sumOf { it.gain }
        val out = FloatArray(total)
        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate
            var s = 0.0
            for (p in PARTIALS) {
                val f = fundamentalHz * p.ratio
                val env = envelope(t, decaySeconds * p.decayFactor)
                s += p.gain * env * 0.5 * (sin(2 * PI * f * t) + sin(2 * PI * (f + BEAT_HZ) * t))
            }
            val attack = min(1.0, t / ATTACK_SECONDS)
            out[i] = (s / norm * attack).toFloat()
        }
        return out
    }

    companion object {
        val PARTIALS = listOf(
            Partial(ratio = 1.0, gain = 1.0, decayFactor = 1.0),
            Partial(ratio = 2.76, gain = 0.55, decayFactor = 0.62),
            Partial(ratio = 5.40, gain = 0.30, decayFactor = 0.40),
            Partial(ratio = 8.93, gain = 0.15, decayFactor = 0.27)
        )

        const val BEAT_HZ = 0.9

        const val ATTACK_SECONDS = 0.005

        const val SILENCE_DB = -60.0

        val NOTES_HZ = listOf(196.0, 220.0, 246.9, 261.6, 293.7, 329.6, 349.2, 392.0)

        fun envelope(t: Double, tau: Double): Double = exp(-t / tau)

        fun timeToLevel(tau: Double, levelDb: Double): Double = tau * ln(10.0.pow(-levelDb / 20.0))
    }
}
