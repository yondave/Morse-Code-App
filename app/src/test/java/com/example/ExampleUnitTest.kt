package com.example

import com.example.core.morse.MorseEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testMorseTextToCode() {
        val sos = MorseEngine.textToMorse("SOS")
        assertEquals("... --- ...", sos)

        val hello = MorseEngine.textToMorse("HELLO")
        assertEquals(".... . .-.. .-.. ---", hello)
    }

    @Test
    fun testMorseCodeToText() {
        val decoded = MorseEngine.morseToText("... --- ...")
        assertEquals("SOS", decoded)
    }

    @Test
    fun testIsMorseCode() {
        assertTrue(MorseEngine.isMorseCode("... --- ..."))
        assertTrue(MorseEngine.isMorseCode(".- -... -.-."))
        assertFalse(MorseEngine.isMorseCode("HELLO WORLD"))
    }

    @Test
    fun testTimingCalculation() {
        // At 12 WPM, standard dot duration is 1200 / 12 = 100ms
        val dot12 = MorseEngine.calculateDotDurationMs(12)
        assertEquals(100L, dot12)

        val dash12 = MorseEngine.calculateDashDurationMs(12)
        assertEquals(300L, dash12)
    }
}
