package com.auraflow.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class Narrator(context: Context) {
    enum class Status { OFF, INITIALIZING, READY, UNAVAILABLE }

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var users = 0

    private val _status = MutableStateFlow(Status.OFF)
    val status: StateFlow<Status> = _status.asStateFlow()

    fun open() {
        users++
        if (tts != null) return
        _status.value = Status.INITIALIZING
        tts = TextToSpeech(appContext) { code -> onInit(code) }
    }

    fun speak(text: String) {
        if (_status.value == Status.READY) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
    }

    fun close() {
        if (users == 0) return
        users--
        if (users > 0) return
        tts?.shutdown()
        tts = null
        _status.value = Status.OFF
    }

    private fun onInit(code: Int) {
        val engine = tts ?: return
        if (code != TextToSpeech.SUCCESS) {
            _status.value = Status.UNAVAILABLE
            return
        }
        val result = engine.setLanguage(RUSSIAN)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _status.value = Status.UNAVAILABLE
            return
        }

        engine.setSpeechRate(0.85f)
        engine.setPitch(0.95f)
        _status.value = Status.READY
    }

    private companion object {
        val RUSSIAN: Locale = Locale.forLanguageTag("ru-RU")
        const val UTTERANCE_ID = "auraflow_cue"
    }
}
