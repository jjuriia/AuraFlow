package com.auraflow.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.auraflow.domain.audio.NatureSound
import com.auraflow.domain.audio.NatureSoundSynth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class NatureSoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = ConcurrentHashMap<NatureSound, ShortArray>()
    private var job: Job? = null

    fun play(sound: NatureSound) {
        val previous = job
        job = scope.launch {
            previous?.cancelAndJoin()
            val pcm = cache.getOrPut(sound) {
                val samples = NatureSoundSynth.render(sound, SAMPLE_RATE)
                ShortArray(samples.size) { i -> (samples[i] * Short.MAX_VALUE).toInt().toShort() }
            }
            val track = createTrack(pcm)
            try {
                track.play()
                fade(track, 0f, VOLUME)
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    fade(track, VOLUME, 0f)
                    track.stop()
                    track.release()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun createTrack(pcm: ShortArray): AudioTrack {
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
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        track.setLoopPoints(0, pcm.size, -1)
        track.setVolume(0f)
        return track
    }

    private suspend fun fade(track: AudioTrack, from: Float, to: Float) {
        for (step in 1..FADE_STEPS) {
            track.setVolume(from + (to - from) * step / FADE_STEPS)
            delay(FADE_MS / FADE_STEPS)
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val VOLUME = 0.45f
        const val FADE_MS = 1_500L
        const val FADE_STEPS = 30
    }
}
