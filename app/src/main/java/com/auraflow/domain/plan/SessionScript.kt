package com.auraflow.domain.plan

data class Cue(val atSecond: Int, val text: String)

object SessionScript {
    private val SETTLE = listOf(
        "Сядьте удобно. Спина прямая, но не напряжённая.",
        "Сделайте три глубоких вдоха и медленных выдоха.",
        "Позвольте телу стать тяжелее. Спешить некуда."
    )

    private val BREATH = listOf(
        "Следуйте за песком: на вдохе он рассыпается, на выдохе собирается в узор.",
        "Дышите в ритме подсказок, не форсируя вдох.",
        "Если ритм сбился — просто вернитесь к нему со следующим вдохом."
    )

    private val ANCHOR = listOf(
        "Смотрите на узор. Когда заметите, что мысли увели вас, коснитесь пластины.",
        "Отвлечься — нормально. Каждое возвращение и есть тренировка.",
        "Мягко возвращайтесь к узору столько раз, сколько нужно."
    )

    private val BODY_SCAN = listOf(
        "Можно закрыть глаза. Перенесите внимание на стопы.",
        "Голени и колени. Просто отмечайте ощущения, ничего не меняя.",
        "Бёдра и таз. Почувствуйте опору под собой.",
        "Живот: как он поднимается и опускается с дыханием.",
        "Грудь и спина.",
        "Плечи. Позвольте им опуститься.",
        "Руки — от плеч до кончиков пальцев.",
        "Шея, челюсть, лицо. Расслабьте лоб.",
        "Почувствуйте тело целиком."
    )

    private val CLOSING = listOf(
        "Откройте глаза. Прислушайтесь к затихающему звону.",
        "Отметьте, как вы себя чувствуете, — без оценки.",
        "Возьмите это спокойствие с собой."
    )

    fun cues(spec: StageSpec): List<Cue> {
        val phrases = when (spec.stage) {
            PracticeStage.SETTLE -> SETTLE
            PracticeStage.BREATH -> BREATH
            PracticeStage.ANCHOR -> ANCHOR
            PracticeStage.BODY_SCAN -> BODY_SCAN
            PracticeStage.CLOSING -> CLOSING
        }
        return phrases.mapIndexed { i, text -> Cue(spec.seconds * i / phrases.size, text) }
    }
}
