package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.decoder.AudioDecoderState
import com.example.core.decoder.AudioMorseDecoder
import com.example.core.decoder.CameraMorseDecoder
import com.example.core.decoder.OpticalDecoderState
import com.example.core.hardware.MorseTransmitter
import com.example.core.morse.MorseDictionaryEntry
import com.example.core.morse.MorseEngine
import com.example.core.morse.PlaybackProgress
import com.example.data.AppDatabase
import com.example.data.MorseMessageEntity
import com.example.data.MorseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class ReceiverMode {
    OPTICAL,
    ACOUSTIC
}

data class ManualKeyerState(
    val isPressed: Boolean = false,
    val currentSymbols: String = "",
    val decodedText: String = "",
    val lastPressDurationMs: Long = 0L
)

class MorseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MorseRepository
    val transmitter: MorseTransmitter = MorseTransmitter(application)
    val opticalDecoder: CameraMorseDecoder = CameraMorseDecoder()
    val audioDecoder: AudioMorseDecoder = AudioMorseDecoder()

    // Receiver Mode (Optical Camera vs Acoustic Mic)
    private val _receiverMode = MutableStateFlow(ReceiverMode.OPTICAL)
    val receiverMode: StateFlow<ReceiverMode> = _receiverMode.asStateFlow()

    // Optical Zoom (0f to 1f)
    private val _opticalZoom = MutableStateFlow(0f)
    val opticalZoom: StateFlow<Float> = _opticalZoom.asStateFlow()

    // Audio Decoder State
    val audioState: StateFlow<AudioDecoderState> = audioDecoder.decoderState

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Input text in Transmitter tab
    private val _inputText = MutableStateFlow("SOS")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // Live Morse translation
    private val _translatedMorse = MutableStateFlow(MorseEngine.textToMorse("SOS"))
    val translatedMorse: StateFlow<String> = _translatedMorse.asStateFlow()

    // Transmitter Settings
    private val _wpm = MutableStateFlow(15)
    val wpm: StateFlow<Int> = _wpm.asStateFlow()

    private val _pitchHz = MutableStateFlow(750)
    val pitchHz: StateFlow<Int> = _pitchHz.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _torchEnabled = MutableStateFlow(true)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _screenFlashEnabled = MutableStateFlow(true)
    val screenFlashEnabled: StateFlow<Boolean> = _screenFlashEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(false)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _loopEnabled = MutableStateFlow(false)
    val loopEnabled: StateFlow<Boolean> = _loopEnabled.asStateFlow()

    // Transmitter playback state
    val playbackProgress: StateFlow<PlaybackProgress> = transmitter.playbackState
    val isScreenLit: StateFlow<Boolean> = transmitter.isScreenLit

    // Optical Decoder state
    val opticalState: StateFlow<OpticalDecoderState> = opticalDecoder.decoderState

    // Manual Keyer State
    private val _manualKeyerState = MutableStateFlow(ManualKeyerState())
    val manualKeyerState: StateFlow<ManualKeyerState> = _manualKeyerState.asStateFlow()
    private var manualKeyPressTime: Long = 0L
    private var manualKeyReleaseTime: Long = 0L

    // Presets
    val quickPresets = listOf(
        "SOS", "HELP", "MAYDAY", "OK", "CQ CQ CQ", "HELLO", "ALL CLEAR", "LOCATION HERE"
    )

    // Tutorial & Onboarding State
    private val sharedPrefs = application.getSharedPreferences("morse_app_prefs", Context.MODE_PRIVATE)
    private val _showTutorial = MutableStateFlow(!sharedPrefs.getBoolean("has_seen_tutorial", false))
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    // History from Room
    val messageHistory: StateFlow<List<MorseMessageEntity>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = MorseRepository(db.morseDao())
        messageHistory = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        initTts(application)
        syncTransmitterConfig()
    }

    private fun initTts(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsReady = true
            }
        }
    }

    private fun syncTransmitterConfig() {
        transmitter.wpm = _wpm.value
        transmitter.audioTonePlayer.frequency = _pitchHz.value.toDouble()
        transmitter.isSoundEnabled = _soundEnabled.value
        transmitter.isTorchEnabled = _torchEnabled.value
        transmitter.isScreenFlashEnabled = _screenFlashEnabled.value
        transmitter.isVibrationEnabled = _vibrationEnabled.value
        transmitter.isLoopingEnabled = _loopEnabled.value
    }

    fun onInputTextChanged(newText: String) {
        _inputText.value = newText
        _translatedMorse.value = MorseEngine.textToMorse(newText)
    }

    fun setWpm(newWpm: Int) {
        val clamped = newWpm.coerceIn(5, 40)
        _wpm.value = clamped
        transmitter.wpm = clamped
        opticalDecoder.targetWpm = clamped
    }

    fun setPitch(newPitch: Int) {
        val clamped = newPitch.coerceIn(400, 1200)
        _pitchHz.value = clamped
        transmitter.audioTonePlayer.frequency = clamped.toDouble()
    }

    fun toggleSound(enabled: Boolean) {
        _soundEnabled.value = enabled
        transmitter.isSoundEnabled = enabled
    }

    fun toggleTorch(enabled: Boolean) {
        _torchEnabled.value = enabled
        transmitter.isTorchEnabled = enabled
    }

    fun toggleScreenFlash(enabled: Boolean) {
        _screenFlashEnabled.value = enabled
        transmitter.isScreenFlashEnabled = enabled
    }

    fun toggleVibration(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        transmitter.isVibrationEnabled = enabled
    }

    fun toggleLoop(enabled: Boolean) {
        _loopEnabled.value = enabled
        transmitter.isLoopingEnabled = enabled
    }

    fun startTransmission() {
        val text = _inputText.value
        if (text.isNotBlank()) {
            syncTransmitterConfig()
            transmitter.startTransmission(text)

            // Save to history as sent message
            viewModelScope.launch {
                repository.saveMessage(
                    type = "SENT",
                    text = text,
                    morseCode = _translatedMorse.value,
                    wpm = _wpm.value
                )
            }
        }
    }

    fun stopTransmission() {
        transmitter.stopTransmission()
    }

    fun playSingleEntry(entry: MorseDictionaryEntry) {
        viewModelScope.launch {
            val prevSound = transmitter.isSoundEnabled
            val prevTorch = transmitter.isTorchEnabled
            transmitter.isSoundEnabled = true
            transmitter.isTorchEnabled = false
            transmitter.startTransmission(entry.character)
            // will finish automatically
        }
    }

    // Manual Telegraph Keyer
    fun onManualKeyDown() {
        manualKeyPressTime = SystemClock.elapsedRealtime()
        _manualKeyerState.update { it.copy(isPressed = true) }
        transmitter.setManualKey(true)
    }

    fun onManualKeyUp() {
        val now = SystemClock.elapsedRealtime()
        val duration = now - manualKeyPressTime
        manualKeyReleaseTime = now
        transmitter.setManualKey(false)

        val dotDuration = MorseEngine.calculateDotDurationMs(_wpm.value)
        val symbol = if (duration < dotDuration * 2.0) "." else "-"

        val updatedSymbols = _manualKeyerState.value.currentSymbols + symbol
        _manualKeyerState.update {
            it.copy(
                isPressed = false,
                currentSymbols = updatedSymbols,
                lastPressDurationMs = duration
            )
        }

        // Schedule letter resolution if no further press in 3 dot units
        viewModelScope.launch {
            kotlinx.coroutines.delay((dotDuration * 2.5).toLong())
            val currentRelease = manualKeyReleaseTime
            if (currentRelease == now && _manualKeyerState.value.currentSymbols.isNotEmpty()) {
                val code = _manualKeyerState.value.currentSymbols
                val decoded = (MorseEngine.REVERSE_MAP[code] ?: '?').toString()
                _manualKeyerState.update {
                    it.copy(
                        currentSymbols = "",
                        decodedText = it.decodedText + decoded
                    )
                }
            }
        }
    }

    fun clearManualKeyerText() {
        _manualKeyerState.update { ManualKeyerState() }
    }

    // Receiver Mode Controls
    fun setReceiverMode(mode: ReceiverMode) {
        _receiverMode.value = mode
        if (mode == ReceiverMode.ACOUSTIC) {
            audioDecoder.startListening(getApplication())
        } else {
            audioDecoder.stopListening()
        }
    }

    // Optical Decoder Actions
    fun setDecoderSensitivity(fraction: Float) {
        opticalDecoder.sensitivityFraction = fraction
    }

    fun toggleAutoThreshold(auto: Boolean) {
        opticalDecoder.isAutoThreshold = auto
    }

    fun setReceiverFixedWpmEnabled(enabled: Boolean) {
        opticalDecoder.isFixedWpm = enabled
    }

    fun setReceiverWpm(wpm: Int) {
        val clamped = wpm.coerceIn(4, 40)
        opticalDecoder.targetWpm = clamped
    }

    fun matchTransmitterWpm() {
        opticalDecoder.isFixedWpm = true
        opticalDecoder.targetWpm = _wpm.value
    }

    fun calibrateDecoder() {
        opticalDecoder.calibrateBaseline()
    }

    fun setOpticalRoi(normX: Float, normY: Float) {
        opticalDecoder.setRoiCenter(normX, normY)
    }

    fun resetOpticalRoi() {
        opticalDecoder.resetRoiCenter()
    }

    fun setOpticalZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0f, 1f)
        _opticalZoom.value = clamped
    }

    fun toggleOpticalExposureLock() {
        val current = opticalDecoder.decoderState.value.isExposureLocked
        opticalDecoder.isExposureLocked = !current
    }

    fun clearDecodedOpticalText() {
        opticalDecoder.clearDecodedText()
    }

    fun saveDecodedMessage() {
        val decoded = opticalDecoder.decoderState.value.decodedText
        if (decoded.isNotBlank()) {
            viewModelScope.launch {
                repository.saveMessage(
                    type = "OPTICAL_RX",
                    text = decoded,
                    morseCode = opticalDecoder.decoderState.value.rawMorseStream,
                    wpm = opticalDecoder.decoderState.value.detectedWpm
                )
                Toast.makeText(getApplication(), "Optical transmission saved to log!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Acoustic Audio Decoder Actions
    fun startAudioListening() {
        audioDecoder.startListening(getApplication())
    }

    fun stopAudioListening() {
        audioDecoder.stopListening()
    }

    fun setAudioPitch(hz: Int) {
        audioDecoder.targetPitchHz = hz
    }

    fun setAudioSquelch(threshold: Float) {
        audioDecoder.squelchThreshold = threshold
    }

    fun setAudioWpm(wpm: Int) {
        audioDecoder.targetWpm = wpm
    }

    fun autoTuneAudioPitch() {
        audioDecoder.autoTunePitch()
    }

    fun clearDecodedAudioText() {
        audioDecoder.clearDecodedText()
    }

    fun saveDecodedAudioMessage() {
        val decoded = audioDecoder.decoderState.value.decodedText
        if (decoded.isNotBlank()) {
            viewModelScope.launch {
                repository.saveMessage(
                    type = "ACOUSTIC_RX",
                    text = decoded,
                    morseCode = audioDecoder.decoderState.value.rawMorseStream,
                    wpm = audioDecoder.decoderState.value.detectedWpm
                )
                Toast.makeText(getApplication(), "Acoustic transmission saved to log!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyToClipboard(text: String, label: String = "Morse Code") {
        if (text.isBlank()) return
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(getApplication(), "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)
        val text = item?.text?.toString()?.trim() ?: ""

        if (text.isEmpty()) {
            Toast.makeText(getApplication(), "Clipboard is empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (MorseEngine.isMorseCode(text)) {
            val decoded = MorseEngine.morseToText(text)
            _inputText.value = decoded
            _translatedMorse.value = MorseEngine.textToMorse(decoded)
            Toast.makeText(getApplication(), "Pasted & decoded Morse: $decoded", Toast.LENGTH_SHORT).show()
        } else {
            _inputText.value = text
            _translatedMorse.value = MorseEngine.textToMorse(text)
            Toast.makeText(getApplication(), "Pasted text: $text", Toast.LENGTH_SHORT).show()
        }
    }

    fun pasteMorseCode(morseText: String) {
        val trimmed = morseText.trim()
        if (trimmed.isEmpty()) return
        val decoded = MorseEngine.morseToText(trimmed)
        _inputText.value = decoded
        _translatedMorse.value = MorseEngine.textToMorse(decoded)
        Toast.makeText(getApplication(), "Decoded: $decoded", Toast.LENGTH_SHORT).show()
    }

    fun getClipboardText(): String {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
    }

    fun speakText(text: String) {
        if (isTtsReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "morse_tts")
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun openTutorial() {
        _showTutorial.value = true
    }

    fun dismissTutorial(neverShowAgain: Boolean = true) {
        if (neverShowAgain) {
            sharedPrefs.edit().putBoolean("has_seen_tutorial", true).apply()
        }
        _showTutorial.value = false
    }

    fun playDitAudio() {
        viewModelScope.launch {
            val dotDuration = MorseEngine.calculateDotDurationMs(_wpm.value)
            transmitter.audioTonePlayer.playTone(dotDuration)
        }
    }

    fun playDahAudio() {
        viewModelScope.launch {
            val dashDuration = MorseEngine.calculateDashDurationMs(_wpm.value)
            transmitter.audioTonePlayer.playTone(dashDuration)
        }
    }

    override fun onCleared() {
        super.onCleared()
        transmitter.release()
        audioDecoder.release()
        tts?.stop()
        tts?.shutdown()
    }
}
