package com.auraflow.domain

import com.auraflow.domain.audio.NatureSound
import com.auraflow.domain.audio.NatureSoundSynth
import com.auraflow.domain.plan.BreathPattern
import com.auraflow.domain.plan.BreathPhase
import com.auraflow.domain.plan.BreathTechnique
import com.auraflow.domain.plan.CustomPractice
import com.auraflow.domain.plan.PlanGenerator
import com.auraflow.domain.plan.PracticeBlock
import com.auraflow.domain.plan.PracticeStage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class PracticeBuilderTest {

    private val practice = CustomPractice(
        name = "Вечер",
        blocks = listOf(PracticeBlock(PracticeStage.BREATH, 3), PracticeBlock(PracticeStage.SILENCE, 5)),
        technique = BreathTechnique.RELAX,
        sound = NatureSound.RAIN
    )

    @Test
    fun boxBreathingGoesThroughFourEqualPhases() {
        val box = BreathTechnique.BOX.pattern
        assertEquals(16, box.cycleSec)
        assertEquals(BreathPhase.INHALE, box.phaseAt(0).first)
        assertEquals(BreathPhase.HOLD_IN, box.phaseAt(4_000).first)
        assertEquals(BreathPhase.EXHALE, box.phaseAt(8_500).first)
        assertEquals(BreathPhase.HOLD_OUT, box.phaseAt(15_999).first)
        assertEquals(BreathPhase.INHALE, box.phaseAt(16_000).first)
        assertEquals("4–4–4–4", box.label)
    }

    @Test
    fun patternWithoutHoldsSkipsThem() {
        val calm = BreathPattern(4, 6)
        assertEquals(BreathPhase.EXHALE, calm.phaseAt(4_000).first)
        assertEquals(BreathPhase.INHALE to 4, calm.phaseAt(10_000))
        assertEquals("4–7–8", BreathTechnique.RELAX.pattern.label)
    }

    @Test
    fun customPracticeBecomesPlanWithEdges() {
        val plan = practice.toPlanDay()
        assertEquals(listOf(PracticeStage.SETTLE, PracticeStage.BREATH, PracticeStage.SILENCE, PracticeStage.CLOSING), plan.stages.map { it.stage })
        assertEquals(8 * 60 + 2 * CustomPractice.EDGE_SECONDS, plan.totalSeconds)
        assertEquals(practice.totalSeconds, plan.totalSeconds)
        assertEquals(BreathTechnique.RELAX.pattern, plan.breath)
    }

    @Test
    fun builderRejectsInvalidPractices() {
        assertNull(CustomPractice.validate(practice))
        assertNotNull(CustomPractice.validate(practice.copy(name = "  ")))
        assertNotNull(CustomPractice.validate(practice.copy(blocks = emptyList())))
        assertNotNull(CustomPractice.validate(practice.copy(blocks = listOf(PracticeBlock(PracticeStage.BREATH, 0)))))
        assertNotNull(CustomPractice.validate(practice.copy(blocks = List(4) { PracticeBlock(PracticeStage.SILENCE, 20) })))
        assertNotNull(CustomPractice.validate(practice.copy(blocks = listOf(PracticeBlock(PracticeStage.CLOSING, 5)))))
    }

    @Test
    fun sosLastsThreeMinutes() {
        val sos = PlanGenerator.sos()
        assertEquals(180, sos.totalSeconds)
        assertTrue(PracticeStage.GROUNDING in sos.stages.map { it.stage })
    }

    @Test
    fun natureSoundsAreDeterministicSeamlessLoops() {
        val rate = 8_000
        for (sound in NatureSound.entries) {
            val loop = NatureSoundSynth.render(sound, rate)
            assertEquals(NatureSoundSynth.LOOP_SECONDS * rate, loop.size)
            assertTrue(loop.all { it in -1f..1f })
            assertEquals(NatureSoundSynth.TARGET_RMS.toDouble(), rms(loop, 0, loop.size), 0.02)
            assertArrayEquals(loop, NatureSoundSynth.render(sound, rate), 0f)
            val maxStep = (1 until loop.size).maxOf { abs(loop[it] - loop[it - 1]) }
            assertTrue("$sound: щелчок на стыке петли", abs(loop[0] - loop.last()) <= maxStep)
        }
    }

    @Test
    fun seaSurgesWithWaves() {
        val rate = 8_000
        val sea = NatureSoundSynth.render(NatureSound.SEA, rate)
        val half = (NatureSoundSynth.WAVE_PERIOD_SECONDS / 2 * rate).toInt()
        val window = rate / 2
        val crest = rms(sea, half - window / 2, half + window / 2)
        val trough = rms(sea, 2 * half - window, 2 * half)
        assertTrue("гребень $crest, впадина $trough", crest > 2 * trough)
    }

    private fun rms(x: FloatArray, from: Int, to: Int): Double =
        sqrt((from until to).sumOf { (x[it] * x[it]).toDouble() } / (to - from))
}
