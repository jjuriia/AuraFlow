package com.auraflow.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.auraflow.domain.model.AppSettings
import com.auraflow.domain.model.DayPart
import com.auraflow.domain.model.Experience
import com.auraflow.domain.model.Goal
import com.auraflow.domain.model.OnboardingAnswers
import com.auraflow.domain.model.StoredCredentials
import com.auraflow.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auraflow_prefs")

class UserPreferences(context: Context) {
    private val store = context.applicationContext.dataStore

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val profile: Flow<UserProfile?> = data.map { p ->
        val name = p[NAME] ?: return@map null
        val email = p[EMAIL] ?: return@map null
        val answers = readAnswers(p) ?: return@map null
        UserProfile(name, email, answers, p[PLAN_START_DAY] ?: return@map null)
    }

    val isLoggedIn: Flow<Boolean> = data.map { it[LOGGED_IN] ?: false }

    val settings: Flow<AppSettings> = data.map { p ->
        AppSettings(
            ttsEnabled = p[TTS_ENABLED] ?: false,
            remindersEnabled = p[REMINDERS_ENABLED] ?: false,
            reminderHour = p[REMINDER_HOUR] ?: readAnswers(p)?.dayPart?.defaultHour ?: DayPart.EVENING.defaultHour,
            reminderMinute = p[REMINDER_MINUTE] ?: 0
        )
    }

    suspend fun credentials(): StoredCredentials? {
        val p = data.first()
        return StoredCredentials(
            email = p[EMAIL] ?: return null,
            passwordHash = p[PASSWORD_HASH] ?: return null,
            salt = p[PASSWORD_SALT] ?: return null
        )
    }

    suspend fun quotesFromNetwork(): Boolean = data.first()[QUOTES_FROM_NETWORK] ?: false

    suspend fun saveRegistration(profile: UserProfile, credentials: StoredCredentials) {
        store.edit { p ->
            p[NAME] = profile.name
            p[EMAIL] = credentials.email
            p[PASSWORD_HASH] = credentials.passwordHash
            p[PASSWORD_SALT] = credentials.salt
            writeAnswers(p, profile.answers)
            p[PLAN_START_DAY] = profile.planStartEpochDay
            p[REMINDER_HOUR] = profile.answers.dayPart.defaultHour
            p[REMINDER_MINUTE] = 0
            p[LOGGED_IN] = true
        }
    }

    suspend fun saveAnswers(answers: OnboardingAnswers, planStartEpochDay: Long) {
        store.edit { p ->
            writeAnswers(p, answers)
            p[PLAN_START_DAY] = planStartEpochDay
        }
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        store.edit { it[LOGGED_IN] = loggedIn }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        store.edit { it[TTS_ENABLED] = enabled }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        require(hour in 0..23 && minute in 0..59) { "Некорректное время: $hour:$minute" }
        store.edit { p ->
            p[REMINDERS_ENABLED] = enabled
            p[REMINDER_HOUR] = hour
            p[REMINDER_MINUTE] = minute
        }
    }

    suspend fun setQuotesFromNetwork(value: Boolean) {
        store.edit { it[QUOTES_FROM_NETWORK] = value }
    }

    private fun readAnswers(p: Preferences): OnboardingAnswers? {
        val goal = Goal.entries.firstOrNull { it.name == p[GOAL] } ?: return null
        val experience = Experience.entries.firstOrNull { it.name == p[EXPERIENCE] } ?: return null
        val dayPart = DayPart.entries.firstOrNull { it.name == p[DAY_PART] } ?: return null
        val minutes = p[MINUTES] ?: return null
        return OnboardingAnswers(goal, experience, minutes, dayPart)
    }

    private fun writeAnswers(p: MutablePreferences, answers: OnboardingAnswers) {
        p[GOAL] = answers.goal.name
        p[EXPERIENCE] = answers.experience.name
        p[MINUTES] = answers.minutes
        p[DAY_PART] = answers.dayPart.name
    }

    private companion object {
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val PASSWORD_SALT = stringPreferencesKey("password_salt")
        val GOAL = stringPreferencesKey("goal")
        val EXPERIENCE = stringPreferencesKey("experience")
        val MINUTES = intPreferencesKey("minutes")
        val DAY_PART = stringPreferencesKey("day_part")
        val PLAN_START_DAY = longPreferencesKey("plan_start_day")
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val QUOTES_FROM_NETWORK = booleanPreferencesKey("quotes_from_network")
    }
}
