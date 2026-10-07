package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import com.example.service.EVAAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class DeviceActionResult(
    val success: Boolean,
    val message: String,
    val actionType: String,
    val requiresSetup: Boolean = false,
    val setupIntent: Intent? = null
)

data class ActionHistoryItem(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val actionType: String,
    val success: Boolean,
    val details: String
)

class DeviceControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _actionHistory = MutableStateFlow<List<ActionHistoryItem>>(emptyList())
    val actionHistory: StateFlow<List<ActionHistoryItem>> = _actionHistory.asStateFlow()

    private val _lastAction = MutableStateFlow<DeviceActionResult?>(null)
    val lastAction: StateFlow<DeviceActionResult?> = _lastAction.asStateFlow()

    private fun logAction(command: String, actionType: String, success: Boolean, details: String) {
        val item = ActionHistoryItem(
            command = command,
            actionType = actionType,
            success = success,
            details = details
        )
        _actionHistory.value = listOf(item) + _actionHistory.value.take(49)
    }

    fun clearHistory() {
        _actionHistory.value = emptyList()
    }

    // 1. App Launcher
    fun openApp(targetName: String, originalCommand: String = "Open $targetName"): DeviceActionResult {
        val query = targetName.trim().lowercase(Locale.ROOT)

        // Known direct package shortcuts
        val commonPackages = mapOf(
            "termux" to "com.termux",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "whatsapp" to "com.whatsapp",
            "settings" to "com.android.settings",
            "calculator" to "com.google.android.calculator",
            "camera" to "com.google.android.GoogleCamera",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm",
            "spotify" to "com.spotify.music",
            "telegram" to "org.telegram.messenger",
            "clock" to "com.google.android.deskclock"
        )

        var resolvedPackage = commonPackages[query]

        // Dynamic package discovery
        if (resolvedPackage == null) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (info in resolveInfos) {
                val appLabel = info.loadLabel(pm).toString().lowercase(Locale.ROOT)
                val pkg = info.activityInfo.packageName.lowercase(Locale.ROOT)
                if (appLabel.contains(query) || pkg.contains(query)) {
                    resolvedPackage = info.activityInfo.packageName
                    break
                }
            }
        }

        if (resolvedPackage.isNullOrEmpty()) {
            val friendlyName = targetName.replaceFirstChar { it.titlecase(Locale.ROOT) }
            val result = DeviceActionResult(
                success = false,
                message = "$friendlyName isn't installed on this device.",
                actionType = "OPEN_APP"
            )
            logAction(originalCommand, "OPEN_APP", false, result.message)
            _lastAction.value = result
            return result
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(resolvedPackage)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                val friendlyName = targetName.replaceFirstChar { it.titlecase(Locale.ROOT) }
                val result = DeviceActionResult(
                    success = true,
                    message = "Opening $friendlyName.",
                    actionType = "OPEN_APP"
                )
                logAction(originalCommand, "OPEN_APP", true, "Launched $resolvedPackage")
                _lastAction.value = result
                result
            } catch (e: Exception) {
                val result = DeviceActionResult(
                    success = false,
                    message = "Could not launch $targetName: ${e.localizedMessage}",
                    actionType = "OPEN_APP"
                )
                logAction(originalCommand, "OPEN_APP", false, e.localizedMessage ?: "Launch error")
                _lastAction.value = result
                result
            }
        } else {
            val friendlyName = targetName.replaceFirstChar { it.titlecase(Locale.ROOT) }
            val result = DeviceActionResult(
                success = false,
                message = "$friendlyName isn't installed on this device.",
                actionType = "OPEN_APP"
            )
            logAction(originalCommand, "OPEN_APP", false, "$resolvedPackage has no launch intent")
            _lastAction.value = result
            result
        }
    }

    // 2. Android Settings Controls
    fun openSettingsPage(target: String, originalCommand: String = "Open $target"): DeviceActionResult {
        val lower = target.lowercase(Locale.ROOT)
        val intent = when {
            lower.contains("wi-fi") || lower.contains("wifi") -> Intent(Settings.ACTION_WIFI_SETTINGS)
            lower.contains("bluetooth") -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            lower.contains("notification") -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            lower.contains("accessibility") -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            lower.contains("battery") -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            lower.contains("display") || lower.contains("brightness") -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            lower.contains("sound") || lower.contains("volume") -> Intent(Settings.ACTION_SOUND_SETTINGS)
            lower.contains("permission") || lower.contains("app") -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            lower.contains("dnd") || lower.contains("do not disturb") -> Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            val result = DeviceActionResult(
                success = true,
                message = "Opening ${target.replaceFirstChar { it.titlecase(Locale.ROOT) }}.",
                actionType = "OPEN_SETTINGS"
            )
            logAction(originalCommand, "OPEN_SETTINGS", true, intent.action ?: target)
            _lastAction.value = result
            result
        } catch (e: Exception) {
            val result = DeviceActionResult(
                success = false,
                message = "Unable to open settings page: ${e.localizedMessage}",
                actionType = "OPEN_SETTINGS"
            )
            logAction(originalCommand, "OPEN_SETTINGS", false, e.localizedMessage ?: "Settings launch error")
            _lastAction.value = result
            result
        }
    }

    // 3. Device Controls (Volume, Media)
    fun adjustVolume(increase: Boolean, originalCommand: String = "Volume adjust"): DeviceActionResult {
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        val msg = if (increase) "Volume increased." else "Volume decreased."
        val result = DeviceActionResult(
            success = true,
            message = msg,
            actionType = if (increase) "VOLUME_UP" else "VOLUME_DOWN"
        )
        logAction(originalCommand, result.actionType, true, msg)
        _lastAction.value = result
        return result
    }

    fun mediaControl(action: String, originalCommand: String = "Media control"): DeviceActionResult {
        val keyCode = when (action.uppercase(Locale.ROOT)) {
            "PLAY", "PLAY_PAUSE" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "PAUSE" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "NEXT" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "PREVIOUS", "PREV" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        val eventTime = SystemClock.uptimeMillis()
        val down = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)

        audioManager.dispatchMediaKeyEvent(down)
        audioManager.dispatchMediaKeyEvent(up)

        val msg = "Media $action dispatched."
        val result = DeviceActionResult(
            success = true,
            message = msg,
            actionType = "MEDIA_$action"
        )
        logAction(originalCommand, result.actionType, true, msg)
        _lastAction.value = result
        return result
    }

    // 4. Accessibility Service Global & Gesture Actions
    fun goBack(originalCommand: String = "Go back"): DeviceActionResult {
        val service = EVAAccessibilityService.instance
        if (service == null) {
            val result = DeviceActionResult(
                success = false,
                message = "Accessibility service is required. Enable EVA Accessibility Service in Settings → Device Control.",
                actionType = "GO_BACK",
                requiresSetup = true,
                setupIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
            logAction(originalCommand, "GO_BACK", false, "Accessibility service not running")
            _lastAction.value = result
            return result
        }

        val success = service.goBack()
        val result = DeviceActionResult(
            success = success,
            message = if (success) "Navigated back." else "Could not perform back action.",
            actionType = "GO_BACK"
        )
        logAction(originalCommand, "GO_BACK", success, result.message)
        _lastAction.value = result
        return result
    }

    fun goHome(originalCommand: String = "Go home"): DeviceActionResult {
        val service = EVAAccessibilityService.instance
        if (service == null) {
            // Safe fallback to Intent Home
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(homeIntent)
            val result = DeviceActionResult(
                success = true,
                message = "Navigating to home screen.",
                actionType = "GO_HOME"
            )
            logAction(originalCommand, "GO_HOME", true, "Used HOME intent")
            _lastAction.value = result
            return result
        }

        val success = service.goHome()
        val result = DeviceActionResult(
            success = success,
            message = if (success) "Navigated home." else "Could not navigate home.",
            actionType = "GO_HOME"
        )
        logAction(originalCommand, "GO_HOME", success, result.message)
        _lastAction.value = result
        return result
    }

    fun openRecents(originalCommand: String = "Open recents"): DeviceActionResult {
        val service = EVAAccessibilityService.instance
        if (service == null) {
            val result = DeviceActionResult(
                success = false,
                message = "Accessibility service is required to open recent apps. Please enable it in Settings → Device Control.",
                actionType = "OPEN_RECENTS",
                requiresSetup = true,
                setupIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
            logAction(originalCommand, "OPEN_RECENTS", false, "Accessibility service missing")
            _lastAction.value = result
            return result
        }

        val success = service.openRecents()
        val result = DeviceActionResult(
            success = success,
            message = if (success) "Opened recent apps." else "Could not open recents.",
            actionType = "OPEN_RECENTS"
        )
        logAction(originalCommand, "OPEN_RECENTS", success, result.message)
        _lastAction.value = result
        return result
    }

    fun scroll(down: Boolean, originalCommand: String = "Scroll"): DeviceActionResult {
        val service = EVAAccessibilityService.instance
        if (service == null) {
            val result = DeviceActionResult(
                success = false,
                message = "Accessibility service is required to scroll. Please enable it in Settings → Device Control.",
                actionType = if (down) "SCROLL_DOWN" else "SCROLL_UP",
                requiresSetup = true,
                setupIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
            logAction(originalCommand, "SCROLL", false, "Accessibility service missing")
            _lastAction.value = result
            return result
        }

        val success = if (down) service.scrollDown() else service.scrollUp()
        val result = DeviceActionResult(
            success = success,
            message = if (success) (if (down) "Scrolled down." else "Scrolled up.") else "No scrollable area on the active screen.",
            actionType = if (down) "SCROLL_DOWN" else "SCROLL_UP"
        )
        logAction(originalCommand, "SCROLL", success, result.message)
        _lastAction.value = result
        return result
    }

    fun readScreen(originalCommand: String = "Read screen"): DeviceActionResult {
        val service = EVAAccessibilityService.instance
        if (service == null) {
            val result = DeviceActionResult(
                success = false,
                message = "Accessibility service is required to read screen content.",
                actionType = "READ_SCREEN",
                requiresSetup = true,
                setupIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
            logAction(originalCommand, "READ_SCREEN", false, "Accessibility service missing")
            _lastAction.value = result
            return result
        }

        val text = service.readVisibleScreenText()
        val result = DeviceActionResult(
            success = true,
            message = "Screen content:\n$text",
            actionType = "READ_SCREEN"
        )
        logAction(originalCommand, "READ_SCREEN", true, "Read ${text.length} chars")
        _lastAction.value = result
        return result
    }

    // 5. Termux Integration
    fun handleTermuxCommand(commandText: String?): DeviceActionResult {
        val pm = context.packageManager
        val isInstalled = try {
            pm.getPackageInfo("com.termux", 0)
            true
        } catch (_: Exception) {
            false
        }

        if (!isInstalled) {
            val result = DeviceActionResult(
                success = false,
                message = "Termux isn't installed on this device.",
                actionType = "TERMUX_COMMAND"
            )
            logAction("Termux command", "TERMUX_COMMAND", false, result.message)
            _lastAction.value = result
            return result
        }

        if (commandText.isNullOrBlank()) {
            return openApp("termux", "Open Termux")
        }

        // Check if Termux:Tasker or RUN_COMMAND is configured
        val intent = Intent("com.termux.RUN_COMMAND").apply {
            setPackage("com.termux")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", commandText))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startService(intent)
            val result = DeviceActionResult(
                success = true,
                message = "Sent command to Termux: $commandText",
                actionType = "TERMUX_EXECUTE"
            )
            logAction("Run Termux: $commandText", "TERMUX_EXECUTE", true, "Dispatched RUN_COMMAND")
            _lastAction.value = result
            result
        } catch (_: Exception) {
            // Explain cleanly as specified in prompt
            val result = DeviceActionResult(
                success = false,
                message = "Termux command integration isn't configured yet. Open EVA Settings → Device Control → Termux Integration to set it up.",
                actionType = "TERMUX_EXECUTE",
                requiresSetup = true
            )
            logAction("Run Termux: $commandText", "TERMUX_EXECUTE", false, "Termux RUN_COMMAND not configured")
            _lastAction.value = result
            result
        }
    }

    // 6. Natural Language / Multilingual Intent Parser (English, Bangla, Banglish, Hinglish)
    fun tryParseAndExecuteCommand(rawInput: String): DeviceActionResult? {
        val input = rawInput.trim().lowercase(Locale.ROOT)
        if (input.isEmpty()) return null

        // App Launching Patterns
        // English: "open termux", "launch youtube", "open whatsapp"
        // Bangla/Banglish: "youtube open koro", "termux chalu koro", "whatsapp khule dao", "settings kholo"
        val openAppRegex = Regex("""^(?:open|launch|start|চালু করো|খুলে দাও)?\s*([a-zA-Z0-9_\-\s]+?)\s*(?:open koro|kholo|chalu koro|khule dao|app)?$""")

        // Wi-Fi / Bluetooth / Settings
        if (input.contains("wi-fi") || input.contains("wifi")) {
            return openSettingsPage("Wi-Fi Settings", rawInput)
        }
        if (input.contains("bluetooth")) {
            return openSettingsPage("Bluetooth Settings", rawInput)
        }
        if (input.contains("accessibility") && (input.contains("setting") || input.contains("settings"))) {
            return openSettingsPage("Accessibility Settings", rawInput)
        }
        if (input.contains("battery") && (input.contains("setting") || input.contains("settings"))) {
            return openSettingsPage("Battery Settings", rawInput)
        }
        if (input.contains("notification") && (input.contains("setting") || input.contains("settings"))) {
            return openSettingsPage("Notification Settings", rawInput)
        }
        if (input.contains("display") || input.contains("brightness")) {
            return openSettingsPage("Display Settings", rawInput)
        }

        // Navigation actions
        // Go home / "home e jao", "home jao", "go to home"
        if (input.contains("go home") || input.contains("home e jao") || input.contains("home jao") || input == "home") {
            return goHome(rawInput)
        }
        // Go back / "back koro", "pichone jao", "go back"
        if (input.contains("go back") || input.contains("back koro") || input == "back") {
            return goBack(rawInput)
        }
        // Open recents / "recent apps", "recents open koro"
        if (input.contains("recent") || input.contains("recents")) {
            return openRecents(rawInput)
        }
        // Scroll down / "scroll down", "niche scroll koro"
        if (input.contains("scroll down") || input.contains("niche scroll") || input.contains("scroll niche")) {
            return scroll(down = true, originalCommand = rawInput)
        }
        // Scroll up / "scroll up", "upore scroll koro"
        if (input.contains("scroll up") || input.contains("upore scroll")) {
            return scroll(down = false, originalCommand = rawInput)
        }
        // Read screen
        if (input.contains("read screen") || input.contains("read this screen") || input.contains("screen porho")) {
            return readScreen(rawInput)
        }

        // Volume
        // "increase volume", "volume up", "volume barao", "volume ta barao", "volume badhao"
        if (input.contains("volume up") || input.contains("increase volume") || input.contains("volume barao") || input.contains("volume ta barao") || input.contains("volume badhao") || input.contains("sound barao")) {
            return adjustVolume(increase = true, originalCommand = rawInput)
        }
        // "decrease volume", "volume down", "volume komao", "volume ta komao", "volume ghatao"
        if (input.contains("volume down") || input.contains("decrease volume") || input.contains("volume komao") || input.contains("volume ta komao") || input.contains("volume ghatao")) {
            return adjustVolume(increase = false, originalCommand = rawInput)
        }

        // Media controls
        if (input.contains("play music") || input.contains("play media") || input.contains("resume music") || input == "play") {
            return mediaControl("PLAY", rawInput)
        }
        if (input.contains("pause music") || input.contains("pause media") || input == "pause") {
            return mediaControl("PAUSE", rawInput)
        }
        if (input.contains("next song") || input.contains("next track") || input == "next") {
            return mediaControl("NEXT", rawInput)
        }

        // Termux commands
        if (input.contains("run") && input.contains("termux")) {
            val cmd = rawInput.substringAfter("termux").trim().removePrefix("command").removePrefix("script").trim()
            return handleTermuxCommand(if (cmd.isNotBlank()) cmd else "python3")
        }

        // App Launching (e.g. "open YouTube", "YouTube open koro", "launch Chrome")
        val appMatches = listOf("termux", "youtube", "chrome", "whatsapp", "settings", "calculator", "camera", "spotify", "telegram", "maps")
        for (candidate in appMatches) {
            if (input.contains(candidate)) {
                return openApp(candidate, rawInput)
            }
        }

        if (input.startsWith("open ") || input.startsWith("launch ")) {
            val targetApp = input.removePrefix("open ").removePrefix("launch ").trim()
            if (targetApp.isNotEmpty()) {
                return openApp(targetApp, rawInput)
            }
        }

        return null
    }
}
