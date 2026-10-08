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

    /**
     * Trims ONLY accidental surrounding (leading/trailing) whitespace from pasted keys.
     * Never modifies or replaces any characters inside the actual key.
     */
    fun sanitizeKey(rawKey: String): String {
        return rawKey.trim()
    }

    /**
     * Checks if the key has basic syntax readiness.
     * Does NOT hardcode key format or reject valid characters such as '_', '-', or '.'.
     */
    fun validateSyntax(rawKey: String): SyntaxValidation {
        if (rawKey.isBlank()) {
            return SyntaxValidation.Missing("Gemini API key is missing. Please provide a key from Google AI Studio.")
        }

        // Check for accidental surrounding whitespace
        val hasLeadingOrTrailingWhitespace = rawKey.startsWith(" ") || rawKey.endsWith(" ") ||
                rawKey.startsWith("\n") || rawKey.endsWith("\n") ||
                rawKey.startsWith("\r") || rawKey.endsWith("\r") ||
                rawKey.startsWith("\t") || rawKey.endsWith("\t")

        if (hasLeadingOrTrailingWhitespace) {
            return SyntaxValidation.Malformed(
                reason = "Whitespace detected around key.",
                tip = "Accidental leading or trailing whitespace was detected. Tap 'Clean & Test Key' to trim it and authenticate."
            )
        }

        val clean = rawKey.trim()

        // Check for internal whitespace (spaces or line breaks inside the key)
        if (clean.contains(" ") || clean.contains("\n") || clean.contains("\r") || clean.contains("\t")) {
            return SyntaxValidation.Malformed(
                reason = "API key contains spaces or line breaks.",
                tip = "Ensure no spaces or line breaks exist inside the key."
            )
        }

        // Check for template placeholder values
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

        // Key is acceptable for server testing without restrictive regex or assumptions
        return SyntaxValidation.Valid(clean)
    }

    suspend fun validateKeyWithServer(
        rawKey: String,
        baseUrl: String? = null,
        maxRetries: Int = 2,
        onProgress: ((KeyValidationState.Validating) -> Unit)? = null
    ): KeyValidationState {
        // Clean accidental surrounding whitespace before server test
        val cleanKey = sanitizeKey(rawKey)
        val syntax = validateSyntax(cleanKey)
        when (syntax) {
            is SyntaxValidation.Missing -> return KeyValidationState.Missing
            is SyntaxValidation.Malformed -> return KeyValidationState.Malformed(syntax.reason, syntax.tip)
            is SyntaxValidation.Valid -> { /* proceed with cleanKey */ }
        }

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
                        message = "Key verified successfully! Connected to Google Gemini.",
                        latencyMs = elapsed
                    )
                }

                is ConnectionStatus.InvalidKey -> {
                    // Fatal authentication error: invalid or revoked key
                    return KeyValidationState.Failed(
                        reason = "Authentication Error",
                        userFriendlyMessage = "Gemini API key is invalid or revoked.",
                        canRetry = true,
                        attempt = attempt,
                        suggestedAction = "Verify your API key at aistudio.google.com and generate a new key if this one was revoked or expired."
                    )
                }

                is ConnectionStatus.PermissionDenied -> {
                    return KeyValidationState.Failed(
                        reason = "API Permission Error",
                        userFriendlyMessage = "Permission denied: ${status.message.ifBlank { "The API key does not have permission to access the Gemini API." }}",
                        canRetry = true,
                        attempt = attempt,
                        suggestedAction = "In Google Cloud Console, check that the Generative Language API is enabled and that no Android package or IP restrictions block requests."
                    )
                }

                is ConnectionStatus.QuotaExceeded -> {
                    return KeyValidationState.Failed(
                        reason = "Quota Limit Exceeded",
                        userFriendlyMessage = "The quota limit for this Gemini API key has been exceeded or rate limited.",
                        canRetry = true,
                        attempt = attempt,
                        suggestedAction = "Check your billing and rate limits in Google AI Studio or Google Cloud Console."
                    )
                }

                is ConnectionStatus.UnsupportedModel -> {
                    return KeyValidationState.Failed(
                        reason = "Unsupported Model",
                        userFriendlyMessage = "The specified model is not supported or was not found: ${status.message}",
                        canRetry = true,
                        attempt = attempt,
                        suggestedAction = "Select a recommended model such as gemini-2.5-flash or gemini-2.0-flash."
                    )
                }

                is ConnectionStatus.NetworkError -> {
                    if (attempt < maxRetries) {
                        delay(1000L * attempt)
                    }
                }

                is ConnectionStatus.NotConfigured -> {
                    return KeyValidationState.Missing
                }

                is ConnectionStatus.Error -> {
                    if (attempt < maxRetries) {
                        delay(1000L * attempt)
                    }
                }

                is ConnectionStatus.Testing -> {}
            }
        }

        // Exhausted retries: map to the appropriate specific failure
        return when (val finalStatus = lastStatus) {
            is ConnectionStatus.InvalidKey -> KeyValidationState.Failed(
                reason = "Authentication Error",
                userFriendlyMessage = "Gemini API key is invalid or revoked.",
                canRetry = true,
                attempt = maxRetries,
                suggestedAction = "Verify your API key at aistudio.google.com."
            )

            is ConnectionStatus.PermissionDenied -> KeyValidationState.Failed(
                reason = "API Permission Error",
                userFriendlyMessage = "Permission denied: ${finalStatus.message}",
                canRetry = true,
                attempt = maxRetries,
                suggestedAction = "Check key restrictions in Google Cloud Console."
            )

            is ConnectionStatus.QuotaExceeded -> KeyValidationState.Failed(
                reason = "Quota Limit Exceeded",
                userFriendlyMessage = "Quota or rate limit exceeded for this Gemini API key.",
                canRetry = true,
                attempt = maxRetries,
                suggestedAction = "Check your billing and quotas in Google AI Studio."
            )

            is ConnectionStatus.UnsupportedModel -> KeyValidationState.Failed(
                reason = "Unsupported Model",
                userFriendlyMessage = "Model error: ${finalStatus.message}",
                canRetry = true,
                attempt = maxRetries,
                suggestedAction = "Switch to a recommended model (e.g. gemini-2.5-flash)."
            )

            is ConnectionStatus.NetworkError -> KeyValidationState.Failed(
                reason = "Network Connection Error",
                userFriendlyMessage = "Unable to connect to Google Gemini servers: ${finalStatus.message}",
                canRetry = true,
                attempt = maxRetries,
                suggestedAction = "Check your device's Wi-Fi or cellular internet connection and tap 'Retry Authentication'."
            )

            else -> {
                val detail = (finalStatus as? ConnectionStatus.Error)?.message ?: "Unable to contact Google Gemini servers."
                KeyValidationState.Failed(
                    reason = "Connection Failed",
                    userFriendlyMessage = detail,
                    canRetry = true,
                    attempt = maxRetries,
                    suggestedAction = "Check your internet connection and tap 'Retry Authentication'."
                )
            }
        }
    }
}
