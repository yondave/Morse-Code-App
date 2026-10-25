package com.example.core.decoder

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.core.morse.MorseEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.ByteBuffer

data class OpticalDecoderState(
    val currentLuminance: Float = 0f,
    val baselineLuminance: Float = 0.2f,
    val peakLuminance: Float = 0.8f,
    val thresholdLuminance: Float = 0.45f,
    val isLightDetected: Boolean = false,
    val currentLetterSymbols: String = "",
    val decodedText: String = "",
    val rawMorseStream: String = "",
    val detectedWpm: Int = 12,
    val isFixedWpm: Boolean = false,
    val fixedWpm: Int = 12,
    val signalStrengthPercent: Int = 0,
    val isAutoThreshold: Boolean = true,
    val manualThresholdFraction: Float = 0.5f,
    val luminanceHistory: List<Float> = emptyList(),
    val roiX: Float = 0.5f,
    val roiY: Float = 0.5f,
    val isExposureLocked: Boolean = false
)

class CameraMorseDecoder : ImageAnalysis.Analyzer {

    private val _decoderState = MutableStateFlow(OpticalDecoderState())
    val decoderState: StateFlow<OpticalDecoderState> = _decoderState.asStateFlow()

    // Configuration
    var sensitivityFraction: Float = 0.45f // 0.1 (very sensitive) to 0.9 (needs bright light)
    var isAutoThreshold: Boolean = true
    var isFixedWpm: Boolean = false
        set(value) {
            field = value
            updateUnitTiming()
        }
    var targetWpm: Int = 12
        set(value) {
            field = value.coerceIn(4, 40)
            updateUnitTiming()
        }

    // Dynamic Tap-to-Aim ROI & Exposure
    var roiCenterNormalizedX: Float = 0.5f
    var roiCenterNormalizedY: Float = 0.5f
    var isExposureLocked: Boolean = false
        set(value) {
            field = value
            _decoderState.update { it.copy(isExposureLocked = value) }
        }

    // Timing in milliseconds
    private var unitTimeMs: Long = MorseEngine.calculateDotDurationMs(targetWpm) // dot length
    private var dotDashThresholdMs: Long = (unitTimeMs * 1.8).toLong()
    private var charSpaceThresholdMs: Long = (unitTimeMs * 2.0).toLong()
    private var wordSpaceThresholdMs: Long = (unitTimeMs * 5.0).toLong()

    // State Tracking
    private var isCurrentlyOn: Boolean = false
    private var lastTransitionTime: Long = SystemClock.elapsedRealtime()
    private var currentLetterMorse: StringBuilder = StringBuilder()
    private var decodedMessage: StringBuilder = StringBuilder()
    private var rawStream: StringBuilder = StringBuilder()

    // Adaptive Luminance Baseline Tracking
    private var minLuminanceSmoothed: Float = 0.1f
    private var maxLuminanceSmoothed: Float = 0.8f
    private val historyBuffer = ArrayDeque<Float>(80)

    private var lastPeriodicCheckTime = SystemClock.elapsedRealtime()

    init {
        updateUnitTiming()
    }

    fun updateUnitTiming() {
        unitTimeMs = MorseEngine.calculateDotDurationMs(targetWpm)
        dotDashThresholdMs = (unitTimeMs * 1.8).toLong()
        charSpaceThresholdMs = (unitTimeMs * 2.0).toLong()
        wordSpaceThresholdMs = (unitTimeMs * 5.0).toLong()
    }

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        val luminance = extractRoiLuminance(image)
        image.close()

        // Update Luminance History for visual oscilloscope
        synchronized(historyBuffer) {
            if (historyBuffer.size >= 80) {
                historyBuffer.removeFirst()
            }
            historyBuffer.addLast(luminance)
        }

        // Adaptive min/max baseline tracking with exponential moving averages
        if (luminance < minLuminanceSmoothed) {
            minLuminanceSmoothed = minLuminanceSmoothed * 0.9f + luminance * 0.1f
        } else {
            minLuminanceSmoothed = minLuminanceSmoothed * 0.995f + luminance * 0.005f
        }

        if (luminance > maxLuminanceSmoothed) {
            maxLuminanceSmoothed = maxLuminanceSmoothed * 0.85f + luminance * 0.15f
        } else {
            maxLuminanceSmoothed = maxLuminanceSmoothed * 0.99f + luminance * 0.01f
        }

        // Ensure reasonable minimum contrast gap
        val lumRange = (maxLuminanceSmoothed - minLuminanceSmoothed).coerceAtLeast(0.12f)
        val threshold = if (isAutoThreshold) {
            minLuminanceSmoothed + lumRange * sensitivityFraction
        } else {
            sensitivityFraction
        }

        val lightDetected = luminance >= threshold
        val signalStrength = (((luminance - minLuminanceSmoothed) / lumRange) * 100f).toInt().coerceIn(0, 100)

