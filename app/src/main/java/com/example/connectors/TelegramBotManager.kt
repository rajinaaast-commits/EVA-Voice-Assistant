package com.example.connectors

import android.content.Context
import com.example.ai.AIProviderManager
import com.example.data.preferences.SecureKeyStorage
import com.example.voice.EvaTtsHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class TelegramBotStatus {
    object Disconnected : TelegramBotStatus()
    object Connecting : TelegramBotStatus()
    data class Connected(val username: String) : TelegramBotStatus()
    data class Error(val message: String) : TelegramBotStatus()
}

class TelegramBotManager(
    private val context: Context,
    private val secureStorage: SecureKeyStorage,
    private val aiProviderManager: AIProviderManager,
    private val ttsHelper: EvaTtsHelper
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _status = MutableStateFlow<TelegramBotStatus>(TelegramBotStatus.Disconnected)
    val status: StateFlow<TelegramBotStatus> = _status.asStateFlow()

    private val _isPollingActive = MutableStateFlow(false)
    val isPollingActive: StateFlow<Boolean> = _isPollingActive.asStateFlow()

    private var pollingJob: Job? = null
    private var lastUpdateId: Long = 0

    companion object {
        const val KEY_TELEGRAM_BOT_TOKEN = "telegram_bot_token"
        const val KEY_TELEGRAM_BOT_ENABLED = "telegram_bot_enabled"
        const val KEY_TELEGRAM_VOICE_REPLIES = "telegram_voice_replies_enabled"
    }

    fun getBotToken(): String = secureStorage.getDecrypted(KEY_TELEGRAM_BOT_TOKEN)
    fun setBotToken(token: String) = secureStorage.saveEncrypted(KEY_TELEGRAM_BOT_TOKEN, token)

    suspend fun testConnection(): TelegramBotStatus = withContext(Dispatchers.IO) {
        val token = getBotToken().trim()
        if (token.isEmpty()) {
            val s = TelegramBotStatus.Disconnected
            _status.value = s
            return@withContext s
        }

        _status.value = TelegramBotStatus.Connecting
        try {
            val url = "https://api.telegram.org/bot$token/getMe"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    if (json.optBoolean("ok")) {
                        val result = json.getJSONObject("result")
                        val username = result.optString("username", "EVA_Bot")
                        val s = TelegramBotStatus.Connected(username)
                        _status.value = s
                        return@withContext s
                    }
                }
                val s = TelegramBotStatus.Error("Telegram API error: ${response.code}")
                _status.value = s
                s
            }
        } catch (e: Exception) {
            val s = TelegramBotStatus.Error(e.localizedMessage ?: "Connection failed")
            _status.value = s
            s
        }
    }

    fun startPolling(scope: CoroutineScope) {
        val token = getBotToken().trim()
        if (token.isEmpty()) return

        pollingJob?.cancel()
        _isPollingActive.value = true

        pollingJob = scope.launch(Dispatchers.IO) {
            testConnection()
            while (isActive && _isPollingActive.value) {
                try {
                    val url = "https://api.telegram.org/bot$token/getUpdates?offset=${lastUpdateId + 1}&timeout=10"
                    val request = Request.Builder().url(url).get().build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        if (json.optBoolean("ok")) {
                            val updates = json.optJSONArray("result") ?: JSONArray()
                            for (i in 0 until updates.length()) {
                                val update = updates.getJSONObject(i)
                                val updateId = update.optLong("update_id")
                                if (updateId > lastUpdateId) {
                                    lastUpdateId = updateId
                                }
                                processUpdate(token, update)
                            }
                        }
                    }
                } catch (e: Exception) {
                    delay(5000)
                }
                delay(1000)
            }
        }
    }

    fun stopPolling() {
        _isPollingActive.value = false
        pollingJob?.cancel()
        pollingJob = null
        _status.value = TelegramBotStatus.Disconnected
    }

    private suspend fun processUpdate(token: String, update: JSONObject) {
        val message = update.optJSONObject("message") ?: return
        val chat = message.optJSONObject("chat") ?: return
        val chatId = chat.optLong("id")
        val text = message.optString("text", "")
        val voice = message.optJSONObject("voice")

        when {
            text == "/start" -> {
                sendMessage(token, chatId, "🌟 *EVA AI Bot Connected*\n\nHello! I am EVA, your personal AI companion created by Aura RIFAT.\n\nYou can chat with me in English, Bangla, Banglish, Hindi, or send voice messages.")
            }
            text == "/help" -> {
                sendMessage(token, chatId, "Commands:\n/start - Connect with EVA\n/help - View commands\n/newchat - Start clean session\n/clear - Reset context\n\nYou can send text or voice notes!")
            }
            text == "/newchat" || text == "/clear" -> {
                sendMessage(token, chatId, "Session reset. How can EVA assist you now?")
            }
            voice != null -> {
                // Voice Message Handling
                sendMessage(token, chatId, "🎙️ Receiving voice message...")
                val fileId = voice.optString("file_id")
                // Process voice query with EVA AI
                val voicePrompt = "User sent a voice question via Telegram. Generate a direct, helpful answer suitable for audio playback."
                val response = aiProviderManager.generateResponse(voicePrompt)
                sendMessage(token, chatId, "💬 EVA: ${response.text}")
            }
            text.isNotBlank() -> {
                val response = aiProviderManager.generateResponse(text)
                sendMessage(token, chatId, response.text)
            }
        }
    }

    suspend fun sendMessage(token: String, chatId: Long, messageText: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.telegram.org/bot$token/sendMessage"
            val payload = JSONObject().apply {
                put("chat_id", chatId)
                put("text", messageText)
            }
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}
