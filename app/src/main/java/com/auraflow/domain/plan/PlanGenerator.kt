package com.auraflow.domain.plan

import com.auraflow.domain.model.DayPart
import com.auraflow.domain.model.Experience
import com.auraflow.domain.model.Goal
import com.auraflow.domain.model.OnboardingAnswers
import com.auraflow.domain.plan.PracticeStage.ANCHOR
import com.auraflow.domain.plan.PracticeStage.BODY_SCAN
import com.auraflow.domain.plan.PracticeStage.BREATH
import com.auraflow.domain.plan.PracticeStage.CLOSING
import com.auraflow.domain.plan.PracticeStage.SETTLE
import kotlin.math.min
import kotlin.math.roundToInt

object PlanGenerator {
    const val PLAN_DAYS = 7

    private val THEMES = listOf(
        "Знакомство с дыханием" to "Учимся следовать за ритмом песка: вдох рассыпает узор, выдох собирает.",
        "Заметить и вернуться" to "Главное упражнение медитации — замечать отвлечение и возвращать внимание.",
        "Звук как якорь" to "Внимание держится за затихающий звон чаши.",
        "Тело и опора" to "Сканирование тела: внимание медленно проходит от стоп до макушки.",
        "Длинный выдох" to "Выдох становится длиннее — телу легче замедлиться.",
        "Тишина между ударами" to "Больше времени наедине с узором и собственным дыханием.",
        "Своя практика" to "Все этапы вместе — в вашем темпе."
    )

    fun generate(answers: OnboardingAnswers): List<PlanDay> = (1..PLAN_DAYS).map { day(answers, it) }

    fun day(answers: OnboardingAnswers, day: Int): PlanDay {
        require(day in 1..PLAN_DAYS) { "День плана вне диапазона: $day" }
        val totalSeconds = minutesFor(answers, day) * 60
        val core = buildList {
            add(BREATH)
            if (day >= anchorFrom(answers)) add(ANCHOR)
            if (day >= scanFrom(answers)) add(BODY_SCAN)
        }
        val edge = if (totalSeconds <= 300) 45 else 60
        val stages = listOf(StageSpec(SETTLE, edge)) + split(totalSeconds - 2 * edge, core, answers.goal) +
            StageSpec(CLOSING, edge)
        val (title, description) = THEMES[day - 1]
        return PlanDay(day, title, description, totalSeconds / 60, stages, breathFor(answers.goal, day))
    }

    fun pause(): PlanDay = PlanDay(
        day = 0,
        title = "Пауза",
        description = "Две минуты наедине с пластиной.",
        minutes = 2,
        stages = listOf(StageSpec(ANCHOR, 120)),
        breath = BreathPattern(4, 6)
    )

    fun today(completed: Set<Int>, doneToday: Int?): Int = doneToday ?: nextDay(completed)

    fun nextDay(completed: Set<Int>): Int = (1..PLAN_DAYS).firstOrNull { it !in completed } ?: PLAN_DAYS

    fun isCompleted(completed: Set<Int>): Boolean = (1..PLAN_DAYS).all { it in completed }

    fun startMinutes(experience: Experience): Int = when (experience) {
        Experience.NEVER -> 5
        Experience.SOMETIMES -> 8
        Experience.REGULAR -> 10
    }

    fun minutesFor(answers: OnboardingAnswers, day: Int): Int {
        val start = min(startMinutes(answers.experience), answers.minutes)
        return start + (answers.minutes - start) * (day - 1) / (PLAN_DAYS - 1)
    }

    fun explain(answers: OnboardingAnswers): List<String> {
        val start = min(startMinutes(answers.experience), answers.minutes)
        val duration = if (start == answers.minutes) {
            "Каждый день по ${answers.minutes} мин — столько, сколько вы готовы уделять."
        } else {
            "Начинаем с $start мин, потому что ${experienceReason(answers.experience)}, и к 7-му дню доходим до выбранных ${answers.minutes} мин."
        }
        val goal = when (answers.goal) {
            Goal.STRESS -> "Больше времени на дыхание с удлинённым выдохом — он помогает снизить напряжение."
            Goal.SLEEP -> "Сканирование тела появится уже на 3-й день — оно помогает расслабиться перед сном."
            Goal.FOCUS -> "Больше времени на объект внимания — это прямая тренировка концентрации."
            Goal.EXPLORE -> "Этапы получают время поровну, чтобы вы попробовали каждый."
        }
        val time = "Напоминание можно включить в настройках — по умолчанию на ${dayPartTime(answers.dayPart)}."
        return listOf(duration, goal, time)
    }

    private fun experienceReason(experience: Experience) = when (experience) {
        Experience.NEVER -> "вы только начинаете"
        Experience.SOMETIMES -> "у вас уже есть опыт"
        Experience.REGULAR -> "вы практикуете регулярно"
    }

    private fun dayPartTime(dayPart: DayPart) = "%02d:00".format(dayPart.defaultHour)

    private fun anchorFrom(answers: OnboardingAnswers) = if (answers.experience == Experience.NEVER) 2 else 1

    private fun scanFrom(answers: OnboardingAnswers) = when {
        answers.experience == Experience.REGULAR -> 1
        answers.goal == Goal.SLEEP -> 3
        else -> 4
    }

    private fun split(seconds: Int, core: List<PracticeStage>, goal: Goal): List<StageSpec> {
        val weights = core.map { weight(goal, it) }
        val sum = weights.sum()
        val specs = core.dropLast(1).mapIndexed { i, stage -> StageSpec(stage, (seconds * weights[i] / sum).roundToInt()) }
        return specs + StageSpec(core.last(), seconds - specs.sumOf { it.seconds })
    }

    private fun weight(goal: Goal, stage: PracticeStage): Double = when (goal) {
        Goal.STRESS -> mapOf(BREATH to 0.45, ANCHOR to 0.35, BODY_SCAN to 0.20)
        Goal.SLEEP -> mapOf(BREATH to 0.35, ANCHOR to 0.20, BODY_SCAN to 0.45)
        Goal.FOCUS -> mapOf(BREATH to 0.25, ANCHOR to 0.50, BODY_SCAN to 0.25)
        Goal.EXPLORE -> mapOf(BREATH to 0.34, ANCHOR to 0.33, BODY_SCAN to 0.33)
    }.getValue(stage)

    private fun breathFor(goal: Goal, day: Int): BreathPattern {
        val longer = if (day >= 5) 1 else 0
        return if (goal == Goal.FOCUS) BreathPattern(4, 4 + longer) else BreathPattern(4, 6 + longer)
    }
}