        // State Machine Pulse Detection
        if (lightDetected != isCurrentlyOn) {
            val duration = now - lastTransitionTime

            if (isCurrentlyOn && !lightDetected) {
                // Signal ended (Light ON -> OFF)
                // Filter out tiny optical glitches/flicker (< 25ms)
                if (duration >= 25) {
                    val symbol = if (duration < dotDashThresholdMs) '.' else '-'
                    currentLetterMorse.append(symbol)
                    rawStream.append(symbol)

                    // Adaptive timing adjustment: only if Auto mode is active
                    if (!isFixedWpm && symbol == '.') {
                        val estimatedUnit = duration.coerceIn(30L, 300L)
                        unitTimeMs = (unitTimeMs * 0.7 + estimatedUnit * 0.3).toLong()
                        dotDashThresholdMs = (unitTimeMs * 1.8).toLong()
                        charSpaceThresholdMs = (unitTimeMs * 2.0).toLong()
                        wordSpaceThresholdMs = (unitTimeMs * 5.0).toLong()
                    }
                }
            }

            isCurrentlyOn = lightDetected
            lastTransitionTime = now
        } else {
            // Check for silence timeouts while OFF
            if (!isCurrentlyOn && currentLetterMorse.isNotEmpty()) {
                val offDuration = now - lastTransitionTime
                if (offDuration >= charSpaceThresholdMs) {
                    // Character boundary reached!
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

            // Word Space timeout
            if (!isCurrentlyOn && decodedMessage.isNotEmpty() && !decodedMessage.endsWith(" ")) {
                val offDuration = now - lastTransitionTime
                if (offDuration >= wordSpaceThresholdMs) {
                    decodedMessage.append(" ")
                    rawStream.append(" / ")
                }
            }
        }

        // Throttle UI update emission to ~30Hz
        if (now - lastPeriodicCheckTime >= 30) {
            lastPeriodicCheckTime = now
            val historyList = synchronized(historyBuffer) { historyBuffer.toList() }

            val estimatedWpm = if (isFixedWpm) targetWpm else (1200L / unitTimeMs.coerceAtLeast(30L)).toInt().coerceIn(5, 40)

            _decoderState.update {
                it.copy(
                    currentLuminance = luminance,
                    baselineLuminance = minLuminanceSmoothed,
                    peakLuminance = maxLuminanceSmoothed,
                    thresholdLuminance = threshold,
                    isLightDetected = lightDetected,
                    currentLetterSymbols = currentLetterMorse.toString(),
                    decodedText = decodedMessage.toString(),
                    rawMorseStream = rawStream.toString(),
                    detectedWpm = estimatedWpm,
                    isFixedWpm = isFixedWpm,
                    fixedWpm = targetWpm,
                    signalStrengthPercent = signalStrength,
                    isAutoThreshold = isAutoThreshold,
                    manualThresholdFraction = sensitivityFraction,
                    luminanceHistory = historyList,
                    roiX = roiCenterNormalizedX,
                    roiY = roiCenterNormalizedY,
                    isExposureLocked = isExposureLocked
                )
            }
        }
    }

    /**
     * Extracts normalized average/peak luminance (0.0 to 1.0) from the dynamic Region of Interest (ROI).
     */
    private fun extractRoiLuminance(image: ImageProxy): Float {
        val plane = image.planes.firstOrNull() ?: return 0f
        val buffer: ByteBuffer = plane.buffer
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        // Dynamic ~22% box ROI positioned at (roiCenterNormalizedX, roiCenterNormalizedY)
        val roiWidth = (width * 0.22).toInt().coerceAtLeast(12)
        val roiHeight = (height * 0.22).toInt().coerceAtLeast(12)
        val startX = ((width * roiCenterNormalizedX) - (roiWidth / 2f)).toInt().coerceIn(0, width - roiWidth)
        val startY = ((height * roiCenterNormalizedY) - (roiHeight / 2f)).toInt().coerceIn(0, height - roiHeight)

        var sum = 0L
        var count = 0
        var maxPixel = 0

        // Subsample for fast real-time performance
        val step = 2
        val rowBuffer = ByteArray(roiWidth * pixelStride)

        for (y in startY until (startY + roiHeight) step step) {
            val rowStart = y * rowStride + startX * pixelStride
            buffer.position(rowStart)
            val bytesToRead = (roiWidth * pixelStride).coerceAtMost(buffer.remaining())
            if (bytesToRead <= 0) continue
            buffer.get(rowBuffer, 0, bytesToRead)

            for (x in 0 until (bytesToRead / pixelStride) step step) {
                val value = rowBuffer[x * pixelStride].toInt() and 0xFF
                sum += value
                count++
                if (value > maxPixel) {
                    maxPixel = value
                }
            }
        }

        if (count == 0) return 0f
        val avg = sum.toFloat() / count
        // Weight both the average and peak in ROI for bright pinpoint LED flashes
        val weighted = (avg * 0.6f + maxPixel * 0.4f) / 255.0f
        return weighted.coerceIn(0.0f, 1.0f)
    }

    fun setRoiCenter(x: Float, y: Float) {
        roiCenterNormalizedX = x.coerceIn(0.12f, 0.88f)
        roiCenterNormalizedY = y.coerceIn(0.12f, 0.88f)
        _decoderState.update { it.copy(roiX = roiCenterNormalizedX, roiY = roiCenterNormalizedY) }
    }

    fun resetRoiCenter() {
        roiCenterNormalizedX = 0.5f
        roiCenterNormalizedY = 0.5f
        _decoderState.update { it.copy(roiX = 0.5f, roiY = 0.5f) }
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

    fun calibrateBaseline() {
        minLuminanceSmoothed = _decoderState.value.currentLuminance * 0.8f
        maxLuminanceSmoothed = (_decoderState.value.currentLuminance * 1.5f).coerceAtMost(1.0f)
    }
}
