package com.example

import android.app.Application
import com.example.ai.AIProviderManager
import com.example.data.local.EvaDatabase
import com.example.data.preferences.EvaPreferences
import com.example.data.preferences.SecureKeyStorage
import com.example.voice.AudioProcessor
import com.example.voice.EvaTtsHelper
import com.example.voice.SpeechRecognitionHelper
import com.example.voice.VoiceProfileManager

class EvaApplication : Application() {
    lateinit var database: EvaDatabase
        private set
    lateinit var secureStorage: SecureKeyStorage
        private set
    lateinit var preferences: EvaPreferences
        private set
    lateinit var aiProviderManager: AIProviderManager
        private set
    lateinit var ttsHelper: EvaTtsHelper
        private set
    lateinit var speechHelper: SpeechRecognitionHelper
        private set
    lateinit var audioProcessor: AudioProcessor
        private set
    lateinit var voiceProfileManager: VoiceProfileManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = EvaDatabase.getDatabase(this)
        secureStorage = SecureKeyStorage(this)
        preferences = EvaPreferences(this)
        aiProviderManager = AIProviderManager(this, secureStorage, preferences, database)
        ttsHelper = EvaTtsHelper(this, preferences)
        speechHelper = SpeechRecognitionHelper(this)
        audioProcessor = AudioProcessor()
        voiceProfileManager = VoiceProfileManager(database.voiceProfileDao())
    }

    companion object {
        lateinit var instance: EvaApplication
            private set
    }
}
