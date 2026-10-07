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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    override suspend fun testConnection(apiKey: String, baseUrl: String?): ConnectionStatus =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext ConnectionStatus.NotConfigured
            try {
                val url = "$baseUrl/models/gemini-3.5-flash:generateContent?key=$apiKey"
                val bodyJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", "ping") })
                            })
                        })
                    })
                }
                val request = Request.Builder()
                    .url(url)
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    when (response.code) {
                        200 -> ConnectionStatus.Connected
                        400, 401, 403 -> ConnectionStatus.InvalidKey
                        429 -> ConnectionStatus.QuotaExceeded
                        else -> ConnectionStatus.Error("HTTP ${response.code}: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                ConnectionStatus.Error(e.localizedMessage ?: "Network connection failed")
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
        if (apiKey.isBlank()) {
            return@withContext AIResponse(
                text = "Gemini API key is not configured. Please add your key in Settings or API Setup.",
                providerUsed = "Gemini",
                isSuccess = false,
                errorMessage = "API key missing"
            )
        }

        val targetModel = if (model.isBlank() || model == "auto") "gemini-3.5-flash" else model
        val isStream = onChunk != null
        val endpoint = if (isStream) {
            "$baseUrl/models/$targetModel:streamGenerateContent?alt=sse&key=$apiKey"
        } else {
            "$baseUrl/models/$targetModel:generateContent?key=$apiKey"
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
            // History
            for ((sender, msg) in history.takeLast(10)) {
                val role = if (sender.equals("user", ignoreCase = true)) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg) })
                    })
                })
            }
            // Current prompt
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            })
            root.put("contents", contentsArray)

            val request = Request.Builder()
                .url(endpoint)
                .post(root.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val code = response.code
                    val errMsg = when (code) {
                        400, 401, 403 -> "Invalid Gemini API Key or unauthorized access."
                        429 -> "Gemini API Quota exceeded. Please try again later."
                        else -> "Gemini API error ($code): $errorBody"
                    }
                    return@withContext AIResponse(
                        text = errMsg,
                        providerUsed = "Gemini",
                        isSuccess = false,
                        errorMessage = errMsg
                    )
                }

                if (isStream) {
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
                            // continue parsing next line
                        }
                    }
                    val resultText = fullText.toString().ifBlank { "No response generated." }
                    return@withContext AIResponse(
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
                    return@withContext AIResponse(
                        text = text,
                        providerUsed = "Gemini",
                        isSuccess = true
                    )
                }
            }
        } catch (e: Exception) {
            AIResponse(
                text = "Connection error: ${e.localizedMessage}",
                providerUsed = "Gemini",
                isSuccess = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    override suspend fun fetchModels(apiKey: String, baseUrl: String?): List<String> =
        withContext(Dispatchers.IO) {
            val defaultModels = listOf(
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview",
                "gemini-3.1-flash-lite-preview",
                "gemini-2.5-flash-image"
            )
            if (apiKey.isBlank()) return@withContext defaultModels
            try {
                val url = "$baseUrl/models?key=$apiKey"
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
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
                                    if (supportedMethods.getString(j) == "generateContent") {
                                        canGenerate = true
                                        break
                                    }
                                }
                            }
                            if (canGenerate && (name.contains("3.") || name.contains("2.5") || name.contains("flash") || name.contains("pro"))) {
                                result.add(name)
                            }
                        }
                        if (result.isNotEmpty()) result else defaultModels
                    } else {
                        defaultModels
                    }
                }
            } catch (_: Exception) {
                defaultModels
            }
        }
}
