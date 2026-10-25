package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.morse.MorseEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Morse Code", appName)
  }

  @Test
  fun `test Morse encoding and decoding`() {
    val text = "SOS"
    val morse = MorseEngine.textToMorse(text)
    assertEquals("... --- ...", morse)

    val decoded = MorseEngine.morseToText(morse)
    assertEquals("SOS", decoded)
  }

  @Test
  fun `test Morse timing calculation`() {
    val dot15Wpm = MorseEngine.calculateDotDurationMs(15)
    val dash15Wpm = MorseEngine.calculateDashDurationMs(15)
    assertEquals(80L, dot15Wpm)
    assertEquals(240L, dash15Wpm)
  }
}
