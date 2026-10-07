package com.example.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider : AIProvider {
    override val id: String = "gemini"
    override val displayName: String = "Google Gemini"

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val DEFAULT_GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        const val PRIMARY_DEFAULT_MODEL = "gemini-2.5-flash"
        const val FALLBACK_MODEL_FAST = "gemini-2.0-flash"
        const val FALLBACK_MODEL_STABLE = "gemini-1.5-flash"
    }

    private fun cleanApiKey(rawKey: String): String {
        return rawKey.trim().trim('"', '\'').trim()
    }

    private fun resolveUrl(customBaseUrl: String?): String {
        return if (!customBaseUrl.isNullOrBlank()) {
            customBaseUrl.trim().removeSuffix("/")
        } else {
            DEFAULT_GEMINI_BASE_URL
        }
    }

    private fun resolveModelName(model: String): String {
        val trimmed = model.trim()
        return when {
            trimmed.isBlank() || trimmed.equals("auto", ignoreCase = true) -> PRIMARY_DEFAULT_MODEL
            trimmed == "gemini-3.5-flash" || trimmed == "gemini-3.1-pro-preview" -> PRIMARY_DEFAULT_MODEL
            else -> trimmed
        }
    }

    override suspend fun testConnection(apiKey: String, baseUrl: String?): ConnectionStatus =
        withContext(Dispatchers.IO) {
            val cleanKey = cleanApiKey(apiKey)
            if (cleanKey.isBlank()) return@withContext ConnectionStatus.NotConfigured

            val effectiveBaseUrl = resolveUrl(baseUrl)

            // 1. Try listing models (standard verification)
            try {
                val listUrl = "$effectiveBaseUrl/models?key=$cleanKey"
                val listRequest = Request.Builder()
                    .url(listUrl)
                    .addHeader("x-goog-api-key", cleanKey)
                    .get()
                    .build()

                client.newCall(listRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext ConnectionStatus.Connected
                    }

                    val code = response.code
                    val body = response.body?.string() ?: ""

                    // If models listing failed due to key restrictions or permission scopes,
                    // test a lightweight direct 1-token prompt on gemini-2.0-flash before giving up
                    if (code == 400 || code == 403 || code == 404) {
                        val pingResult = testDirectPing(cleanKey, effectiveBaseUrl, FALLBACK_MODEL_FAST)
                        if (pingResult is ConnectionStatus.Connected) {
                            return@withContext ConnectionStatus.Connected
                        }
                    }

                    val errMessage = parseErrorMessage(body)
                    return@withContext when {
                        code == 400 && (errMessage.contains("API key not valid", ignoreCase = true) || errMessage.contains("API_KEY_INVALID", ignoreCase = true)) ->
                            ConnectionStatus.InvalidKey
                        code == 401 || code == 403 ->
                            ConnectionStatus.InvalidKey
                        code == 429 || errMessage.contains("quota", ignoreCase = true) ->
                            ConnectionStatus.QuotaExceeded
                        errMessage.isNotBlank() ->
                            ConnectionStatus.Error(errMessage.take(80))
                        else ->
                            ConnectionStatus.Error("HTTP $code: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                // If list models had a network glitch, try fallback ping once
                val pingResult = testDirectPing(cleanKey, effectiveBaseUrl, FALLBACK_MODEL_FAST)
                if (pingResult is ConnectionStatus.Connected) {
                    return@withContext ConnectionStatus.Connected
                }
                ConnectionStatus.Error(e.localizedMessage ?: "Network connection failed")
            }
        }

    private fun testDirectPing(cleanKey: String, effectiveBaseUrl: String, model: String): ConnectionStatus {
        try {
            val endpoint = "$effectiveBaseUrl/models/$model:generateContent?key=$cleanKey"
            val bodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "ping") })
                        })
                    })
                })
            }
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("x-goog-api-key", cleanKey)
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return ConnectionStatus.Connected
                }
                val code = response.code
                val body = response.body?.string() ?: ""
                val errMessage = parseErrorMessage(body)
                return when {
                    code == 400 && (errMessage.contains("API key not valid", ignoreCase = true) || errMessage.contains("API_KEY_INVALID", ignoreCase = true)) ->
                        ConnectionStatus.InvalidKey
                    code == 401 || code == 403 ->
                        ConnectionStatus.InvalidKey
                    code == 429 ->
                        ConnectionStatus.QuotaExceeded
                    else ->
                        ConnectionStatus.Error(if (errMessage.isNotBlank()) errMessage.take(80) else "HTTP $code")
                }
            }
        } catch (_: Exception) {
            return ConnectionStatus.Error("Connection timed out")
        }
    }

    private fun parseErrorMessage(rawJson: String): String {
        return try {
            val json = JSONObject(rawJson)
            json.optJSONObject("error")?.optString("message") ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    override suspend fun generate(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>>,
        baseUrl: String?,
        onChunk: ((String) -> Unit)?
    ): AIResponse = withContext(Dispatchers.IO) {
        val cleanKey = cleanApiKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext AIResponse(
                text = "Gemini API key is not configured. Please add your key in Settings or API Setup.",
                providerUsed = "Gemini",
                isSuccess = false,
                errorMessage = "API key missing"
            )
        }

        val targetModel = resolveModelName(model)
        val isStream = onChunk != null
        val effectiveBaseUrl = resolveUrl(baseUrl)

        // Attempt generation with selected model
        val result = executeGenerate(cleanKey, targetModel, prompt, systemInstruction, history, effectiveBaseUrl, isStream, onChunk)

        // If failure was due to model not found (404), auto-fallback to FALLBACK_MODEL_FAST
        if (!result.isSuccess && result.errorMessage?.contains("404") == true && targetModel != FALLBACK_MODEL_FAST) {
            return@withContext executeGenerate(cleanKey, FALLBACK_MODEL_FAST, prompt, systemInstruction, history, effectiveBaseUrl, isStream, onChunk)
        }

        result
    }

    private fun executeGenerate(
        cleanKey: String,
        targetModel: String,
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>>,
        effectiveBaseUrl: String,
        isStream: Boolean,
        onChunk: ((String) -> Unit)?
    ): AIResponse {
        val endpoint = if (isStream) {
            "$effectiveBaseUrl/models/$targetModel:streamGenerateContent?alt=sse&key=$cleanKey"
        } else {
            "$effectiveBaseUrl/models/$targetModel:generateContent?key=$cleanKey"
        }

        try {
            val root = JSONObject()

            // System instruction
            if (systemInstruction.isNotBlank()) {
                root.put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
            }

            // Contents array
            val contentsArray = JSONArray()
            for ((sender, msg) in history.takeLast(10)) {
                val role = if (sender.equals("user", ignoreCase = true)) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg) })
                    })
                })
            }
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            })
            root.put("contents", contentsArray)

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("x-goog-api-key", cleanKey)
                .post(root.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val code = response.code
                    val parsedMsg = parseErrorMessage(errorBody)
                    val errMsg = when {
                        code == 400 && parsedMsg.contains("API key not valid", ignoreCase = true) ->
                            "Invalid Gemini API Key. Please verify your key at Google AI Studio."
                        code == 401 || code == 403 ->
                            "Unauthorized Gemini API Key. Check project access or restrictions."
                        code == 404 ->
                            "Model '$targetModel' not found (HTTP 404). Falling back to standard model."
                        code == 429 ->
                            "Gemini API Quota exceeded. Please try again later or check your Google Cloud quota."
                        parsedMsg.isNotBlank() ->
                            "Gemini API: $parsedMsg"
                        else ->
                            "Gemini API error ($code): ${response.message}"
                    }
                    return AIResponse(
                        text = errMsg,
                        providerUsed = "Gemini",
                        isSuccess = false,
                        errorMessage = errMsg
                    )
                }

                if (isStream && onChunk != null) {
                    val reader = response.body?.charStream()?.buffered()
                    val fullText = StringBuilder()
                    var line: String?
                    while (reader?.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: continue
                        if (!trimmed.startsWith("data:")) continue
                        val payload = trimmed.removePrefix("data:").trim()
                        if (payload.isEmpty() || payload == "[DONE]") continue
                        try {
                            val chunkJson = JSONObject(payload)
                            val candidates = chunkJson.optJSONArray("candidates")
                            val cand = candidates?.optJSONObject(0)
                            val parts = cand?.optJSONObject("content")?.optJSONArray("parts")
                            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""
                            if (text.isNotEmpty()) {
                                fullText.append(text)
                                onChunk(text)
                            }
                        } catch (_: Exception) {
                            // continue parsing next stream chunk
                        }
                    }
                    val resultText = fullText.toString().ifBlank { "No response generated." }
                    return AIResponse(
                        text = resultText,
                        providerUsed = "Gemini",
                        isSuccess = true
                    )
                } else {
                    val respBody = response.body?.string() ?: ""
                    val json = JSONObject(respBody)
                    val candidates = json.optJSONArray("candidates")
                    val cand = candidates?.optJSONObject(0)
                    val parts = cand?.optJSONObject("content")?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text", "") ?: "No response generated."
                    return AIResponse(
                        text = text,
                        providerUsed = "Gemini",
                        isSuccess = true
                    )
                }
            }
        } catch (e: Exception) {
            return AIResponse(
                text = "Connection to Gemini failed: ${e.localizedMessage ?: "Unknown network error"}",
                providerUsed = "Gemini",
                isSuccess = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    override suspend fun fetchModels(apiKey: String, baseUrl: String?): List<String> =
        withContext(Dispatchers.IO) {
            val cleanKey = cleanApiKey(apiKey)
            val defaultModels = listOf(
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-2.0-flash",
                "gemini-1.5-flash"
            )
            if (cleanKey.isBlank()) return@withContext defaultModels

            try {
                val effectiveBaseUrl = resolveUrl(baseUrl)
                val url = "$effectiveBaseUrl/models?key=$cleanKey"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", cleanKey)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext defaultModels
                    val body = response.body?.string() ?: return@withContext defaultModels
                    val json = JSONObject(body)
                    val modelsArray = json.optJSONArray("models") ?: return@withContext defaultModels
                    val result = mutableListOf<String>()
                    for (i in 0 until modelsArray.length()) {
                        val obj = modelsArray.optJSONObject(i) ?: continue
                        val name = obj.optString("name", "").removePrefix("models/")
                        val supportedMethods = obj.optJSONArray("supportedGenerationMethods")
                        var canGenerate = false
                        if (supportedMethods != null) {
                            for (j in 0 until supportedMethods.length()) {
                                if (supportedMethods.optString(j) == "generateContent") {
                                    canGenerate = true
                                    break
                                }
                            }
                        }
                        if (canGenerate && name.isNotBlank()) {
                            result.add(name)
                        }
                    }
                    if (result.isNotEmpty()) result else defaultModels
                }
            } catch (_: Exception) {
                defaultModels
            }
        }
}
