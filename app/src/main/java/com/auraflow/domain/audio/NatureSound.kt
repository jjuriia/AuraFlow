package com.auraflow.domain.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class NatureSound(val title: String) {
    RAIN("Дождь"),
    SEA("Прибой"),
    WIND("Ветер")
}

object NatureSoundSynth {
    const val LOOP_SECONDS = 16
    const val WAVE_PERIOD_SECONDS = 8.0
    const val TARGET_RMS = 0.2f
    private const val CROSSFADE_SECONDS = 1

    fun render(sound: NatureSound, sampleRate: Int, seed: Long = 7L): FloatArray {
        val loop = LOOP_SECONDS * sampleRate
        val fade = CROSSFADE_SECONDS * sampleRate
        val raw = FloatArray(loop + fade)
        val random = Random(seed)
        when (sound) {
            NatureSound.RAIN -> rain(raw, sampleRate, random)
            NatureSound.SEA -> sea(raw, sampleRate, random)
            NatureSound.WIND -> wind(raw, sampleRate, random)
        }
        val out = FloatArray(loop) { i ->
            if (i < fade) {
                val k = i.toFloat() / fade
                raw[i] * sqrt(k) + raw[loop + i] * sqrt(1f - k)
            } else {
                raw[i]
            }
        }
        normalize(out)
        return out
    }

    fun waveEnvelope(t: Double): Double = 0.25 + 0.75 * ((1 - cos(2 * PI * t / WAVE_PERIOD_SECONDS)) / 2).pow(2)

    private fun rain(out: FloatArray, sampleRate: Int, random: Random) {
        var low = 0f
        var drop = 0f
        var dropDecay = 0f
        for (i in out.indices) {
            val t = i.toDouble() / sampleRate
            val white = random.nextFloat() * 2f - 1f
            low += 0.08f * (white - low)
            val hiss = (white - low) * 0.35f
            if (random.nextFloat() < 25f / sampleRate) {
                drop = 0.3f + 0.7f * random.nextFloat()
                dropDecay = exp(-1f / (sampleRate * (0.002f + 0.006f * random.nextFloat())))
            }
            drop *= dropDecay
            val intensity = 0.85f + 0.15f * sin(2 * PI * t / LOOP_SECONDS).toFloat()
            out[i] = (hiss + drop * (random.nextFloat() * 2f - 1f)) * intensity
        }
    }

    private fun sea(out: FloatArray, sampleRate: Int, random: Random) {
        var brown = 0f
        var low = 0f
        for (i in out.indices) {
            val t = i.toDouble() / sampleRate
            val white = random.nextFloat() * 2f - 1f
            brown = 0.995f * brown + 0.02f * white
            low += 0.1f * (white - low)
            val env = waveEnvelope(t).toFloat()
            val foam = (white - low) * env * env * env * 0.25f
            out[i] = brown * env + foam
        }
    }

    private fun wind(out: FloatArray, sampleRate: Int, random: Random) {
        var lp1 = 0f
        var lp2 = 0f
        for (i in out.indices) {
            val t = i.toDouble() / sampleRate
            val gust = (0.5 + 0.3 * sin(2 * PI * t / LOOP_SECONDS) + 0.2 * sin(2 * PI * 3 * t / LOOP_SECONDS + 1)).toFloat()
            val coefficient = 0.01f + 0.06f * gust
            val white = random.nextFloat() * 2f - 1f
            lp1 += coefficient * (white - lp1)
            lp2 += coefficient * (lp1 - lp2)
            out[i] = lp2 * (0.4f + 0.6f * gust)
        }
    }

    private fun normalize(samples: FloatArray) {
        val rms = sqrt(samples.sumOf { (it * it).toDouble() } / samples.size).toFloat()
        if (rms == 0f) return
        val gain = TARGET_RMS / rms
        for (i in samples.indices) samples[i] = (samples[i] * gain).coerceIn(-1f, 1f)
    }
}
