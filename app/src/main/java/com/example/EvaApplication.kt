package com.example

import android.app.Application
import com.example.ai.AIProviderManager
import com.example.connectors.GmailSmtpManager
import com.example.connectors.TelegramBotManager
import com.example.data.local.EvaDatabase
import com.example.data.preferences.EvaPreferences
import com.example.data.preferences.SecureKeyStorage
import com.example.device.DeviceControlManager
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
    lateinit var deviceControlManager: DeviceControlManager
        private set
    lateinit var telegramBotManager: TelegramBotManager
        private set
    lateinit var gmailSmtpManager: GmailSmtpManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = EvaDatabase.getDatabase(this)
        secureStorage = SecureKeyStorage(this)
        preferences = EvaPreferences(this)
        deviceControlManager = DeviceControlManager(this)
        aiProviderManager = AIProviderManager(this, secureStorage, preferences, database, deviceControlManager)
        ttsHelper = EvaTtsHelper(this, preferences)
        speechHelper = SpeechRecognitionHelper(this)
        audioProcessor = AudioProcessor()
        voiceProfileManager = VoiceProfileManager(database.voiceProfileDao())
        telegramBotManager = TelegramBotManager(this, secureStorage, aiProviderManager, ttsHelper)
        gmailSmtpManager = GmailSmtpManager(this, secureStorage)
    }

    companion object {
        lateinit var instance: EvaApplication
            private set
    }
}
