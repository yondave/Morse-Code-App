package com.example.core.morse

import java.util.Locale

object MorseEngine {

    // ITU Standard Morse Code Map
    val MORSE_MAP: Map<Char, String> = mapOf(
        // Letters
        'A' to ".-",    'B' to "-...",  'C' to "-.-.",  'D' to "-..",
        'E' to ".",     'F' to "..-.",  'G' to "--.",   'H' to "....",
        'I' to "..",    'J' to ".---",  'K' to "-.-",   'L' to ".-..",
        'M' to "--",    'N' to "-.",    'O' to "---",   'P' to ".--.",
        'Q' to "--.-",  'R' to ".-.",   'S' to "...",   'T' to "-",
        'U' to "..-",   'V' to "...-",  'W' to ".--",   'X' to "-..-",
        'Y' to "-.--",  'Z' to "--..",

        // Numbers
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----.",

        // Punctuation & Symbols
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.",
        '!' to "-.-.--", '/' to "-..-.",  '(' to "-.--.",  ')' to "-.--.-",
        '&' to ".-...",  ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
        '+' to ".-.-.",  '-' to "-....-", '_' to "..--.-", '"' to ".-..-.",
        '$' to "...-..-", '@' to ".--.-."
    )

    // Reverse Map for Decoding
    val REVERSE_MAP: Map<String, Char> = MORSE_MAP.entries.associate { (k, v) -> v to k }

    // Dictionary with details for learning and reference
    val DICTIONARY: List<MorseDictionaryEntry> = buildList {
        // Letters A-Z
        val phonetics = mapOf(
            'A' to "Alfa", 'B' to "Bravo", 'C' to "Charlie", 'D' to "Delta",
            'E' to "Echo", 'F' to "Foxtrot", 'G' to "Golf", 'H' to "Hotel",
            'I' to "India", 'J' to "Juliett", 'K' to "Kilo", 'L' to "Lima",
            'M' to "Mike", 'N' to "November", 'O' to "Oscar", 'P' to "Papa",
            'Q' to "Quebec", 'R' to "Romeo", 'S' to "Sierra", 'T' to "Tango",
            'U' to "Uniform", 'V' to "Victor", 'W' to "Whiskey", 'X' to "X-ray",
            'Y' to "Yankee", 'Z' to "Zulu"
        )
        for (c in 'A'..'Z') {
            val code = MORSE_MAP[c] ?: ""
            add(MorseDictionaryEntry(
                character = c.toString(),
                code = code,
                category = MorseCategory.LETTERS,
                phonetic = phonetics[c] ?: ""
            ))
        }

        // Numbers 0-9
        for (c in '0'..'9') {
            val code = MORSE_MAP[c] ?: ""
            add(MorseDictionaryEntry(
                character = c.toString(),
                code = code,
                category = MorseCategory.NUMBERS,
                phonetic = "Number $c"
            ))
        }

        // Common Punctuation
        val puncts = listOf(
            Triple('.', ".-.-.-", "Period / Full Stop"),
            Triple(',', "--..--", "Comma"),
            Triple('?', "..--..", "Question Mark"),
            Triple('!', "-.-.--", "Exclamation Point"),
            Triple('/', "-..-.", "Slash / Fraction Bar"),
            Triple('=', "-...-", "Equals Sign"),
            Triple('+', ".-.-.", "Plus Sign"),
            Triple('-', "-....-", "Hyphen / Minus"),
            Triple('@', ".--.-.", "At Sign")
        )
        for ((char, code, desc) in puncts) {
            add(MorseDictionaryEntry(
                character = char.toString(),
                code = code,
                category = MorseCategory.PUNCTUATION,
                description = desc
            ))
        }

        // Special Emergency Prosigns
        add(MorseDictionaryEntry(
            character = "SOS",
            code = "...---...",
            category = MorseCategory.PROSIGNS,
            phonetic = "Distress Signal",
            description = "International Emergency Distress Call"
        ))
        add(MorseDictionaryEntry(
            character = "AR",
            code = ".-.-.",
            category = MorseCategory.PROSIGNS,
            phonetic = "End of Message",
            description = "Over / End of Transmission"
        ))
        add(MorseDictionaryEntry(
            character = "SK",
            code = "...-.-",
            category = MorseCategory.PROSIGNS,
            phonetic = "End of Contact",
            description = "Signing Off / Out"
        ))
        add(MorseDictionaryEntry(
            character = "BT",
            code = "-...-",
            category = MorseCategory.PROSIGNS,
            phonetic = "New Paragraph / Break",
            description = "Separation in Message"
        ))
    }

    /**
     * Calculates unit duration in milliseconds based on Words Per Minute (WPM).
     * PARIS standard: 1 word = 50 dot units.
     * dotDurationMs = 60,000 / (50 * WPM) = 1,200 / WPM.
     */
    fun calculateDotDurationMs(wpm: Int): Long {
        val safeWpm = wpm.coerceIn(5, 50)
        return (1200L / safeWpm).coerceAtLeast(20L)
    }

    /**
     * Standard Dah duration is exactly 3 dot units.
     */
    fun calculateDashDurationMs(wpm: Int): Long {
        return calculateDotDurationMs(wpm) * 3L
    }

