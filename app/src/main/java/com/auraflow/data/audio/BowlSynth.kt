package com.auraflow.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.auraflow.domain.audio.BowlTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

class BowlSynth {
    private val cache = ConcurrentHashMap<BowlTone, ShortArray>()
    private val voices = Semaphore(MAX_VOICES)

    suspend fun preload(tones: List<BowlTone>) {
        tones.forEach { pcmFor(it) }
    }

    suspend fun strike(tone: BowlTone, volume: Float = 1f) {
        if (!voices.tryAcquire()) return
        try {
            val pcm = pcmFor(tone)
            withContext(Dispatchers.IO) { play(pcm, volume.coerceIn(0f, 1f)) }
        } finally {
            voices.release()
        }
    }

    private suspend fun pcmFor(tone: BowlTone): ShortArray =
        cache[tone] ?: withContext(Dispatchers.Default) {
            val samples = tone.render(SAMPLE_RATE)
            ShortArray(samples.size) { i -> (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
        }.also { cache[tone] = it }

    private suspend fun play(pcm: ShortArray, volume: Float) {
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuffer * 2)
            .build()
        try {
            track.setVolume(volume)
            track.play()
            var written = 0
            while (written < pcm.size) {
                coroutineContext.ensureActive()
                val result = track.write(pcm, written, minOf(CHUNK, pcm.size - written))
                if (result <= 0) break
                written += result
            }

            val tailMs = written * 1000L / SAMPLE_RATE + 500
            withTimeoutOrNull(tailMs) {
                while (track.playbackHeadPosition < written) delay(POLL_MS)
            }
        } finally {
            track.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val CHUNK = 4_096
        const val POLL_MS = 50L
        const val MAX_VOICES = 6
    }
}
