package com.example.ai

sealed class ConnectionStatus {
    object NotConfigured : ConnectionStatus()
    object Testing : ConnectionStatus()
    object Connected : ConnectionStatus()
    object InvalidKey : ConnectionStatus()
    object QuotaExceeded : ConnectionStatus()
    data class PermissionDenied(val message: String) : ConnectionStatus()
    data class UnsupportedModel(val message: String) : ConnectionStatus()
    data class NetworkError(val message: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

data class ToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)

data class AIResponse(
    val text: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val providerUsed: String,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

interface AIProvider {
    val id: String
    val displayName: String

    suspend fun testConnection(apiKey: String, baseUrl: String? = null): ConnectionStatus

    suspend fun generate(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>>, // list of (sender, text)
        baseUrl: String? = null,
        onChunk: ((String) -> Unit)? = null
    ): AIResponse

    suspend fun fetchModels(apiKey: String, baseUrl: String? = null): List<String>
}
