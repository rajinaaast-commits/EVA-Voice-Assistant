package com.example.ai

import kotlinx.coroutines.delay

sealed class KeyValidationState {
    object Idle : KeyValidationState()
    object Missing : KeyValidationState()
    data class Malformed(
        val reason: String,
        val tip: String
    ) : KeyValidationState()

    data class Validating(
        val attempt: Int = 1,
        val maxAttempts: Int = 2
    ) : KeyValidationState()

    data class Success(
        val message: String,
        val latencyMs: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : KeyValidationState()

    data class Failed(
        val reason: String,
        val userFriendlyMessage: String,
        val canRetry: Boolean = true,
        val attempt: Int = 1,
        val suggestedAction: String
    ) : KeyValidationState()
}

sealed class SyntaxValidation {
    data class Valid(val cleanedKey: String) : SyntaxValidation()
    data class Missing(val message: String) : SyntaxValidation()
    data class Malformed(val reason: String, val tip: String) : SyntaxValidation()
}

class GeminiKeyValidator(
    private val geminiProvider: GeminiProvider
) {

    fun sanitizeKey(rawKey: String): String {
        return rawKey.trim()
            .trim('"', '\'')
            .replace("\n", "")
            .replace("\r", "")
            .trim()
    }

    fun validateSyntax(rawKey: String): SyntaxValidation {
        val clean = sanitizeKey(rawKey)
        if (clean.isBlank()) {
            return SyntaxValidation.Missing("Gemini API key is missing. Please provide a key from Google AI Studio.")
        }

        if (clean.contains(" ")) {
            return SyntaxValidation.Malformed(
                reason = "API key contains spaces.",
                tip = "Ensure no spaces were copied at the beginning, end, or inside the key."
            )
        }

        val placeholders = listOf(
            "YOUR_API_KEY",
            "YOUR_GEMINI_KEY",
            "API_KEY",
            "TODO",
            "REPLACE_ME",
            "ENTER_KEY_HERE",
            "PASTE_KEY_HERE",
            "NONE"
        )
        if (placeholders.any { clean.equals(it, ignoreCase = true) }) {
            return SyntaxValidation.Malformed(
                reason = "Placeholder value detected.",
                tip = "Please replace the placeholder with an active API key from Google AI Studio."
            )
        }

        if (clean.length < 20) {
            return SyntaxValidation.Malformed(
                reason = "Key length (${clean.length} characters) is too short.",
                tip = "Google Gemini API keys are typically ~39 characters long (e.g. AIzaSy...)."
            )
        }

        // Check for disallowed characters in API keys
        val allowedPattern = Regex("^[A-Za-z0-9_\\-]+$")
        if (!allowedPattern.matches(clean)) {
            return SyntaxValidation.Malformed(
                reason = "Key contains invalid characters or special symbols.",
                tip = "Standard Gemini API keys contain only letters, numbers, underscores, and hyphens."
            )
        }

        return SyntaxValidation.Valid(clean)
    }

    suspend fun validateKeyWithServer(
        rawKey: String,
        baseUrl: String? = null,
        maxRetries: Int = 2,
        onProgress: ((KeyValidationState.Validating) -> Unit)? = null
    ): KeyValidationState {
        val syntax = validateSyntax(rawKey)
        when (syntax) {
            is SyntaxValidation.Missing -> return KeyValidationState.Missing
            is SyntaxValidation.Malformed -> return KeyValidationState.Malformed(syntax.reason, syntax.tip)
            is SyntaxValidation.Valid -> { /* proceed */ }
        }

        val cleanKey = (syntax as SyntaxValidation.Valid).cleanedKey
        var lastStatus: ConnectionStatus? = null

        for (attempt in 1..maxRetries) {
            onProgress?.invoke(KeyValidationState.Validating(attempt = attempt, maxAttempts = maxRetries))
            val startTime = System.currentTimeMillis()
            val status = geminiProvider.testConnection(cleanKey, baseUrl)
            val elapsed = System.currentTimeMillis() - startTime
            lastStatus = status

            when (status) {
                is ConnectionStatus.Connected -> {
                    return KeyValidationState.Success(
                        message = "Key verified successfully! Connected to Google Gemini 2.5 Flash.",
                        latencyMs = elapsed
                    )
                }

                is ConnectionStatus.InvalidKey -> {
                    // Fatal key rejection by Google API: no point retrying without modifying key
                    return KeyValidationState.Failed(
                        reason = "Authentication Rejected (Invalid Key)",
                        userFriendlyMessage = "Google AI Studio rejected this API key. The key was not recognized or has been disabled.",
                        canRetry = true,
                        attempt = attempt,
                        suggestedAction = "Verify your API key at aistudio.google.com and check that no IP/referrer restrictions block Android requests."
                    )
                }

                is ConnectionStatus.QuotaExceeded -> {
                    return KeyValidationState.Failed(
                        reason = "Quota Limit Exceeded",
                        userFriendlyMessage = "The quota limit for this Gemini API key has been exceeded or rate limited.",
                        canRetry = true,
                        attempt = attempt,
                        suggestedAction = "Check your billing and rate limits in Google AI Studio or Cloud Console."
                    )
                }

                is ConnectionStatus.NotConfigured -> {
                    return KeyValidationState.Missing
                }

                is ConnectionStatus.Error -> {
                    // Possible transient network glitch: wait and retry if attempts remain
                    if (attempt < maxRetries) {
                        delay(1200L * attempt)
                    }
                }

                is ConnectionStatus.Testing -> {
                    // transient
                }
            }
        }

        val detail = (lastStatus as? ConnectionStatus.Error)?.message ?: "Unable to contact Google Gemini servers."
        return KeyValidationState.Failed(
            reason = "Connection & Authentication Failed",
            userFriendlyMessage = "Could not verify your API key with Google servers: $detail",
            canRetry = true,
            attempt = maxRetries,
            suggestedAction = "Check your device's Wi-Fi or cellular internet connection and tap 'Retry' below."
        )
    }
}
