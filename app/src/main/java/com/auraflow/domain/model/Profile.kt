package com.auraflow.domain.model

enum class Goal { STRESS, SLEEP, FOCUS, EXPLORE }

enum class Experience { NEVER, SOMETIMES, REGULAR }

enum class DayPart(val defaultHour: Int) {
    MORNING(8),
    AFTERNOON(13),
    EVENING(21)
}

data class OnboardingAnswers(
    val goal: Goal,
    val experience: Experience,
    val minutes: Int,
    val dayPart: DayPart
)

data class UserProfile(
    val name: String,
    val email: String,
    val answers: OnboardingAnswers,

    val planStartEpochDay: Long
)

data class StoredCredentials(
    val email: String,
    val passwordHash: String,
    val salt: String
)

data class AppSettings(
    val ttsEnabled: Boolean,
    val remindersEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int
)
