package com.auraflow.ui.screens.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.auraflow.appContainer
import com.auraflow.data.audio.BowlSynth
import com.auraflow.data.audio.Narrator
import com.auraflow.data.audio.NatureSoundPlayer
import com.auraflow.data.repository.CustomPracticeRepository
import com.auraflow.data.repository.SessionRepository
import com.auraflow.data.repository.UserRepository
import com.auraflow.domain.audio.BowlTone
import com.auraflow.domain.audio.NatureSound
import com.auraflow.domain.chladni.ChladniField
import com.auraflow.domain.chladni.PlateEngine
import com.auraflow.domain.model.ChladniPattern
import com.auraflow.domain.model.Session
import com.auraflow.domain.model.isCompleted
import com.auraflow.domain.plan.BreathPattern
import com.auraflow.domain.plan.BreathPhase
import com.auraflow.domain.plan.BreathTechnique
import com.auraflow.domain.plan.PlanDay
import com.auraflow.domain.plan.PlanGenerator
import com.auraflow.domain.plan.PracticeStage
import com.auraflow.domain.plan.SessionScript
import com.auraflow.domain.plan.StageSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class PracticeKind { PLAN, PAUSE, SOS, CUSTOM }

enum class PracticePhase { MOOD_BEFORE, PRACTICE, MOOD_AFTER, SUMMARY }

data class BreathUi(val phase: BreathPhase, val phaseSeconds: Int, val startedAtNanos: Long)

data class PracticeUiState(
    val kind: PracticeKind = PracticeKind.PLAN,
    val plan: PlanDay? = null,
    val header: String = "",
    val phase: PracticePhase = PracticePhase.MOOD_BEFORE,
    val stageIndex: Int = 0,
    val stageElapsedSec: Int = 0,
    val cue: String = "",
    val breath: BreathUi? = null,
    val technique: BreathTechnique? = null,
    val sound: NatureSound? = null,
    val soundOn: Boolean = true,
    val moodBefore: Int = 3,
    val moodAfter: Int = 3,
    val attentionReturns: Int = 0,
    val bowlStrikes: Int = 0,
    val durationSec: Int = 0,
    val completed: Boolean = false,
    val finalPattern: ChladniPattern? = null
) {
    val stage: StageSpec? get() = plan?.stages?.getOrNull(stageIndex)
    val hasBreath: Boolean get() = plan?.stages?.any { it.stage == PracticeStage.BREATH } == true
    val canChooseTechnique: Boolean get() = hasBreath && kind != PracticeKind.SOS
    val breathPattern: BreathPattern? get() = technique?.pattern ?: plan?.breath
}

