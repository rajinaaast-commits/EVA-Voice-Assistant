package com.example.service

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.voice.VoiceInteractionService

/**
 * Native Android VoiceInteractionService for EVA AI.
 * Enables Android OS to recognize EVA AI as the default digital assistant.
 */
class EVAVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        isServiceActive = true
    }

    override fun onShutdown() {
        super.onShutdown()
        isServiceActive = false
    }

    companion object {
        var isServiceActive: Boolean = false
            private set

        /**
         * Checks if EVA is selected as the default digital assistant in Android.
         */
        fun isDefaultAssistant(context: Context): Boolean {
            val myPackage = context.packageName
            val componentName = ComponentName(context, EVAVoiceInteractionService::class.java)

            // 1. Android standard VoiceInteractionService check
            try {
                if (VoiceInteractionService.isActiveService(context, componentName)) {
                    return true
                }
            } catch (_: Exception) {}

            // 2. Android 10+ RoleManager check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                        if (roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                            return true
                        }
                    }
                } catch (_: Exception) {}
            }

            // 3. Settings.Secure check for voice_interaction_service
            val voiceSetting = Settings.Secure.getString(
                context.contentResolver,
                "voice_interaction_service"
            )
            if (voiceSetting?.contains(myPackage) == true) {
                return true
            }

            // 4. Settings.Secure check for assistant
            val assistSetting = Settings.Secure.getString(
                context.contentResolver,
                "assistant"
            )
            if (assistSetting?.contains(myPackage) == true) {
                return true
            }

            return false
        }

        /**
         * Checks if EVA VoiceInteractionService is actively running in system
         */
        fun isActive(context: Context): Boolean {
            return isServiceActive || isDefaultAssistant(context)
        }
    }
}
