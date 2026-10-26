package com.example.core.decoder

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.core.morse.MorseEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class AudioDecoderState(
    val isListening: Boolean = false,
    val currentEnergy: Float = 0f,
    val squelchThreshold: Float = 0.30f,
    val isToneDetected: Boolean = false,
    val targetPitchHz: Int = 750,
    val detectedPitchHz: Int = 750,
    val currentLetterSymbols: String = "",
    val decodedText: String = "",
    val rawMorseStream: String = "",
    val detectedWpm: Int = 15,
    val waveformHistory: List<Float> = emptyList(),
    val audioLevelDb: Float = -60f,
    val isAutoTuneActive: Boolean = false
)

class AudioMorseDecoder {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var recordingJob: Job? = null

    private val _decoderState = MutableStateFlow(AudioDecoderState())
    val decoderState: StateFlow<AudioDecoderState> = _decoderState.asStateFlow()

    // Configuration
    var targetPitchHz: Int = 750
        set(value) {
            field = value.coerceIn(350, 1500)
            updateGoertzelCoeff()
            _decoderState.update { it.copy(targetPitchHz = field, detectedPitchHz = field) }
        }

    var squelchThreshold: Float = 0.30f
        set(value) {
            field = value.coerceIn(0.05f, 0.95f)
            _decoderState.update { it.copy(squelchThreshold = field) }
        }

    var targetWpm: Int = 15
        set(value) {
            field = value.coerceIn(5, 40)
            updateUnitTiming()
        }

    // Timing parameters
    private var unitTimeMs: Long = MorseEngine.calculateDotDurationMs(targetWpm)
    private var dotDashThresholdMs: Long = (unitTimeMs * 1.8).toLong()
    private var charSpaceThresholdMs: Long = (unitTimeMs * 2.0).toLong()
    private var wordSpaceThresholdMs: Long = (unitTimeMs * 5.0).toLong()

    // State machine
    private var isCurrentlyToneOn = false
    private var lastTransitionTime = SystemClock.elapsedRealtime()
    private val currentLetterMorse = StringBuilder()
    private val decodedMessage = StringBuilder()
    private val rawStream = StringBuilder()

    // Oscilloscope & Peak tracking
    private val historyBuffer = ArrayDeque<Float>(80)
    private var peakEnergySmoothed = 1000.0
    private var noiseFloorSmoothed = 100.0

    private val sampleRate = 44100
    private val bufferChunkSize = 512 // ~11.6ms window for low latency
    private var cachedGoertzelCoeff: Double = 0.0

    init {
        updateUnitTiming()
        updateGoertzelCoeff()
    }

    private fun updateGoertzelCoeff() {
        val k = (0.5 + (bufferChunkSize * targetPitchHz.toDouble() / sampleRate)).toInt()
        val omega = (2.0 * Math.PI * k) / bufferChunkSize
        cachedGoertzelCoeff = 2.0 * kotlin.math.cos(omega)
    }

    fun updateUnitTiming() {
        unitTimeMs = MorseEngine.calculateDotDurationMs(targetWpm)
        dotDashThresholdMs = (unitTimeMs * 1.8).toLong()
        charSpaceThresholdMs = (unitTimeMs * 2.0).toLong()
        wordSpaceThresholdMs = (unitTimeMs * 5.0).toLong()
    }

    @SuppressLint("MissingPermission")
    fun startListening(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("AudioMorseDecoder", "RECORD_AUDIO permission not granted.")
            return
        }

        if (recordingJob?.isActive == true) return

