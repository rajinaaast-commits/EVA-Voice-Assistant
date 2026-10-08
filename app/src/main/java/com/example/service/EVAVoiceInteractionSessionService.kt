package com.example.service

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * Android VoiceInteractionSessionService providing sessions for system assist triggers.
 */
class EVAVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return EVAVoiceInteractionSession(this)
    }
}
