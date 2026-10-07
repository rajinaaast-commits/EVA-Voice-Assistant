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

class OmniRouteProvider : AIProvider {
    override val id: String = "omniroute"
    override val displayName: String = "OmniRoute"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun cleanBaseUrl(url: String?): String {
        val raw = if (url.isNullOrBlank()) "http://localhost:20128/v1" else url.trim()
        return raw.removeSuffix("/")
    }

    override suspend fun testConnection(apiKey: String, baseUrl: String?): ConnectionStatus =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext ConnectionStatus.NotConfigured
            val rootUrl = cleanBaseUrl(baseUrl)
            try {
                // First attempt models endpoint
                val request = Request.Builder()
                    .url("$rootUrl/models")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    when (response.code) {
                        200 -> ConnectionStatus.Connected
                        401, 403 -> ConnectionStatus.InvalidKey
                        429 -> ConnectionStatus.QuotaExceeded
                        404 -> {
                            // Try test completion
                            testCompletionProbe(apiKey, rootUrl)
                        }
                        else -> ConnectionStatus.Error("HTTP ${response.code}: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                // If local server or offline, provide clear error
                ConnectionStatus.Error(e.localizedMessage ?: "OmniRoute server unreachable at $rootUrl")
            }
        }

    private fun testCompletionProbe(apiKey: String, rootUrl: String): ConnectionStatus {
        return try {
            val probeBody = JSONObject().apply {
                put("model", "auto")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
                put("max_tokens", 5)
            }
            val req = Request.Builder()
                .url("$rootUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(probeBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                when (resp.code) {
                    200 -> ConnectionStatus.Connected
                    401, 403 -> ConnectionStatus.InvalidKey
                    429 -> ConnectionStatus.QuotaExceeded
                    else -> ConnectionStatus.Error("HTTP ${resp.code}: ${resp.message}")
                }
            }
        } catch (e: Exception) {
            ConnectionStatus.Error(e.localizedMessage ?: "OmniRoute connection failed")
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
                text = "OmniRoute API key is not configured. Please add your key in Settings or API Setup.",
                providerUsed = "OmniRoute",
                isSuccess = false,
                errorMessage = "API key missing"
            )
        }

        val rootUrl = cleanBaseUrl(baseUrl)
        val endpoint = "$rootUrl/chat/completions"
        val targetModel = if (model.isBlank()) "auto" else model
        val isStream = onChunk != null

        try {
            val root = JSONObject()
            root.put("model", targetModel)
            root.put("stream", isStream)

            val messages = JSONArray()
            if (systemInstruction.isNotBlank()) {
                messages.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
            }

            for ((sender, text) in history.takeLast(10)) {
                val role = if (sender.equals("user", ignoreCase = true)) "user" else "assistant"
                messages.put(JSONObject().apply {
                    put("role", role)
                    put("content", text)
                })
            }

            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
            root.put("messages", messages)

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(root.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val code = response.code
                    val errMsg = when (code) {
                        401, 403 -> "Invalid OmniRoute API Key or unauthorized access."
                        429 -> "OmniRoute API Quota exceeded."
                        else -> "OmniRoute error ($code): $errorBody"
                    }
                    return@withContext AIResponse(
                        text = errMsg,
                        providerUsed = "OmniRoute",
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
                            val choices = chunkJson.optJSONArray("choices")
                            val delta = choices?.optJSONObject(0)?.optJSONObject("delta")
                            val content = delta?.optString("content", "") ?: ""
                            if (content.isNotEmpty()) {
                                fullText.append(content)
                                onChunk(content)
                            }
                        } catch (_: Exception) {
                            // continue
                        }
                    }
                    val resultText = fullText.toString().ifBlank { "No response generated." }
                    return@withContext AIResponse(
                        text = resultText,
                        providerUsed = "OmniRoute",
                        isSuccess = true
                    )
                } else {
                    val respBody = response.body?.string() ?: ""
                    val json = JSONObject(respBody)
                    val choices = json.optJSONArray("choices")
                    val messageObj = choices?.optJSONObject(0)?.optJSONObject("message")
                    val content = messageObj?.optString("content", "") ?: "No response generated."
                    return@withContext AIResponse(
                        text = content,
                        providerUsed = "OmniRoute",
                        isSuccess = true
                    )
                }
            }
        } catch (e: Exception) {
            AIResponse(
                text = "OmniRoute connection failed: ${e.localizedMessage}",
                providerUsed = "OmniRoute",
                isSuccess = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    override suspend fun fetchModels(apiKey: String, baseUrl: String?): List<String> =
        withContext(Dispatchers.IO) {
            val defaultModels = listOf("auto", "claude-3-5-sonnet", "gpt-4o", "gemini-1.5-pro-routed", "deepseek-r1")
            if (apiKey.isBlank()) return@withContext defaultModels
            val rootUrl = cleanBaseUrl(baseUrl)
            try {
                val request = Request.Builder()
                    .url("$rootUrl/models")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@withContext defaultModels
                        val json = JSONObject(body)
                        val dataArray = json.optJSONArray("data") ?: return@withContext defaultModels
                        val models = mutableListOf("auto")
                        for (i in 0 until dataArray.length()) {
                            val obj = dataArray.optJSONObject(i)
                            val id = obj?.optString("id", "") ?: ""
                            if (id.isNotBlank() && !models.contains(id)) {
                                models.add(id)
                            }
                        }
                        if (models.isNotEmpty()) models else defaultModels
                    } else {
                        defaultModels
                    }
                }
            } catch (_: Exception) {
                defaultModels
            }
        }
}