class PracticeViewModel(
    savedStateHandle: SavedStateHandle,
    private val users: UserRepository,
    private val sessions: SessionRepository,
    private val customPractices: CustomPracticeRepository,
    private val synth: BowlSynth,
    private val narrator: Narrator,
    private val nature: NatureSoundPlayer
) : ViewModel() {
    private val kind = PracticeKind.entries.firstOrNull { it.name == savedStateHandle.get<String>("kind") } ?: PracticeKind.PAUSE
    private val id: Long = savedStateHandle.get<Long>("id") ?: 0L
    private val noteIndex =
        if (kind == PracticeKind.PLAN && id > 0) ((id - 1) % ChladniField.MODES.size).toInt() else Random.nextInt(ChladniField.MODES.size)
    private val tone = BowlTone(BowlTone.NOTES_HZ[noteIndex])

    val engine = PlateEngine(PARTICLES, System.nanoTime()).apply {
        val (n, m) = ChladniField.MODES[noteIndex]
        setMode(n, m)
    }

    private val _state = MutableStateFlow(PracticeUiState(kind = kind))
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    private var sessionTitle = ""
    private var narration = false
    private var startedAtMillis = 0L
    private var startedAtNanos = 0L
    private var timer: Job? = null

    init {
        viewModelScope.launch {
            val profile = users.profile.first()
            val custom = if (kind == PracticeKind.CUSTOM) customPractices.get(id) else null
            val plan = when {
                kind == PracticeKind.PLAN && profile != null && id in 1..PlanGenerator.PLAN_DAYS -> PlanGenerator.day(profile.answers, id.toInt())
                kind == PracticeKind.SOS -> PlanGenerator.sos()
                custom != null -> custom.toPlanDay()
                else -> PlanGenerator.pause()
            }
            sessionTitle = when {
                kind == PracticeKind.PLAN && plan.day > 0 -> "День ${plan.day} · ${plan.title}"
                else -> plan.title
            }
            val header = if (plan.day > 0) "День ${plan.day} · ${plan.title} · ${plan.minutes} мин" else "${plan.title} · ${plan.minutes} мин"
            val sound = if (custom != null) custom.sound else users.natureSound.first()
            _state.update { it.copy(plan = plan, header = header, technique = custom?.technique, sound = sound) }
            if (users.settings.first().ttsEnabled) {
                narrator.open()
                narration = true
            }
        }
        viewModelScope.launch { synth.preload(listOf(tone)) }
    }

    fun setMoodBefore(value: Int) = _state.update { it.copy(moodBefore = value) }

    fun setMoodAfter(value: Int) = _state.update { it.copy(moodAfter = value) }

    fun selectTechnique(technique: BreathTechnique?) = _state.update { it.copy(technique = technique) }

    fun selectSound(sound: NatureSound?) {
        _state.update { it.copy(sound = sound) }
        if (kind != PracticeKind.CUSTOM) viewModelScope.launch { users.setNatureSound(sound) }
    }

    fun toggleSound() {
        val s = _state.value
        val sound = s.sound ?: return
        if (s.soundOn) nature.stop() else nature.play(sound)
        _state.update { it.copy(soundOn = !it.soundOn) }
    }

    fun start() {
        val current = _state.value
        val plan = current.plan ?: return
        if (current.phase != PracticePhase.MOOD_BEFORE) return
        startedAtMillis = System.currentTimeMillis()
        startedAtNanos = System.nanoTime()
        current.sound?.let { nature.play(it) }
        _state.update { it.copy(phase = PracticePhase.PRACTICE, soundOn = true) }
        timer = viewModelScope.launch {
            runStages(plan, current.breathPattern ?: plan.breath)
            finishPractice()
        }
    }

    fun onPlateTouch(x: Float, y: Float) {
        engine.touch(x, y)
        val s = _state.value
        if (s.phase == PracticePhase.PRACTICE && s.stage?.stage == PracticeStage.ANCHOR) {
            _state.update { it.copy(attentionReturns = it.attentionReturns + 1) }
            ring(volume = 0.7f, energy = 0.9f)
        }
    }

    fun onBowlTap() {
        engine.strike(1f, tone.decaySeconds.toFloat())
        viewModelScope.launch { synth.strike(tone, 1f) }
    }

    fun finishEarly() {
        timer?.cancel()
        finishPractice()
    }

    fun complete() {
        val s = _state.value
        if (s.phase != PracticePhase.MOOD_AFTER) return
        val pattern = ChladniField.patternFor(s.durationSec, s.attentionReturns, s.moodAfter - s.moodBefore, startedAtMillis)
        engine.setMode(pattern.n, pattern.m)
        engine.sand.scatter()
        ring(volume = 1f, energy = 1f, decaySeconds = SUMMARY_DECAY)
        _state.update { it.copy(phase = PracticePhase.SUMMARY, finalPattern = pattern) }
        viewModelScope.launch {
            sessions.save(
                Session(
                    startedAt = startedAtMillis,
                    durationSec = s.durationSec,
                    planDay = id.toInt().takeIf { kind == PracticeKind.PLAN && it > 0 },
                    attentionReturns = s.attentionReturns,
                    moodBefore = s.moodBefore,
                    moodAfter = s.moodAfter,
                    pattern = pattern,
                    completed = s.completed,
                    title = sessionTitle
                )
            )
        }
    }

    private suspend fun runStages(plan: PlanDay, breath: BreathPattern) = coroutineScope {
        for ((index, spec) in plan.stages.withIndex()) {
            _state.update { it.copy(stageIndex = index, stageElapsedSec = 0, breath = null) }
            ring(volume = 0.6f, energy = 0.8f)
            if (spec.stage == PracticeStage.CLOSING) {
                launch {
                    repeat(2) {
                        delay(CLOSING_CHIME_GAP_MS)
                        ring(volume = 0.45f, energy = 0.6f)
                    }
                }
            }
            val cues = SessionScript.cues(spec)
            var nextCue = 0
            var breathPhase: BreathPhase? = null
            val stageStart = System.nanoTime()
            while (true) {
                val elapsedMs = (System.nanoTime() - stageStart) / 1_000_000
                if (elapsedMs >= spec.seconds * 1000L) break
                val elapsedSec = (elapsedMs / 1000).toInt()
                while (nextCue < cues.size && elapsedSec >= cues[nextCue].atSecond) {
                    showCue(cues[nextCue].text)
                    if (spec.stage in CHIME_ON_CUE && nextCue > 0) ring(volume = 0.3f, energy = 0.4f)
                    nextCue++
                }
                if (spec.stage == PracticeStage.BREATH) breathPhase = updateBreath(breath, elapsedMs, breathPhase)
                _state.update { it.copy(stageElapsedSec = elapsedSec) }
                delay(TICK_MS)
            }
        }
    }

    private fun updateBreath(pattern: BreathPattern, elapsedMs: Long, previous: BreathPhase?): BreathPhase {
        val (phase, seconds) = pattern.phaseAt(elapsedMs)
        if (phase != previous) {
            when (phase) {
                BreathPhase.INHALE -> engine.inhale()
                BreathPhase.EXHALE -> ring(volume = 0.3f, energy = 0.55f)
                BreathPhase.HOLD_IN, BreathPhase.HOLD_OUT -> Unit
            }
            _state.update { it.copy(breath = BreathUi(phase, seconds, System.nanoTime())) }
        }
        return phase
    }

    private fun finishPractice() {
        if (_state.value.phase != PracticePhase.PRACTICE) return
        narrator.stop()
        nature.stop()
        val duration = ((System.nanoTime() - startedAtNanos) / 1_000_000_000L).toInt()
        val completed = isCompleted(duration, _state.value.plan?.totalSeconds ?: 0)
        _state.update { it.copy(phase = PracticePhase.MOOD_AFTER, durationSec = duration, completed = completed, breath = null, cue = "") }
    }

    private fun showCue(text: String) {
        _state.update { it.copy(cue = text) }
        if (narration) narrator.speak(text)
    }

    private fun ring(volume: Float, energy: Float, decaySeconds: Double = tone.decaySeconds) {
        engine.strike(energy, decaySeconds.toFloat())
        _state.update { it.copy(bowlStrikes = it.bowlStrikes + 1) }
        viewModelScope.launch { synth.strike(tone, volume) }
    }

    override fun onCleared() {
        nature.stop()
        if (narration) narrator.close()
    }

    companion object {
        private const val PARTICLES = 1_400
        private const val TICK_MS = 100L
        private const val CLOSING_CHIME_GAP_MS = 4_000L
        private const val SUMMARY_DECAY = 6.0
        private val CHIME_ON_CUE = setOf(PracticeStage.BODY_SCAN, PracticeStage.GROUNDING)

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                PracticeViewModel(
                    createSavedStateHandle(),
                    c.userRepository,
                    c.sessionRepository,
                    c.customPracticeRepository,
                    c.bowlSynth,
                    c.narrator,
                    c.natureSoundPlayer
                )
            }
        }
    }
}
