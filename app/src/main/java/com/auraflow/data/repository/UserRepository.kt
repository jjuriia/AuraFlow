package com.auraflow.data.repository

import com.auraflow.data.local.prefs.UserPreferences
import com.auraflow.domain.audio.NatureSound
import com.auraflow.domain.auth.PasswordHasher
import com.auraflow.domain.model.AppSettings
import com.auraflow.domain.model.OnboardingAnswers
import com.auraflow.domain.model.StoredCredentials
import com.auraflow.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

class UserRepository(private val prefs: UserPreferences) {
    val profile: Flow<UserProfile?> = prefs.profile
    val isLoggedIn: Flow<Boolean> = prefs.isLoggedIn
    val settings: Flow<AppSettings> = prefs.settings

    suspend fun register(name: String, email: String, password: String, answers: OnboardingAnswers, today: LocalDate) {
        val normalizedEmail = normalize(email)
        val salt = PasswordHasher.newSalt()

        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password, salt) }
        prefs.saveRegistration(
            UserProfile(name.trim(), normalizedEmail, answers, today.toEpochDay()),
            StoredCredentials(normalizedEmail, hash, salt)
        )
    }

    suspend fun login(email: String, password: String): Boolean {
        val credentials = prefs.credentials() ?: return false
        if (credentials.email != normalize(email)) return false
        val valid = withContext(Dispatchers.Default) {
            PasswordHasher.verify(password, credentials.salt, credentials.passwordHash)
        }
        if (valid) prefs.setLoggedIn(true)
        return valid
    }

    suspend fun hasProfile(): Boolean = prefs.credentials() != null

    suspend fun logout() = prefs.setLoggedIn(false)

    suspend fun rebuildPlan(answers: OnboardingAnswers, today: LocalDate) =
        prefs.saveAnswers(answers, today.toEpochDay())

    suspend fun setTtsEnabled(enabled: Boolean) = prefs.setTtsEnabled(enabled)

    val natureSound: Flow<NatureSound?> = prefs.natureSound

    suspend fun setNatureSound(sound: NatureSound?) = prefs.setNatureSound(sound)

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) = prefs.setReminder(enabled, hour, minute)

    private fun normalize(email: String) = email.trim().lowercase()
}
