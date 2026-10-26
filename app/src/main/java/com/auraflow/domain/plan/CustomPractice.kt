package com.auraflow.domain.plan

import com.auraflow.domain.audio.NatureSound

data class PracticeBlock(val stage: PracticeStage, val minutes: Int)

data class CustomPractice(
    val id: Long = 0,
    val name: String,
    val blocks: List<PracticeBlock>,
    val technique: BreathTechnique = BreathTechnique.CALM,
    val sound: NatureSound? = null
) {
    val totalSeconds: Int get() = EDGE_SECONDS * 2 + blocks.sumOf { it.minutes * 60 }

    fun toPlanDay(): PlanDay = PlanDay(
        day = 0,
        title = name,
        description = blocks.joinToString(" · ") { "${it.stage.title} ${it.minutes} мин" },
        minutes = (totalSeconds + 30) / 60,
        stages = listOf(StageSpec(PracticeStage.SETTLE, EDGE_SECONDS)) +
            blocks.map { StageSpec(it.stage, it.minutes * 60) } +
            StageSpec(PracticeStage.CLOSING, EDGE_SECONDS),
        breath = technique.pattern
    )

    companion object {
        const val EDGE_SECONDS = 30
        const val MAX_NAME = 30
        const val MAX_BLOCKS = 6
        const val MAX_BLOCK_MINUTES = 20
        const val MAX_TOTAL_MINUTES = 60

        val BLOCK_STAGES = listOf(
            PracticeStage.BREATH,
            PracticeStage.SILENCE,
            PracticeStage.ANCHOR,
            PracticeStage.BODY_SCAN,
            PracticeStage.GROUNDING
        )

        fun validate(practice: CustomPractice): String? {
            val name = practice.name.trim()
            val minutes = practice.blocks.sumOf { it.minutes }
            return when {
                name.isEmpty() -> "Дайте практике название"
                name.length > MAX_NAME -> "Название — не длиннее $MAX_NAME символов"
                practice.blocks.isEmpty() -> "Добавьте хотя бы один блок"
                practice.blocks.size > MAX_BLOCKS -> "Не больше $MAX_BLOCKS блоков"
                practice.blocks.any { it.stage !in BLOCK_STAGES } -> "Недопустимый блок"
                practice.blocks.any { it.minutes !in 1..MAX_BLOCK_MINUTES } -> "Блок — от 1 до $MAX_BLOCK_MINUTES минут"
                minutes > MAX_TOTAL_MINUTES -> "Вся практика — не длиннее $MAX_TOTAL_MINUTES минут"
                else -> null
            }
        }
    }
}