    /**
     * Converts a raw text string into standard Morse representation with spaces between letters
     * and triple spaces (or slash) between words.
     */
    fun textToMorse(text: String, wordSeparator: String = "   "): String {
        val upper = text.uppercase(Locale.ROOT).trim()
        if (upper.isEmpty()) return ""

        val words = upper.split(Regex("\\s+"))
        return words.joinToString(wordSeparator) { word ->
            word.mapNotNull { c -> MORSE_MAP[c] }
                .joinToString(" ")
        }
    }

    /**
     * Normalizes a Morse string by replacing unicode dots, dashes, bullets, and apostrophes with standard '.' and '-'.
     */
    fun normalizeMorse(input: String): String {
        return input
            .replace('•', '.')
            .replace('·', '.')
            .replace('*', '.')
            .replace('\'', '.')
            .replace('`', '.')
            .replace('’', '.')
            .replace('‘', '.')
            .replace('—', '-')
            .replace('–', '-')
            .replace('_', '-')
            .replace('|', '/')
    }

    /**
     * Determines whether a given string is predominantly Morse code (contains dots, dashes, spaces, slashes, apostrophes).
     */
    fun isMorseCode(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        val normalized = normalizeMorse(trimmed)
        val morseChars = setOf('.', '-', '/', ' ')
        val hasMorseSymbols = normalized.any { it == '.' || it == '-' }
        val nonMorseCount = normalized.count { it !in morseChars }
        return hasMorseSymbols && nonMorseCount == 0
    }

    /**
     * Converts a Morse string (with dots and dashes) into plain text.
     * Standard input format: letters separated by space, words separated by 3 spaces or slash '/'.
     */
    fun morseToText(morse: String): String {
        val normalized = normalizeMorse(morse.trim())
        if (normalized.isEmpty()) return ""

        // Handle slash, pipe, or triple spaces as word breaks
        val words = if (normalized.contains("/")) {
            normalized.split("/")
        } else {
            normalized.split(Regex(" {2,}"))
        }

        return words.joinToString(" ") { wordMorse ->
            val letters = wordMorse.trim().split(Regex("\\s+"))
            letters.filter { it.isNotEmpty() }.map { letterCode ->
                when (letterCode) {
                    "...---..." -> "SOS"
                    ".-.-." -> "[AR]"
                    "...-.-" -> "[SK]"
                    "-...-" -> "[BT]"
                    else -> REVERSE_MAP[letterCode] ?: '?'
                }
            }.joinToString("")
        }.trim()
    }

    /**
     * Breaks down text into a sequence of atomic transmission tokens for sound, torch, and screen.
     */
    fun generateTransmissionTokens(text: String, wpm: Int): List<MorseSignalToken> {
        val dotMs = calculateDotDurationMs(wpm)
        val dashMs = dotMs * 3
        val intraSpaceMs = dotMs
        val charSpaceMs = dotMs * 3
        val wordSpaceMs = dotMs * 7

        val tokens = mutableListOf<MorseSignalToken>()
        val upper = text.uppercase(Locale.ROOT)

        for (i in upper.indices) {
            val char = upper[i]
            if (char.isWhitespace()) {
                tokens.add(
                    MorseSignalToken(
                        type = MorseTokenType.WORD_SPACE,
                        durationUnits = 7,
                        durationMs = wordSpaceMs,
                        isSignalOn = false,
                        charIndex = i,
                        sourceChar = ' ',
                        morseSymbol = " "
                    )
                )
                continue
            }

            val morse = MORSE_MAP[char]
            if (morse != null) {
                for (j in morse.indices) {
                    val symbol = morse[j]
                    val isDot = symbol == '.'
                    val type = if (isDot) MorseTokenType.DOT else MorseTokenType.DASH
                    val durationUnits = if (isDot) 1 else 3
                    val durationMs = if (isDot) dotMs else dashMs

                    // Signal ON
                    tokens.add(
                        MorseSignalToken(
                            type = type,
                            durationUnits = durationUnits,
                            durationMs = durationMs,
                            isSignalOn = true,
                            charIndex = i,
                            sourceChar = char,
                            morseSymbol = symbol.toString()
                        )
                    )

                    // Intra-character space (between dots/dashes of same letter)
                    if (j < morse.length - 1) {
                        tokens.add(
                            MorseSignalToken(
                                type = MorseTokenType.INTRA_SPACE,
                                durationUnits = 1,
                                durationMs = intraSpaceMs,
                                isSignalOn = false,
                                charIndex = i,
                                sourceChar = char,
                                morseSymbol = ""
                            )
                        )
                    }
                }

                // Space after character (if next char is not a whitespace and not end)
                if (i < upper.length - 1 && !upper[i + 1].isWhitespace()) {
                    tokens.add(
                        MorseSignalToken(
                            type = MorseTokenType.CHAR_SPACE,
                            durationUnits = 3,
                            durationMs = charSpaceMs,
                            isSignalOn = false,
                            charIndex = i,
                            sourceChar = char,
                            morseSymbol = ""
                        )
                    )
                }
            }
        }

        return tokens
    }
}
