package com.example.core.hardware

import android.content.Context
import com.example.core.morse.MorseEngine
import com.example.core.morse.MorseSignalToken
import com.example.core.morse.MorseTokenType
import com.example.core.morse.PlaybackProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MorseTransmitter(context: Context) {

    val audioTonePlayer = AudioTonePlayer()
    val torchController = TorchController(context)
    val hapticController = HapticController(context)

    // Configuration Settings
    var isSoundEnabled: Boolean = true
    var isTorchEnabled: Boolean = true
    var isScreenFlashEnabled: Boolean = true
    var isVibrationEnabled: Boolean = false
    var isLoopingEnabled: Boolean = false
    var wpm: Int = 15
        set(value) {
            field = value.coerceIn(5, 45)
        }

    private val _playbackState = MutableStateFlow(PlaybackProgress())
    val playbackState: StateFlow<PlaybackProgress> = _playbackState.asStateFlow()

    // Screen flash visual indicator
    private val _isScreenLit = MutableStateFlow(false)
    val isScreenLit: StateFlow<Boolean> = _isScreenLit.asStateFlow()

    private var transmitJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Transmits the given message text using the active outputs.
     */
    fun startTransmission(text: String) {
        stopTransmission()

        if (text.isBlank()) return

        transmitJob = scope.launch {
            try {
                do {
                    val tokens = MorseEngine.generateTransmissionTokens(text, wpm)
                    if (tokens.isEmpty()) break

                    val totalDuration = tokens.sumOf { it.durationMs }
                    var elapsedMs = 0L

                    _playbackState.update {
                        PlaybackProgress(
                            isPlaying = true,
                            isPaused = false,
                            currentCharIndex = 0,
                            totalDurationMs = totalDuration,
                            elapsedDurationMs = 0L
                        )
                    }

                    for (token in tokens) {
                        if (!isActive) break

                        val isSignal = token.isSignalOn

                        _playbackState.update {
                            it.copy(
                                isPlaying = true,
                                currentCharIndex = token.charIndex,
                                currentMorseToken = token,
                                isSignalActive = isSignal,
                                elapsedDurationMs = elapsedMs,
                                progressFraction = if (totalDuration > 0) (elapsedMs.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f
                            )
                        }

                        if (isSignal) {
                            if (isTorchEnabled) torchController.setTorch(true)
                            if (isScreenFlashEnabled) _isScreenLit.value = true
                            if (isVibrationEnabled) hapticController.vibratePulse(token.durationMs)

                            if (isSoundEnabled) {
                                // Sound plays in IO/Track thread
                                audioTonePlayer.playTone(token.durationMs)
                            } else {
                                delay(token.durationMs)
                            }
                        } else {
                            if (isTorchEnabled) torchController.setTorch(false)
                            if (isScreenFlashEnabled) _isScreenLit.value = false
                            delay(token.durationMs)
                        }

                        // Silence immediately after signal pulse
                        if (isTorchEnabled) torchController.setTorch(false)
                        if (isScreenFlashEnabled) _isScreenLit.value = false

                        elapsedMs += token.durationMs
                    }

                    // Reset signal at end of iteration
                    turnOffOutputs()
                    if (isLoopingEnabled && isActive) {
                        delay(MorseEngine.calculateDotDurationMs(wpm) * 7) // Standard word gap between loops
                    }
                } while (isLoopingEnabled && isActive)

            } catch (e: CancellationException) {
                // Normal cancellation
            } finally {
                turnOffOutputs()
                _playbackState.update {
                    PlaybackProgress(
                        isPlaying = false,
                        isPaused = false,
                        currentCharIndex = -1,
                        currentMorseToken = null,
                        isSignalActive = false
                    )
                }
            }
        }
    }

    /**
     * Stop and reset all active transmissions and hardware states.
     */
    fun stopTransmission() {
        transmitJob?.cancel()
        transmitJob = null
        turnOffOutputs()
        _playbackState.update {
            PlaybackProgress(
                isPlaying = false,
                isPaused = false,
                currentCharIndex = -1,
                currentMorseToken = null,
                isSignalActive = false
            )
        }
    }

    private fun turnOffOutputs() {
        torchController.turnOff()
        hapticController.cancel()
        _isScreenLit.value = false
    }

    /**
     * Direct manual keying (telegraph paddle): turn on tone/torch/screen.
     */
    fun setManualKey(isPressed: Boolean) {
        _isScreenLit.value = isPressed
        if (isPressed) {
            if (isSoundEnabled) audioTonePlayer.startContinuousTone()
            if (isTorchEnabled) torchController.setTorch(true)
            if (isVibrationEnabled) hapticController.vibratePulse(500)
        } else {
            if (isSoundEnabled) audioTonePlayer.stopContinuousTone()
            if (isTorchEnabled) torchController.setTorch(false)
            hapticController.cancel()
        }
    }

    fun release() {
        stopTransmission()
        audioTonePlayer.release()
    }
}
