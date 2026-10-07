package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AIProviderMode {
    AUTO,
    GEMINI,
    OMNIROUTE
}

enum class VoiceEngineMode {
    SWIFT,
    SOUL,
    AGENTIC
}

enum class SubscriptionPlan {
    FREE,
    PREMIUM,
    PRO,
    MAX
}

class EvaPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("eva_app_prefs", Context.MODE_PRIVATE)

    private val _onboardingCompleted = MutableStateFlow(isOnboardingCompleted())
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _providerMode = MutableStateFlow(getProviderMode())
    val providerMode: StateFlow<AIProviderMode> = _providerMode.asStateFlow()

    private val _subscriptionPlan = MutableStateFlow(getSubscriptionPlan())
    val subscriptionPlan: StateFlow<SubscriptionPlan> = _subscriptionPlan.asStateFlow()

    fun isOnboardingCompleted(): Boolean =
        prefs.getBoolean("onboarding_completed", false)

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
        _onboardingCompleted.value = completed
    }

    fun getProviderMode(): AIProviderMode {
        val modeStr = prefs.getString("ai_provider_mode", AIProviderMode.AUTO.name) ?: AIProviderMode.AUTO.name
        return try {
            AIProviderMode.valueOf(modeStr)
        } catch (_: Exception) {
            AIProviderMode.AUTO
        }
    }

    fun setProviderMode(mode: AIProviderMode) {
        prefs.edit().putString("ai_provider_mode", mode.name).apply()
        _providerMode.value = mode
    }

    fun getGeminiModel(): String =
        prefs.getString("gemini_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"

    fun setGeminiModel(model: String) {
        prefs.edit().putString("gemini_model", model).apply()
    }

    fun getOmniRouteModel(): String =
        prefs.getString("omniroute_model", "auto") ?: "auto"

    fun setOmniRouteModel(model: String) {
        prefs.edit().putString("omniroute_model", model).apply()
    }

    fun getOmniRouteBaseUrl(): String =
        prefs.getString("omniroute_base_url", "http://localhost:20128/v1") ?: "http://localhost:20128/v1"

    fun setOmniRouteBaseUrl(url: String) {
        prefs.edit().putString("omniroute_base_url", url).apply()
    }

    fun getAssistantName(): String =
        prefs.getString("assistant_name", "EVA") ?: "EVA"

    fun setAssistantName(name: String) {
        prefs.edit().putString("assistant_name", name).apply()
    }

    fun getPersona(): String =
        prefs.getString("persona", "EVA") ?: "EVA"

    fun setPersona(persona: String) {
        prefs.edit().putString("persona", persona).apply()
    }

    fun getCustomPersonaPrompt(): String =
        prefs.getString("custom_persona_prompt", "") ?: ""

    fun setCustomPersonaPrompt(prompt: String) {
        prefs.edit().putString("custom_persona_prompt", prompt).apply()
    }

    fun isMemoryEnabled(): Boolean =
        prefs.getBoolean("memory_enabled", true)

    fun setMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("memory_enabled", enabled).apply()
    }

    fun isIncognito(): Boolean =
        prefs.getBoolean("incognito_mode", false)

    fun setIncognito(incognito: Boolean) {
        prefs.edit().putBoolean("incognito_mode", incognito).apply()
    }

    fun isAutonomousMemory(): Boolean =
        prefs.getBoolean("auto_memory", true)

    fun setAutonomousMemory(auto: Boolean) {
        prefs.edit().putBoolean("auto_memory", auto).apply()
    }

    fun getSelectedVoice(): String =
        prefs.getString("selected_voice", "Breezy — Aoede") ?: "Breezy — Aoede"

    fun setSelectedVoice(voice: String) {
        prefs.edit().putString("selected_voice", voice).apply()
    }

    fun getVoiceSpeed(): Float =
        prefs.getFloat("voice_speed", 1.0f)

    fun setVoiceSpeed(speed: Float) {
        prefs.edit().putFloat("voice_speed", speed).apply()
    }

    fun getVoicePitch(): Float =
        prefs.getFloat("voice_pitch", 1.0f)

    fun setVoicePitch(pitch: Float) {
        prefs.edit().putFloat("voice_pitch", pitch).apply()
    }

    fun isAutoSpeak(): Boolean =
        prefs.getBoolean("auto_speak", true)

    fun setAutoSpeak(auto: Boolean) {
        prefs.edit().putBoolean("auto_speak", auto).apply()
    }

    fun getVoiceEngineMode(): VoiceEngineMode {
        val modeStr = prefs.getString("voice_engine_mode", VoiceEngineMode.SWIFT.name) ?: VoiceEngineMode.SWIFT.name
        return try {
            VoiceEngineMode.valueOf(modeStr)
        } catch (_: Exception) {
            VoiceEngineMode.SWIFT
        }
    }

    fun setVoiceEngineMode(mode: VoiceEngineMode) {
        prefs.edit().putString("voice_engine_mode", mode.name).apply()
    }

    fun getLanguage(): String =
        prefs.getString("language", "Hinglish") ?: "Hinglish"

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
    }

    fun isWakeWordEnabled(): Boolean =
        prefs.getBoolean("wake_word_enabled", true)

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("wake_word_enabled", enabled).apply()
    }

    fun isProactiveEvaEnabled(): Boolean =
        prefs.getBoolean("proactive_eva_enabled", true)

    fun setProactiveEvaEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("proactive_eva_enabled", enabled).apply()
    }

    fun getProactiveIntervalMinutes(): Int =
        prefs.getInt("proactive_interval_min", 30)

    fun setProactiveIntervalMinutes(minutes: Int) {
        prefs.edit().putInt("proactive_interval_min", minutes).apply()
    }

    fun getSecretMode(): String =
        prefs.getString("secret_mode", "Friendly") ?: "Friendly"

    fun setSecretMode(mode: String) {
        prefs.edit().putString("secret_mode", mode).apply()
    }

    fun isIntenseModeConfirmed(): Boolean =
        prefs.getBoolean("intense_mode_confirmed", false)

    fun setIntenseModeConfirmed(confirmed: Boolean) {
        prefs.edit().putBoolean("intense_mode_confirmed", confirmed).apply()
    }

    fun getSubscriptionPlan(): SubscriptionPlan {
        val planStr = prefs.getString("subscription_plan", SubscriptionPlan.FREE.name) ?: SubscriptionPlan.FREE.name
        return try {
            SubscriptionPlan.valueOf(planStr)
        } catch (_: Exception) {
            SubscriptionPlan.FREE
        }
    }

    fun setSubscriptionPlan(plan: SubscriptionPlan) {
        prefs.edit().putString("subscription_plan", plan.name).apply()
        _subscriptionPlan.value = plan
    }

    fun getLicenseKey(): String =
        prefs.getString("license_key", "") ?: ""

    fun setLicenseKey(key: String) {
        prefs.edit().putString("license_key", key).apply()
    }

    fun getTextScale(): String =
        prefs.getString("text_scale", "Medium") ?: "Medium"

    fun setTextScale(scale: String) {
        prefs.edit().putString("text_scale", scale).apply()
    }

    fun isHighContrast(): Boolean =
        prefs.getBoolean("high_contrast", false)

    fun setHighContrast(highContrast: Boolean) {
        prefs.edit().putBoolean("high_contrast", highContrast).apply()
    }
}
