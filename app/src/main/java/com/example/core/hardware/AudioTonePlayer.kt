package com.example.core.hardware

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class AudioTonePlayer {

    companion object {
        private const val TAG = "AudioTonePlayer"
        private const val SAMPLE_RATE = 44100
        private const val DEFAULT_FREQUENCY = 750.0 // Hz - Standard pleasant CW sidetone
    }

    private var audioTrack: AudioTrack? = null
    private var isPlayingContinuous = false
    private var continuousThread: Thread? = null

    var frequency: Double = DEFAULT_FREQUENCY
        set(value) {
            field = value.coerceIn(300.0, 1500.0)
        }

    var volume: Float = 0.9f
        set(value) {
            field = value.coerceIn(0.0f, 1.0f)
            audioTrack?.setVolume(field)
        }

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufferSize * 2).coerceAtLeast(SAMPLE_RATE / 4)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            audioTrack?.setVolume(volume)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack: ${e.message}", e)
        }
    }

    /**
     * Generates a smooth PCM buffer for the given duration with raised cosine ramp up & down.
     */
    private fun generateSineWaveWithEnvelope(durationMs: Long, freq: Double): ShortArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(50)
        val buffer = ShortArray(totalSamples)
        val rampSamples = ((SAMPLE_RATE * 0.005).toInt()).coerceAtMost(totalSamples / 4) // 5ms ramp

        val twoPiF = 2.0 * PI * freq / SAMPLE_RATE

        for (i in 0 until totalSamples) {
            var envelope = 1.0
            if (i < rampSamples) {
                // Fade in (Hann window)
                envelope = 0.5 * (1.0 - cos(PI * i / rampSamples))
            } else if (i >= totalSamples - rampSamples) {
                // Fade out (Hann window)
                val remaining = totalSamples - 1 - i
                envelope = 0.5 * (1.0 - cos(PI * remaining / rampSamples))
            }

            val rawSample = sin(twoPiF * i) * envelope
            val pcmSample = (rawSample * Short.MAX_VALUE * 0.95).toInt()
            buffer[i] = pcmSample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return buffer
    }

    /**
     * Synchronously/suspendingly plays a tone of specified duration with smooth envelope.
     */
    suspend fun playTone(durationMs: Long) = withContext(Dispatchers.IO) {
        if (durationMs <= 0) return@withContext
        try {
            ensureTrackPlaying()
            val track = audioTrack ?: return@withContext
            val pcm = generateSineWaveWithEnvelope(durationMs, frequency)
            track.write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING)
        } catch (e: Exception) {
            Log.w(TAG, "playTone error: ${e.message}")
        }
    }

    /**
     * Start continuous tone for manual keyer.
     */
    @Synchronized
    fun startContinuousTone() {
        if (isPlayingContinuous) return
        isPlayingContinuous = true
        ensureTrackPlaying()

        continuousThread = Thread {
            val chunkSize = 512
            val chunk = ShortArray(chunkSize)
            var phase = 0.0
            val phaseInc = 2.0 * PI * frequency / SAMPLE_RATE

            while (isPlayingContinuous) {
                for (i in 0 until chunkSize) {
                    val rawSample = sin(phase)
                    phase += phaseInc
                    if (phase >= 2.0 * PI) phase -= 2.0 * PI
                    chunk[i] = (rawSample * Short.MAX_VALUE * 0.9).toInt().toShort()
                }
                audioTrack?.write(chunk, 0, chunkSize, AudioTrack.WRITE_BLOCKING)
            }
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    /**
     * Stop continuous tone.
     */
    @Synchronized
    fun stopContinuousTone() {
        isPlayingContinuous = false
        continuousThread = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.w(TAG, "stopContinuousTone flush error: ${e.message}")
        }
    }

    private fun ensureTrackPlaying() {
        try {
            if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                initAudioTrack()
            }
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "ensureTrackPlaying error: ${e.message}")
        }
    }

    fun release() {
        isPlayingContinuous = false
        continuousThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.w(TAG, "release error: ${e.message}")
        }
    }
}