        recordingJob = scope.launch {
            val minBufSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufferChunkSize * 4)

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("AudioMorseDecoder", "AudioRecord initialization failed")
                    return@launch
                }

                audioRecord.startRecording()
                _decoderState.update { it.copy(isListening = true) }

                val audioBuffer = ShortArray(bufferChunkSize)
                var lastUiEmitTime = SystemClock.elapsedRealtime()

                while (isActive) {
                    val readSamples = audioRecord.read(audioBuffer, 0, bufferChunkSize)
                    if (readSamples <= 0) continue

                    val now = SystemClock.elapsedRealtime()

                    // Compute overall RMS for dB level
                    var sumSquares = 0.0
                    for (i in 0 until readSamples) {
                        val sample = audioBuffer[i].toDouble()
                        sumSquares += sample * sample
                    }
                    val rms = sqrt(sumSquares / readSamples)
                    val db = if (rms > 0) (20 * log10(rms / 32767.0)).toFloat().coerceIn(-80f, 0f) else -80f

                    // Compute tone energy via Goertzel algorithm at target frequency (using precomputed coeff)
                    val toneMagnitude = computeGoertzelFast(audioBuffer, readSamples, cachedGoertzelCoeff)

                    // Track noise floor vs peak energy with exponential moving average
                    if (toneMagnitude > peakEnergySmoothed) {
                        peakEnergySmoothed = peakEnergySmoothed * 0.85 + toneMagnitude * 0.15
                    } else {
                        peakEnergySmoothed = peakEnergySmoothed * 0.995 + toneMagnitude * 0.005
                    }

                    if (toneMagnitude < noiseFloorSmoothed) {
                        noiseFloorSmoothed = noiseFloorSmoothed * 0.85 + toneMagnitude * 0.15
                    } else {
                        noiseFloorSmoothed = noiseFloorSmoothed * 0.995 + toneMagnitude * 0.005
                    }

                    val dynamicRange = (peakEnergySmoothed - noiseFloorSmoothed).coerceAtLeast(200.0)
                    val normalizedEnergy = ((toneMagnitude - noiseFloorSmoothed) / dynamicRange).toFloat().coerceIn(0f, 1f)

                    // Waveform oscilloscope buffer
                    synchronized(historyBuffer) {
                        if (historyBuffer.size >= 80) {
                            historyBuffer.removeFirst()
                        }
                        historyBuffer.addLast(normalizedEnergy)
                    }

                    // Hysteresis tone detection
                    val threshold = squelchThreshold
                    val toneDetected = if (isCurrentlyToneOn) {
                        normalizedEnergy >= threshold * 0.70f
                    } else {
                        normalizedEnergy >= threshold * 1.05f
                    }

                    // Pulse Timing & State Machine
                    if (toneDetected != isCurrentlyToneOn) {
                        val duration = now - lastTransitionTime

                        if (isCurrentlyToneOn && !toneDetected) {
                            // Tone turned OFF -> Check pulse duration
                            if (duration >= 25) { // Glitch filter
                                val symbol = if (duration < dotDashThresholdMs) '.' else '-'
                                currentLetterMorse.append(symbol)
                                rawStream.append(symbol)
                            }
                        }

                        isCurrentlyToneOn = toneDetected
                        lastTransitionTime = now
                    } else {
                        // Check silence duration when tone is OFF
                        if (!isCurrentlyToneOn && currentLetterMorse.isNotEmpty()) {
                            val offDuration = now - lastTransitionTime
                            if (offDuration >= charSpaceThresholdMs) {
                                // Character boundary reached
                                val code = currentLetterMorse.toString()
                                val decodedChar = when (code) {
                                    "...---..." -> "SOS"
                                    ".-.-." -> "[AR]"
                                    "...-.-" -> "[SK]"
                                    else -> (MorseEngine.REVERSE_MAP[code] ?: '?').toString()
                                }
                                decodedMessage.append(decodedChar)
                                rawStream.append(" ")
                                currentLetterMorse.clear()
                            }
                        }

                        // Word space timeout
                        if (!isCurrentlyToneOn && decodedMessage.isNotEmpty() && !decodedMessage.endsWith(" ")) {
                            val offDuration = now - lastTransitionTime
                            if (offDuration >= wordSpaceThresholdMs) {
                                decodedMessage.append(" ")
                                rawStream.append(" / ")
                            }
                        }
                    }

                    // Throttle UI update emission to ~30Hz
                    if (now - lastUiEmitTime >= 33) {
                        lastUiEmitTime = now
                        val historyList = synchronized(historyBuffer) { historyBuffer.toList() }

                        _decoderState.update {
                            it.copy(
                                isListening = true,
                                currentEnergy = normalizedEnergy,
                                isToneDetected = isCurrentlyToneOn,
                                currentLetterSymbols = currentLetterMorse.toString(),
                                decodedText = decodedMessage.toString(),
                                rawMorseStream = rawStream.toString(),
                                detectedWpm = targetWpm,
                                waveformHistory = historyList,
                                audioLevelDb = db
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioMorseDecoder", "Error in audio decoding loop", e)
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (e: Exception) {
                    Log.w("AudioMorseDecoder", "Error releasing AudioRecord", e)
                }
                _decoderState.update { it.copy(isListening = false, isToneDetected = false) }
            }
        }
    }

    fun stopListening() {
        recordingJob?.cancel()
        recordingJob = null
        isCurrentlyToneOn = false
        _decoderState.update { it.copy(isListening = false, isToneDetected = false) }
    }

    /**
     * Scans surrounding frequencies (400 - 1200 Hz) to auto-tune to the loudest incoming tone.
     */
    fun autoTunePitch() {
        _decoderState.update { it.copy(isAutoTuneActive = true) }
        scope.launch {
            val minBufSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufferChunkSize * 4)

            var testRecord: AudioRecord? = null
            try {
                testRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize
                )
                if (testRecord.state == AudioRecord.STATE_INITIALIZED) {
                    testRecord.startRecording()
                    val buffer = ShortArray(bufferChunkSize * 2)
                    testRecord.read(buffer, 0, buffer.size)

                    var bestPitch = targetPitchHz
                    var maxMag = 0.0

                    // Scan from 450 to 1100 Hz in 25 Hz steps
                    for (f in 450..1100 step 25) {
                        val mag = computeGoertzel(buffer, buffer.size, f.toDouble(), sampleRate)
                        if (mag > maxMag) {
                            maxMag = mag
                            bestPitch = f
                        }
                    }

                    if (maxMag > 150.0) {
                        targetPitchHz = bestPitch
                    }
                }
            } catch (e: Exception) {
                Log.w("AudioMorseDecoder", "Auto-tune failed", e)
            } finally {
                try {
                    testRecord?.stop()
                    testRecord?.release()
                } catch (_: Exception) {}
                _decoderState.update { it.copy(isAutoTuneActive = false) }
            }
        }
    }

    fun clearDecodedText() {
        decodedMessage.clear()
        rawStream.clear()
        currentLetterMorse.clear()
        _decoderState.update {
            it.copy(
                decodedText = "",
                rawMorseStream = "",
                currentLetterSymbols = ""
            )
        }
    }

    /**
     * Efficient Goertzel single-frequency filter calculation
     */
    private fun computeGoertzel(
        samples: ShortArray,
        numSamples: Int,
        targetFreq: Double,
        sampleRate: Int
    ): Double {
        val k = (0.5 + (numSamples * targetFreq / sampleRate)).toInt()
        val omega = (2.0 * Math.PI * k) / numSamples
        val cosine = Math.cos(omega)
        val coeff = 2.0 * cosine

        var s0: Double
        var s1 = 0.0
        var s2 = 0.0

        for (i in 0 until numSamples) {
            val x = samples[i].toDouble()
            s0 = x + coeff * s1 - s2
            s2 = s1
            s1 = s0
        }

        val magSq = s1 * s1 + s2 * s2 - coeff * s1 * s2
        return sqrt(max(0.0, magSq)) / numSamples
    }

    /**
     * Fast Goertzel calculation with precomputed coefficient
     */
    private fun computeGoertzelFast(
        samples: ShortArray,
        numSamples: Int,
        coeff: Double
    ): Double {
        var s0: Double
        var s1 = 0.0
        var s2 = 0.0

        for (i in 0 until numSamples) {
            val x = samples[i].toDouble()
            s0 = x + coeff * s1 - s2
            s2 = s1
            s1 = s0
        }

        val magSq = s1 * s1 + s2 * s2 - coeff * s1 * s2
        return sqrt(max(0.0, magSq)) / numSamples
    }

    fun release() {
        stopListening()
        scope.coroutineContext.cancelChildren()
    }
}
