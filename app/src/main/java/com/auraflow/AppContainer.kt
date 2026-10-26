package com.auraflow

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.auraflow.data.audio.BowlSynth
import com.auraflow.data.audio.Narrator
import com.auraflow.data.audio.NatureSoundPlayer
import com.auraflow.data.local.db.AuraDatabase
import com.auraflow.data.local.prefs.UserPreferences
import com.auraflow.data.reminder.ReminderScheduler
import com.auraflow.data.remote.QuotesApi
import com.auraflow.data.repository.CustomPracticeRepository
import com.auraflow.data.repository.QuoteRepository
import com.auraflow.data.repository.SessionRepository
import com.auraflow.data.repository.UserRepository

class AppContainer(context: Context) {
    private val database = AuraDatabase.create(context)
    private val preferences = UserPreferences(context)

    val userRepository = UserRepository(preferences)
    val sessionRepository = SessionRepository(database.sessionDao())
    val customPracticeRepository = CustomPracticeRepository(database.customPracticeDao())
    val quoteRepository = QuoteRepository(
        api = QuotesApi.create(),
        dao = database.quoteDao(),
        prefs = preferences,
        assets = context.assets,
        quotesUrl = BuildConfig.QUOTES_URL
    )
    val bowlSynth = BowlSynth()
    val natureSoundPlayer = NatureSoundPlayer()
    val narrator = Narrator(context)
    val reminderScheduler = ReminderScheduler(context)
}

fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AuraFlowApp).container
