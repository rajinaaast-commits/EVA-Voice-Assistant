package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.EvaPreferences

/**
 * Restores EVA background voice service on device reboot if configured by the user.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = EvaPreferences(context)
            if (prefs.isOnboardingCompleted() && (prefs.isWakeWordEnabled() || prefs.isEdgeGlowEnabled())) {
                try {
                    EvaVoiceService.startService(context)
                } catch (_: Exception) {}
            }
        }
    }
}
