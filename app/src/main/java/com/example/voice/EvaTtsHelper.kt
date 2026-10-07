package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.preferences.EvaPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class EvaTtsHelper(
    private val context: Context,
    private val preferences: EvaPreferences
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyVoiceSettings()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun applyVoiceSettings() {
        if (!isInitialized) return
        val speed = preferences.getVoiceSpeed()
        val pitch = preferences.getVoicePitch()
        val voiceName = preferences.getSelectedVoice()

        tts?.setSpeechRate(speed)

        // Custom pitch variations based on selected voice persona
        val adjustedPitch = when {
            voiceName.contains("Aoede") -> pitch * 1.15f
            voiceName.contains("Kore") -> pitch * 0.9f
            voiceName.contains("Leda") -> pitch * 1.25f
            voiceName.contains("Zephyr") -> pitch * 1.05f
            voiceName.contains("Laomedeia") -> pitch * 1.2f
            voiceName.contains("Despina") -> pitch * 0.95f
            voiceName.contains("Mature") -> pitch * 0.85f
            else -> pitch
        }
        tts?.setPitch(adjustedPitch)

        // Apply locale based on language settings
        val lang = preferences.getLanguage()
        val locale = when (lang) {
            "Bangla", "Banglish" -> Locale("bn", "BD")
            "Hindi", "Hinglish" -> Locale("hi", "IN")
            else -> Locale.US
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.US
        }
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (!preferences.isAutoSpeak()) return
        if (!isInitialized) return

        applyVoiceSettings()
        _isSpeaking.value = true
        val utteranceId = "eva_utterance_${System.currentTimeMillis()}"

        // Filter out markdown symbols and tool tags before speaking aloud
        val cleanText = text
            .replace(Regex("""\[TOOL_CALL:.*?\]"""), "")
            .replace(Regex("""[`*#_>~]"""), "")
            .trim()

        if (cleanText.isEmpty()) {
            _isSpeaking.value = false
            return
        }

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        val AVAILABLE_VOICES = listOf(
            "Breezy — Aoede",
            "Firm — Kore",
            "Youthful — Leda",
            "Bright — Zephyr",
            "Upbeat — Laomedeia",
            "Smooth — Despina",
            "Clear — Erinome",
            "Easy-going",
            "Mature",
            "Forward"
        )
    }
}
