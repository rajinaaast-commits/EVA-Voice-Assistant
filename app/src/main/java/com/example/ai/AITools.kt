package com.example.ai

data class AIToolDefinition(
    val name: String,
    val description: String,
    val isSensitive: Boolean = false,
    val parametersJson: String
)

data class ToolExecutionResult(
    val toolName: String,
    val success: Boolean,
    val output: String,
    val requiresConfirmation: Boolean = false,
    val pendingActionDescription: String? = null,
    val onConfirmAction: (() -> Unit)? = null
)

object AIToolRegistry {
    val availableTools = listOf(
        AIToolDefinition(
            name = "open_app",
            description = "Launch an Android application on device (e.g. Termux, YouTube, Chrome, WhatsApp, Calculator, Settings).",
            isSensitive = false,
            parametersJson = """{"app_name": "app name or package e.g. termux, youtube"}"""
        ),
        AIToolDefinition(
            name = "open_settings",
            description = "Open an Android settings screen (wifi, bluetooth, accessibility, battery, display, notification, sound).",
            isSensitive = false,
            parametersJson = """{"setting": "wifi|bluetooth|accessibility|battery|display|notification|sound"}"""
        ),
        AIToolDefinition(
            name = "go_home",
            description = "Navigate to the Android Home screen.",
            isSensitive = false,
            parametersJson = """{}"""
        ),
        AIToolDefinition(
            name = "go_back",
            description = "Perform the Android Back navigation.",
            isSensitive = false,
            parametersJson = """{}"""
        ),
        AIToolDefinition(
            name = "open_recents",
            description = "Open Android recent apps switcher.",
            isSensitive = false,
            parametersJson = """{}"""
        ),
        AIToolDefinition(
            name = "adjust_volume",
            description = "Raise or lower the device media/ringer volume.",
            isSensitive = false,
            parametersJson = """{"direction": "up|down"}"""
        ),
        AIToolDefinition(
            name = "scroll_screen",
            description = "Scroll the active screen up or down.",
            isSensitive = false,
            parametersJson = """{"direction": "up|down"}"""
        ),
        AIToolDefinition(
            name = "read_screen",
            description = "Read all visible UI text on the screen using Accessibility Service.",
            isSensitive = false,
            parametersJson = """{}"""
        ),
        AIToolDefinition(
            name = "termux_execute",
            description = "Run a terminal command or script in Termux.",
            isSensitive = true,
            parametersJson = """{"command": "bash command to execute"}"""
        ),
        AIToolDefinition(
            name = "search_web",
            description = "Search the web or get real-time facts and answers.",
            isSensitive = false,
            parametersJson = """{"query": "search query string"}"""
        ),
        AIToolDefinition(
            name = "memory_save",
            description = "Store personal user information or preferences in EVA's long-term memory.",
            isSensitive = false,
            parametersJson = """{"fact": "fact to remember"}"""
        ),
        AIToolDefinition(
            name = "reminder_create",
            description = "Set an alert or reminder for a specific time and task.",
            isSensitive = false,
            parametersJson = """{"title": "reminder title", "time": "e.g. 8:00 PM today"}"""
        ),
        AIToolDefinition(
            name = "phone_call",
            description = "Call a phone number or contact via phone dialer.",
            isSensitive = true,
            parametersJson = """{"contact_or_number": "phone number or name"}"""
        ),
        AIToolDefinition(
            name = "whatsapp_send",
            description = "Send a WhatsApp message to a contact or group.",
            isSensitive = true,
            parametersJson = """{"recipient": "contact name or phone", "message": "message body"}"""
        ),
        AIToolDefinition(
            name = "email_send",
            description = "Draft or send an email to a recipient.",
            isSensitive = true,
            parametersJson = """{"recipient": "email address", "subject": "subject", "body": "email content"}"""
        ),
        AIToolDefinition(
            name = "document_create",
            description = "Create or format a document (TXT, PDF, DOC).",
            isSensitive = false,
            parametersJson = """{"title": "title", "content": "text content", "format": "TXT|PDF|DOC"}"""
        ),
        AIToolDefinition(
            name = "coding_agent",
            description = "Generate, explain, debug, or refactor code for an application.",
            isSensitive = false,
            parametersJson = """{"task": "coding instruction", "language": "Kotlin|HTML|Python|etc"}"""
        ),
        AIToolDefinition(
            name = "website_builder",
            description = "Generate or edit a web project with HTML, CSS, and JavaScript.",
            isSensitive = false,
            parametersJson = """{"title": "site name", "html": "html code", "css": "css code", "js": "js code"}"""
        ),
        AIToolDefinition(
            name = "study_planner",
            description = "Add study task, create flashcard, or schedule focus session.",
            isSensitive = false,
            parametersJson = """{"subject": "subject name", "task": "task description"}"""
        ),
        AIToolDefinition(
            name = "weather_lookup",
            description = "Get current weather condition and forecast for a city.",
            isSensitive = false,
            parametersJson = """{"location": "city name"}"""
        ),
        AIToolDefinition(
            name = "music_control",
            description = "Play music, focus ambience, or study soundscapes.",
            isSensitive = false,
            parametersJson = """{"query": "song, artist, or ambience type"}"""
        )
    )

    fun getToolDescriptionSystemPrompt(): String {
        val sb = StringBuilder()
        sb.append("You have access to the following EVA tools. When the user asks you to perform an action, trigger it using [TOOL_CALL: tool_name {\"arg\": \"val\"}]:\n")
        for (tool in availableTools) {
            sb.append("- ${tool.name}: ${tool.description} Parameters: ${tool.parametersJson}\n")
        }
        sb.append("If a tool call is needed, format it clearly on its own line: [TOOL_CALL: tool_name {args}]. You can provide a friendly conversational response alongside it.\n")
        return sb.toString()
    }
}
