package com.auraflow.domain.plan

enum class PracticeStage(val title: String, val eyesClosed: Boolean = false) {
    SETTLE("Настрой"),
    BREATH("Дыхание"),
    ANCHOR("Объект внимания"),
    SILENCE("Тишина"),
    BODY_SCAN("Сканирование тела", eyesClosed = true),
    GROUNDING("Заземление 5-4-3-2-1"),
    CLOSING("Завершение")
}

data class StageSpec(val stage: PracticeStage, val seconds: Int)

enum class BreathPhase(val title: String) {
    INHALE("Вдох…"),
    HOLD_IN("Задержка…"),
    EXHALE("Выдох…"),
    HOLD_OUT("Пауза…")
}

data class BreathPattern(
    val inhaleSec: Int,
    val exhaleSec: Int,
    val holdAfterInhaleSec: Int = 0,
    val holdAfterExhaleSec: Int = 0
) {
    val cycleSec: Int get() = inhaleSec + holdAfterInhaleSec + exhaleSec + holdAfterExhaleSec

    val label: String
        get() = listOf(inhaleSec, holdAfterInhaleSec, exhaleSec, holdAfterExhaleSec)
            .filterIndexed { i, s -> s > 0 || i % 2 == 0 }
            .joinToString("–")

    fun phaseAt(elapsedMs: Long): Pair<BreathPhase, Int> {
        var t = elapsedMs % (cycleSec * 1000L)
        for ((phase, seconds) in phases()) {
            if (t < seconds * 1000L) return phase to seconds
            t -= seconds * 1000L
        }
        return BreathPhase.INHALE to inhaleSec
    }

    private fun phases() = listOf(
        BreathPhase.INHALE to inhaleSec,
        BreathPhase.HOLD_IN to holdAfterInhaleSec,
        BreathPhase.EXHALE to exhaleSec,
        BreathPhase.HOLD_OUT to holdAfterExhaleSec
    ).filter { it.second > 0 }
}

enum class BreathTechnique(val title: String, val description: String, val pattern: BreathPattern) {
    CALM("Спокойное", "Удлинённый выдох замедляет ритм сердца", BreathPattern(4, 6)),
    BOX("Квадрат", "Вдох, задержка, выдох, задержка — по 4 секунды", BreathPattern(4, 4, holdAfterInhaleSec = 4, holdAfterExhaleSec = 4)),
    RELAX("4-7-8", "Глубокое расслабление перед сном", BreathPattern(4, 8, holdAfterInhaleSec = 7))
}

data class PlanDay(
    val day: Int,
    val title: String,
    val description: String,
    val minutes: Int,
    val stages: List<StageSpec>,
    val breath: BreathPattern
) {
    val totalSeconds: Int get() = stages.sumOf { it.seconds }
}
