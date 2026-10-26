package com.auraflow.domain.plan

enum class PracticeStage(val title: String, val eyesClosed: Boolean = false) {
    SETTLE("Настрой"),
    BREATH("Дыхание"),
    ANCHOR("Объект внимания"),
    BODY_SCAN("Сканирование тела", eyesClosed = true),
    CLOSING("Завершение")
}

data class StageSpec(val stage: PracticeStage, val seconds: Int)

data class BreathPattern(val inhaleSec: Int, val exhaleSec: Int)

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
