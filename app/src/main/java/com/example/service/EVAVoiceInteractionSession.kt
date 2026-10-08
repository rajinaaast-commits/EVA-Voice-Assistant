package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import com.example.MainActivity
import com.example.voice.EvaStateManager

/**
 * System assist session handler for EVA AI.
 * Invoked by Android when the user long-presses the home button,
 * activates the assistant gesture, or triggers system assist.
 */
class EVAVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        // Transition EVA state machine to WAKE
        EvaStateManager.setWake()

        // Launch MainActivity with assist trigger
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_ASSIST
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("trigger_assist", true)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}

        hide()
    }
}
