package com.auraflow.domain

import com.auraflow.domain.audio.BowlTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

class BowlToneTest {
    @Test
    fun envelopeFallsToOneOverEAfterTau() {
        assertEquals(1.0, BowlTone.envelope(0.0, tau = 2.0), 1e-12)
        assertEquals(1.0 / E, BowlTone.envelope(2.0, tau = 2.0), 1e-12)
    }

    @Test
    fun ringTimeMatchesMinus60Decibels() {
        val tone = BowlTone(fundamentalHz = 220.0, decaySeconds = 3.5)
        assertEquals(3.5 * ln(1000.0), tone.ringSeconds(), 1e-9)
        assertEquals(0.001, tone.energyAt(tone.ringSeconds()), 1e-9)
    }

    @Test
    fun renderedSignalNeverClips() {
        val samples = BowlTone(fundamentalHz = 220.0, decaySeconds = 1.0).render(sampleRate = 8_000)
        val peak = samples.maxOf { abs(it) }
        assertTrue("клиппинг: пик $peak", peak <= 1f)
    }

    @Test
    fun renderedSignalDecays() {
        val sampleRate = 8_000
        val samples = BowlTone(fundamentalHz = 220.0, decaySeconds = 1.0).render(sampleRate)
        val window = sampleRate / 4
        val head = rms(samples, 0, window)
        val tail = rms(samples, samples.size - window, samples.size)
        assertTrue("звук не затухает: начало $head, конец $tail", head > tail * 100)
    }

    private fun rms(s: FloatArray, from: Int, to: Int): Double {
        var sum = 0.0
        for (i in from until to) sum += s[i].toDouble() * s[i]
        return sqrt(sum / (to - from))
    }
}
