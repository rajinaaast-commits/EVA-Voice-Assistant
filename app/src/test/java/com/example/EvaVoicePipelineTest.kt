package com.example

import com.example.voice.SpeechCleaner
import com.example.voice.WakeWordDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaVoicePipelineTest {

    @Test
    fun testWakeWordDetectionAndStripping() {
        // Hey Eevva variation
        val result1 = WakeWordDetector.detectAndStrip("Hey Eevva, open YouTube.")
        assertTrue(result1.detected)
        assertEquals("open YouTube.", result1.cleanCommand)

        // Wake EVA variation
        val result2 = WakeWordDetector.detectAndStrip("Wake EVA, remind me at 8 PM.")
        assertTrue(result2.detected)
        assertEquals("remind me at 8 PM.", result2.cleanCommand)

        // Wake up EVA variation
        val result3 = WakeWordDetector.detectAndStrip("Wake up EVA, tell me a story.")
        assertTrue(result3.detected)
        assertEquals("tell me a story.", result3.cleanCommand)
    }

    @Test
    fun testSpeechDisfluencyCleaning() {
        // Disfluency removal
        val cleaned1 = SpeechCleaner.clean("umm... ah... please open YouTube.")
        assertEquals("Please open YouTube.", cleaned1)

        // Contextual "like" filler removal
        val cleaned2 = SpeechCleaner.clean("like, can you tell me today's weather?")
        assertEquals("Can you tell me today's weather?", cleaned2)

        // Contextual "like" preservation ("I like YouTube")
        val cleaned3 = SpeechCleaner.clean("I like YouTube.")
        assertEquals("I like YouTube.", cleaned3)
    }
}
