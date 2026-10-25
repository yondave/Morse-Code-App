package com.example.core.morse

/**
 * Tokens representing the atomic units in a Morse transmission stream.
 */
enum class MorseTokenType {
    DOT,
    DASH,
    INTRA_SPACE,   // Silence between dots/dashes within the same character (1 unit)
    CHAR_SPACE,    // Silence between characters (3 units)
    WORD_SPACE     // Silence between words (7 units)
}

data class MorseSignalToken(
    val type: MorseTokenType,
    val durationUnits: Int,
    val durationMs: Long,
    val isSignalOn: Boolean,
    val charIndex: Int,
    val sourceChar: Char,
    val morseSymbol: String
)

data class MorseDictionaryEntry(
    val character: String,
    val code: String,
    val category: MorseCategory,
    val phonetic: String = "",
    val description: String = ""
)

enum class MorseCategory {
    LETTERS,
    NUMBERS,
    PUNCTUATION,
    PROSIGNS
}

data class PlaybackProgress(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentCharIndex: Int = -1,
    val currentMorseToken: MorseSignalToken? = null,
    val isSignalActive: Boolean = false,
    val progressFraction: Float = 0f,
    val totalDurationMs: Long = 0L,
    val elapsedDurationMs: Long = 0L
)
