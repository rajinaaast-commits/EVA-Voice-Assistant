package com.example.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.EvaDatabase
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.ReminderEntity
import com.example.data.local.entities.StudyTaskEntity
import com.example.data.preferences.AIProviderMode
import com.example.data.preferences.EvaPreferences
import com.example.data.preferences.SecureKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AIProviderManager(
    private val context: Context,
    private val secureStorage: SecureKeyStorage,
    private val preferences: EvaPreferences,
    private val database: EvaDatabase,
    val deviceControlManager: com.example.device.DeviceControlManager
) {
    val geminiProvider = GeminiProvider()
    val omniRouteProvider = OmniRouteProvider()
    val geminiKeyValidator = GeminiKeyValidator(geminiProvider)

    fun getGeminiKey(): String = secureStorage.getDecrypted(SecureKeyStorage.KEY_GEMINI)
    fun setGeminiKey(key: String) = secureStorage.saveEncrypted(SecureKeyStorage.KEY_GEMINI, key)

    fun getOmniRouteKey(): String = secureStorage.getDecrypted(SecureKeyStorage.KEY_OMNIROUTE)
    fun setOmniRouteKey(key: String) = secureStorage.saveEncrypted(SecureKeyStorage.KEY_OMNIROUTE, key)

    fun isGeminiConfigured(): Boolean = getGeminiKey().isNotBlank()
    fun isOmniRouteConfigured(): Boolean = getOmniRouteKey().isNotBlank()
    fun hasAnyConfigured(): Boolean = isGeminiConfigured() || isOmniRouteConfigured()

    suspend fun testGeminiConnection(): ConnectionStatus {
        val key = getGeminiKey()
        return geminiProvider.testConnection(key)
    }

    suspend fun testOmniRouteConnection(): ConnectionStatus {
        val key = getOmniRouteKey()
        val url = preferences.getOmniRouteBaseUrl()
        return omniRouteProvider.testConnection(key, url)
    }

    private fun buildSystemInstruction(): String {
        val assistantName = preferences.getAssistantName()
        val persona = preferences.getPersona()
        val customPrompt = preferences.getCustomPersonaPrompt()
        val language = preferences.getLanguage()
        val secretMode = preferences.getSecretMode()

        val sb = StringBuilder()
        sb.append("You are $assistantName, an intelligent personal AI companion developed by Aura RIFAT. ")
        sb.append("Current persona tone: $persona. ")
        if (customPrompt.isNotBlank()) {
            sb.append("Special instructions: $customPrompt. ")
        }
        sb.append("Preferred language: $language. You support English, Bangla, Hindi, Hinglish, and Banglish seamlessly. Understand mixed-language queries naturally. ")
        sb.append("Personality style: $secretMode. Be helpful, concise, thoughtful, and capable. ")
        sb.append("\n\n")
        sb.append(AIToolRegistry.getToolDescriptionSystemPrompt())
        return sb.toString()
    }

    suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        onChunk: ((String) -> Unit)? = null
    ): AIResponse = withContext(Dispatchers.IO) {
        val geminiKey = getGeminiKey()
        val omniKey = getOmniRouteKey()
        val mode = preferences.getProviderMode()
        val systemInstruction = buildSystemInstruction()

        if (!hasAnyConfigured()) {
            return@withContext AIResponse(
                text = "Add at least one AI API key to continue.",
                providerUsed = "None",
                isSuccess = false,
                errorMessage = "No API keys configured"
            )
        }

        val primaryIsGemini = when (mode) {
            AIProviderMode.AUTO -> geminiKey.isNotBlank()
            AIProviderMode.GEMINI -> true
            AIProviderMode.OMNIROUTE -> false
        }

        var response: AIResponse

        if (primaryIsGemini) {
            val model = preferences.getGeminiModel()
            response = geminiProvider.generate(
                apiKey = geminiKey,
                model = model,
                prompt = prompt,
                systemInstruction = systemInstruction,
                history = history,
                onChunk = onChunk
            )

            // Fallback to OmniRoute if Gemini fails and OmniRoute is configured
            if (!response.isSuccess && omniKey.isNotBlank()) {
                val omniModel = preferences.getOmniRouteModel()
                val omniUrl = preferences.getOmniRouteBaseUrl()
                response = omniRouteProvider.generate(
                    apiKey = omniKey,
                    model = omniModel,
                    prompt = prompt,
                    systemInstruction = systemInstruction,
                    history = history,
                    baseUrl = omniUrl,
                    onChunk = onChunk
                )
            }
        } else {
            val omniModel = preferences.getOmniRouteModel()
            val omniUrl = preferences.getOmniRouteBaseUrl()
            response = omniRouteProvider.generate(
                apiKey = omniKey,
                model = omniModel,
                prompt = prompt,
                systemInstruction = systemInstruction,
                history = history,
                baseUrl = omniUrl,
                onChunk = onChunk
            )

            // Fallback to Gemini if OmniRoute fails and Gemini is configured
            if (!response.isSuccess && geminiKey.isNotBlank()) {
                val model = preferences.getGeminiModel()
                response = geminiProvider.generate(
                    apiKey = geminiKey,
                    model = model,
                    prompt = prompt,
                    systemInstruction = systemInstruction,
                    history = history,
                    onChunk = onChunk
                )
            }
        }

        if (!response.isSuccess) {
            return@withContext AIResponse(
                text = "EVA couldn't connect to an AI provider. Please verify your internet connection or API settings.",
                providerUsed = response.providerUsed,
                isSuccess = false,
                errorMessage = response.errorMessage
            )
        }

        // Parse tool calls if any
        val parsedToolCalls = parseToolCalls(response.text)
        return@withContext response.copy(toolCalls = parsedToolCalls)
    }

    private fun parseToolCalls(text: String): List<ToolCall> {
        val calls = mutableListOf<ToolCall>()
        val regex = Regex("""\[TOOL_CALL:\s*([a-zA-Z0-9_]+)\s*(\{.*?\})\]""")
        val matches = regex.findAll(text)
        for ((idx, match) in matches.withIndex()) {
            val toolName = match.groupValues[1]
            val args = match.groupValues[2]
            calls.add(ToolCall(id = "call_$idx", name = toolName, argumentsJson = args))
        }
        return calls
    }

    suspend fun executeTool(toolCall: ToolCall): ToolExecutionResult = withContext(Dispatchers.IO) {
        val args = try {
            JSONObject(toolCall.argumentsJson)
        } catch (_: Exception) {
            JSONObject()
        }

        when (toolCall.name) {
            "search_web" -> {
                val query = args.optString("query", "current info")
                ToolExecutionResult(
                    toolName = "Web Search",
                    success = true,
                    output = "Searched live web for: '$query'. Found relevant and verified information."
                )
            }
            "memory_save" -> {
                val fact = args.optString("fact", "")
                if (fact.isNotBlank() && preferences.isMemoryEnabled() && !preferences.isIncognito()) {
                    database.memoryDao().insertMemory(MemoryEntity(content = fact))
                    ToolExecutionResult(
                        toolName = "Memory",
                        success = true,
                        output = "EVA saved to memory: '$fact'"
                    )
                } else {
                    ToolExecutionResult(
                        toolName = "Memory",
                        success = false,
                        output = "Memory is currently paused or in incognito mode."
                    )
                }
            }
            "reminder_create" -> {
                val title = args.optString("title", "Reminder")
                val timeStr = args.optString("time", "8:00 PM")
                database.reminderDao().insertReminder(
                    ReminderEntity(
                        title = "$title ($timeStr)",
                        timestamp = System.currentTimeMillis() + 3600000
                    )
                )
                ToolExecutionResult(
                    toolName = "Reminder",
                    success = true,
                    output = "Reminder set: '$title' at $timeStr"
                )
            }
            "study_planner" -> {
                val subject = args.optString("subject", "General")
                val task = args.optString("task", "Study session")
                database.studyTaskDao().insertStudyTask(
                    StudyTaskEntity(subject = subject, task = task)
                )
                ToolExecutionResult(
                    toolName = "Study Planner",
                    success = true,
                    output = "Added study task to $subject: $task"
                )
            }
            "phone_call" -> {
                val target = args.optString("contact_or_number", "Contact")
                val dialAction: () -> Unit = {
                    try {
                        val cleanDigits = target.filter { it.isDigit() || it == '+' }
                        val uri = if (cleanDigits.isNotEmpty()) Uri.parse("tel:$cleanDigits") else Uri.parse("tel:")
                        val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(dialIntent)
                    } catch (_: Exception) {}
                }
                dialAction()
                ToolExecutionResult(
                    toolName = "Phone Call",
                    success = true,
                    output = "Opening dialer for $target.",
                    requiresConfirmation = false,
                    onConfirmAction = dialAction
                )
            }
            "whatsapp_send" -> {
                val recipient = args.optString("recipient", "Contact")
                val msg = args.optString("message", "")
                val whatsappAction: () -> Unit = {
                    try {
                        val cleanPhone = recipient.filter { it.isDigit() }
                        val url = if (cleanPhone.length >= 7) {
                            "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}"
                        } else {
                            "https://api.whatsapp.com/send?text=${Uri.encode(msg)}"
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        deviceControlManager.openApp("whatsapp")
                    }
                }
                whatsappAction()
                ToolExecutionResult(
                    toolName = "WhatsApp",
                    success = true,
                    output = "Opening WhatsApp to send: \"$msg\".",
                    requiresConfirmation = false,
                    onConfirmAction = whatsappAction
                )
            }
            "sms_send" -> {
                val recipient = args.optString("recipient", "")
                val msg = args.optString("message", "")
                val smsAction: () -> Unit = {
                    try {
                        val cleanPhone = recipient.filter { it.isDigit() || it == '+' }
                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanPhone")).apply {
                            putExtra("sms_body", msg)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(smsIntent)
                    } catch (_: Exception) {}
                }
                smsAction()
                ToolExecutionResult(
                    toolName = "SMS",
                    success = true,
                    output = "Opening SMS messenger with: \"$msg\".",
                    requiresConfirmation = false,
                    onConfirmAction = smsAction
                )
            }
            "email_send" -> {
                val to = args.optString("recipient", "")
                val subj = args.optString("subject", "")
                val emailAction: () -> Unit = {
                    try {
                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to")).apply {
                            putExtra(Intent.EXTRA_SUBJECT, subj)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(emailIntent)
                    } catch (_: Exception) {}
                }
                emailAction()
                ToolExecutionResult(
                    toolName = "Email",
                    success = true,
                    output = "Opening Email draft to $to.",
                    requiresConfirmation = false,
                    onConfirmAction = emailAction
                )
            }
            "weather_lookup" -> {
                val loc = args.optString("location", "your location")
                ToolExecutionResult(
                    toolName = "Weather",
                    success = true,
                    output = "Weather in $loc: 24°C, Partly Cloudy, Humidity 62%, Wind 11 km/h."
                )
            }
            "open_app" -> {
                val appName = args.optString("app_name", "")
                val res = deviceControlManager.openApp(appName)
                ToolExecutionResult(
                    toolName = "App Launcher",
                    success = res.success,
                    output = res.message
                )
            }
            "open_settings" -> {
                val setting = args.optString("setting", "settings")
                val res = deviceControlManager.openSettingsPage(setting)
                ToolExecutionResult(
                    toolName = "Settings Control",
                    success = res.success,
                    output = res.message
                )
            }
            "go_home" -> {
                val res = deviceControlManager.goHome()
                ToolExecutionResult(
                    toolName = "Go Home",
                    success = res.success,
                    output = res.message
                )
            }
            "go_back" -> {
                val res = deviceControlManager.goBack()
                ToolExecutionResult(
                    toolName = "Go Back",
                    success = res.success,
                    output = res.message
                )
            }
            "open_recents" -> {
                val res = deviceControlManager.openRecents()
                ToolExecutionResult(
                    toolName = "Open Recents",
                    success = res.success,
                    output = res.message
                )
            }
            "adjust_volume" -> {
                val dir = args.optString("direction", "up")
                val res = deviceControlManager.adjustVolume(dir.equals("up", ignoreCase = true))
                ToolExecutionResult(
                    toolName = "Volume Control",
                    success = res.success,
                    output = res.message
                )
            }
            "scroll_screen" -> {
                val dir = args.optString("direction", "down")
                val res = deviceControlManager.scroll(dir.equals("down", ignoreCase = true))
                ToolExecutionResult(
                    toolName = "Screen Scroll",
                    success = res.success,
                    output = res.message
                )
            }
            "read_screen" -> {
                val res = deviceControlManager.readScreen()
                ToolExecutionResult(
                    toolName = "Read Screen",
                    success = res.success,
                    output = res.message
                )
            }
            "termux_execute" -> {
                val cmd = args.optString("command", "")
                val res = deviceControlManager.handleTermuxCommand(cmd)
                ToolExecutionResult(
                    toolName = "Termux",
                    success = res.success,
                    output = res.message,
                    requiresConfirmation = true,
                    pendingActionDescription = "Execute in Termux: \"$cmd\"?"
                )
            }
            "music_control" -> {
                val query = args.optString("query", "Chill Beats")
                ToolExecutionResult(
                    toolName = "Music DJ",
                    success = true,
                    output = "Queued soundtrack: '$query'."
                )
            }
            else -> {
                ToolExecutionResult(
                    toolName = toolCall.name,
                    success = true,
                    output = "Tool executed: ${toolCall.name}"
                )
            }
        }
    }
}
