package com.example.ui.settings

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EvaApplication
import com.example.ai.KeyValidationState
import com.example.data.preferences.AIProviderMode
import com.example.data.preferences.SubscriptionPlan
import com.example.data.preferences.VoiceEngineMode
import com.example.ui.theme.*
import com.example.voice.EvaTtsHelper
import kotlinx.coroutines.launch

enum class SettingsSubpage {
    MAIN,
    DEVICE_CONTROL,
    API_KEYS,
    EVA_PERSONA,
    MEMORY,
    VOICE_SETTINGS,
    VOICE_ENGINE,
    VOICE_PROFILE,
    TELEGRAM_BOT,
    GMAIL_SMTP,
    SECRET_SETTINGS,
    EDGE_GLOW,
    SUBSCRIPTION,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()

    var activeSubpage by remember { mutableStateOf(SettingsSubpage.MAIN) }

    androidx.activity.compose.BackHandler(enabled = activeSubpage != SettingsSubpage.MAIN) {
        activeSubpage = SettingsSubpage.MAIN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (activeSubpage) {
                            SettingsSubpage.MAIN -> "Settings"
                            SettingsSubpage.DEVICE_CONTROL -> "Device Control"
                            SettingsSubpage.API_KEYS -> "AI Providers & API Keys"
                            SettingsSubpage.EVA_PERSONA -> "EVA Persona & Tone"
                            SettingsSubpage.MEMORY -> "Memory & Privacy"
                            SettingsSubpage.VOICE_SETTINGS -> "Voice & Audio"
                            SettingsSubpage.VOICE_ENGINE -> "Voice Engine Architecture"
                            SettingsSubpage.VOICE_PROFILE -> "Voice Recognition Profile"
                            SettingsSubpage.TELEGRAM_BOT -> "Telegram Bot Integration"
                            SettingsSubpage.GMAIL_SMTP -> "Gmail SMTP Connector"
                            SettingsSubpage.SECRET_SETTINGS -> "Secret Settings"
                            SettingsSubpage.EDGE_GLOW -> "EVA Edge Glow Overlay"
                            SettingsSubpage.SUBSCRIPTION -> "Subscription & License"
                            SettingsSubpage.ABOUT -> "About EVA AI"
                        },
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeSubpage != SettingsSubpage.MAIN) {
                            activeSubpage = SettingsSubpage.MAIN
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDarkBackground)
            )
        },
        containerColor = CosmicDarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (activeSubpage) {
                SettingsSubpage.MAIN -> MainSettingsMenu(onSelectSubpage = { activeSubpage = it })
                SettingsSubpage.DEVICE_CONTROL -> DeviceControlSettingsView()
                SettingsSubpage.API_KEYS -> ApiKeysSettingsView()
                SettingsSubpage.EVA_PERSONA -> PersonaSettingsView()
                SettingsSubpage.MEMORY -> MemorySettingsView()
                SettingsSubpage.VOICE_SETTINGS -> VoiceSettingsView()
                SettingsSubpage.VOICE_ENGINE -> VoiceEngineSettingsView()
                SettingsSubpage.VOICE_PROFILE -> VoiceProfileSettingsView()
                SettingsSubpage.TELEGRAM_BOT -> TelegramBotSettingsView()
                SettingsSubpage.GMAIL_SMTP -> GmailSmtpSettingsView()
                SettingsSubpage.SECRET_SETTINGS -> SecretSettingsView()
                SettingsSubpage.EDGE_GLOW -> EdgeGlowSettingsView()
                SettingsSubpage.SUBSCRIPTION -> SubscriptionSettingsView()
                SettingsSubpage.ABOUT -> AboutView()
            }
        }
    }
}

@Composable
fun MainSettingsMenu(onSelectSubpage: (SettingsSubpage) -> Unit) {
    val items = listOf(
        Triple("⚡ Device Control", "App Launcher, Android Intents, Accessibility & Termux", SettingsSubpage.DEVICE_CONTROL),
        Triple("AI Providers & Keys", "Configure Gemini & OmniRoute keys and models", SettingsSubpage.API_KEYS),
        Triple("✨ EVA Edge Glow", "Corner neon animations, background overlay & voice reaction", SettingsSubpage.EDGE_GLOW),
        Triple("EVA Persona", "Personalize name, tone, custom instructions", SettingsSubpage.EVA_PERSONA),
        Triple("Memory & Privacy", "Persistent autonomous memories & incognito toggle", SettingsSubpage.MEMORY),
        Triple("Voice & Audio", "Select from 10 voices, speed, pitch, interrupt", SettingsSubpage.VOICE_SETTINGS),
        Triple("Voice Engine", "Swift, Soul, or Advanced Agentic Ear-Brain-Voice", SettingsSubpage.VOICE_ENGINE),
        Triple("Voice Profile", "Train your voice match & anti-spoof sensitivity", SettingsSubpage.VOICE_PROFILE),
        Triple("Telegram Bot", "Chat & Voice message integration via Bot API", SettingsSubpage.TELEGRAM_BOT),
        Triple("Gmail SMTP", "Send and draft emails via secure SMTP", SettingsSubpage.GMAIL_SMTP),
        Triple("Secret Settings", "Personality modes: Friendly, Romantic, Intense (18+)", SettingsSubpage.SECRET_SETTINGS),
        Triple("Subscription & License", "Free, Premium, Pro, Max comparison & activation", SettingsSubpage.SUBSCRIPTION),
        Triple("About EVA AI", "Version 1.0.0 by Aura RIFAT", SettingsSubpage.ABOUT)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { (title, subtitle, page) ->
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSubpage(page) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                }
            }
        }
    }
}

@Composable
fun ApiKeysSettingsView() {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }

    var geminiKey by remember { mutableStateOf(app.aiProviderManager.getGeminiKey()) }
    var omniKey by remember { mutableStateOf(app.aiProviderManager.getOmniRouteKey()) }
    var omniUrl by remember { mutableStateOf(app.preferences.getOmniRouteBaseUrl()) }
    var geminiModel by remember { mutableStateOf(app.preferences.getGeminiModel()) }
    var omniModel by remember { mutableStateOf(app.preferences.getOmniRouteModel()) }
    var mode by remember { mutableStateOf(app.preferences.getProviderMode()) }

    var showGeminiKey by remember { mutableStateOf(false) }
    var showOmniKey by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }
    var isValidatingGemini by remember { mutableStateOf(false) }
    var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }

    var geminiValidationState by remember {
        mutableStateOf<KeyValidationState>(
            if (geminiKey.isBlank()) {
                KeyValidationState.Missing
            } else {
                KeyValidationState.Idle
            }
        )
    }

    // Function to run server-backed validation and retry
    fun triggerGeminiValidation(isRetry: Boolean = false) {
        val cleanKey = geminiKey.trim().trim('"', '\'')
        geminiKey = cleanKey

        if (cleanKey.isBlank()) {
            geminiValidationState = KeyValidationState.Missing
            return
        }

        if (isRetry) {
            retryCount++
        }

        isValidatingGemini = true
        coroutineScope.launch {
            val result = app.aiProviderManager.geminiKeyValidator.validateKeyWithServer(
                rawKey = cleanKey,
                onProgress = { progressState ->
                    geminiValidationState = progressState
                }
            )
            geminiValidationState = result
            isValidatingGemini = false

            if (result is KeyValidationState.Success) {
                app.aiProviderManager.setGeminiKey(cleanKey)
            }
        }
    }

    // Auto-check format or initial state if key is present but unvalidated
    LaunchedEffect(Unit) {
        if (geminiKey.isNotBlank() && geminiValidationState is KeyValidationState.Idle) {
            val syntax = app.aiProviderManager.geminiKeyValidator.validateSyntax(geminiKey)
            if (syntax is com.example.ai.SyntaxValidation.Malformed) {
                geminiValidationState = KeyValidationState.Malformed(syntax.reason, syntax.tip)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // AI Provider Mode
        Text(
            text = "AI Provider Mode",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AIProviderMode.values().forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = {
                        mode = m
                        app.preferences.setProviderMode(m)
                    },
                    label = { Text(m.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- GEMINI CONFIGURATION CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(
                width = 1.dp,
                color = when (geminiValidationState) {
                    is KeyValidationState.Success -> StatusSuccess.copy(alpha = 0.5f)
                    is KeyValidationState.Failed, is KeyValidationState.Missing -> StatusError.copy(alpha = 0.4f)
                    is KeyValidationState.Malformed -> StatusWarning.copy(alpha = 0.5f)
                    is KeyValidationState.Validating -> NeonCyan.copy(alpha = 0.6f)
                    KeyValidationState.Idle -> DarkOutline
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Gemini API",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Status Badge Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (geminiValidationState) {
                            is KeyValidationState.Success -> StatusSuccess.copy(alpha = 0.18f)
                            is KeyValidationState.Failed, is KeyValidationState.Missing -> StatusError.copy(alpha = 0.18f)
                            is KeyValidationState.Malformed -> StatusWarning.copy(alpha = 0.18f)
                            is KeyValidationState.Validating -> NeonCyan.copy(alpha = 0.18f)
                            KeyValidationState.Idle -> if (geminiKey.isNotBlank()) NeonCyan.copy(alpha = 0.12f) else DarkOutline.copy(alpha = 0.3f)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val (badgeText, badgeColor) = when (geminiValidationState) {
                                is KeyValidationState.Success -> "Verified" to StatusSuccess
                                is KeyValidationState.Failed -> "Auth Failed" to StatusError
                                is KeyValidationState.Missing -> "Missing Key" to StatusError
                                is KeyValidationState.Malformed -> "Invalid Format" to StatusWarning
                                is KeyValidationState.Validating -> "Authenticating..." to NeonCyan
                                KeyValidationState.Idle -> if (geminiKey.isNotBlank()) "Configured" to TextSecondary else "Not Configured" to TextMuted
                            }
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(badgeColor, RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gemini API Key Input
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = {
                        geminiKey = it
                        if (it.isBlank()) {
                            geminiValidationState = KeyValidationState.Missing
                        } else {
                            val syntax = app.aiProviderManager.geminiKeyValidator.validateSyntax(it)
                            geminiValidationState = if (syntax is com.example.ai.SyntaxValidation.Malformed) {
                                KeyValidationState.Malformed(syntax.reason, syntax.tip)
                            } else {
                                KeyValidationState.Idle
                            }
                        }
                    },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = geminiValidationState is KeyValidationState.Failed ||
                            geminiValidationState is KeyValidationState.Malformed ||
                            (geminiValidationState is KeyValidationState.Missing && geminiKey.isBlank()),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showGeminiKey) "Hide Key" else "Show Key",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (geminiKey.isNotBlank()) {
                                IconButton(onClick = {
                                    geminiKey = ""
                                    geminiValidationState = KeyValidationState.Missing
                                    app.aiProviderManager.setGeminiKey("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Key",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_key_input_settings")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // --- DETAILED USER-FRIENDLY VALIDATION STATUS / ERROR MESSAGES & RETRY ---
                when (val state = geminiValidationState) {
                    is KeyValidationState.Missing -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusError.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_missing_error_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.KeyOff,
                                        contentDescription = null,
                                        tint = StatusError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Gemini API Key Required",
                                            color = StatusError,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "EVA needs a valid Gemini API key to understand voice commands and provide intelligent responses. Generate a free key in Google AI Studio to activate.",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboardText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                            if (clipboardText.isNotBlank()) {
                                                geminiKey = clipboardText.trim().trim('"', '\'')
                                                triggerGeminiValidation(isRetry = false)
                                            } else {
                                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paste Key", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "Visit https://aistudio.google.com/app/apikey", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Get Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    is KeyValidationState.Malformed -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusWarning.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_malformed_error_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = StatusWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Invalid Key Format: ${state.reason}",
                                            color = StatusWarning,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = state.tip,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        geminiKey = app.aiProviderManager.geminiKeyValidator.sanitizeKey(geminiKey)
                                        triggerGeminiValidation(isRetry = false)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusWarning, contentColor = CosmicDarkBackground),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Clean & Test Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is KeyValidationState.Validating -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeonCyan.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_validating_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = NeonCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Authenticating with Google Gemini...",
                                        color = NeonCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Connecting to Google servers (attempt ${state.attempt} of ${state.maxAttempts})...",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    is KeyValidationState.Failed -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusError.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_failed_error_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = StatusError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = state.reason,
                                            color = StatusError,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = state.userFriendlyMessage,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tip: ${state.suggestedAction}",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // --- RETRY MECHANISM BUTTON ---
                                Button(
                                    onClick = { triggerGeminiValidation(isRetry = true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusError,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("gemini_retry_authentication_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (retryCount > 0) "Retry Re-Authentication (Attempt #${retryCount + 1})" else "Retry Authentication",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    is KeyValidationState.Success -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusSuccess.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_success_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Gemini API Key Verified",
                                            color = StatusSuccess,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${state.message} (${state.latencyMs}ms)",
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { triggerGeminiValidation(isRetry = true) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Re-check",
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    KeyValidationState.Idle -> {
                        // Action row to test or obtain key
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { triggerGeminiValidation(isRetry = false) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gemini_test_button")
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Validate Key")
                            }

                            TextButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Visit aistudio.google.com/app/apikey", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Get API Key", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gemini Model Selection & Quick Select Chips
                Text(
                    text = "Gemini Model Selection",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val recommendedModels = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
                    recommendedModels.forEach { m ->
                        FilterChip(
                            selected = geminiModel == m,
                            onClick = {
                                geminiModel = m
                                app.preferences.setGeminiModel(m)
                            },
                            label = { Text(m, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = geminiModel,
                    onValueChange = {
                        geminiModel = it
                        app.preferences.setGeminiModel(it)
                    },
                    label = { Text("Custom Model ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- OMNIROUTE CONFIGURATION CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "OmniRoute",
                        tint = ElectricViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OmniRoute (Self-Hosted / Proxy)",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = omniKey,
                    onValueChange = { omniKey = it },
                    label = { Text("OmniRoute API Key") },
                    singleLine = true,
                    visualTransformation = if (showOmniKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showOmniKey = !showOmniKey }) {
                            Icon(
                                imageVector = if (showOmniKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showOmniKey) "Hide" else "Show",
                                tint = TextSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = omniUrl,
                    onValueChange = {
                        omniUrl = it
                        app.preferences.setOmniRouteBaseUrl(it)
                    },
                    label = { Text("OmniRoute Base URL") },
                    placeholder = { Text("https://your-omniroute-instance.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = omniModel,
                    onValueChange = {
                        omniModel = it
                        app.preferences.setOmniRouteModel(it)
                    },
                    label = { Text("OmniRoute Model ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Feedback Banner (if any)
        saveFeedbackMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StatusSuccess.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(msg, color = StatusSuccess, fontSize = 13.sp)
                }
            }
        }

        // Save API Settings Button
        Button(
            onClick = {
                val cleanGemini = app.aiProviderManager.geminiKeyValidator.sanitizeKey(geminiKey)
                val cleanOmni = omniKey.trim().trim('"', '\'')
                geminiKey = cleanGemini
                omniKey = cleanOmni

                app.aiProviderManager.setGeminiKey(cleanGemini)
                app.aiProviderManager.setOmniRouteKey(cleanOmni)
                app.preferences.setGeminiModel(geminiModel.trim())
                app.preferences.setOmniRouteModel(omniModel.trim())
                app.preferences.setOmniRouteBaseUrl(omniUrl.trim())

                if (cleanGemini.isNotBlank()) {
                    triggerGeminiValidation(isRetry = false)
                    saveFeedbackMessage = "Settings saved! Verifying Gemini authentication..."
                } else {
                    geminiValidationState = KeyValidationState.Missing
                    saveFeedbackMessage = "API Settings saved. Note: Gemini API key is missing."
                    Toast.makeText(context, "Gemini key is missing. Add key to enable Gemini features.", Toast.LENGTH_LONG).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_api_settings_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save API Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PersonaSettingsView() {
    val app = EvaApplication.instance
    var assistantName by remember { mutableStateOf(app.preferences.getAssistantName()) }
    var selectedPersona by remember { mutableStateOf(app.preferences.getPersona()) }
    var customPrompt by remember { mutableStateOf(app.preferences.getCustomPersonaPrompt()) }

    val personas = listOf("EVA", "Jarvis", "Friday", "Maya", "Venom", "Friendly", "Professional", "Study Mentor", "Coding Expert", "Creative", "Custom")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = assistantName,
            onValueChange = {
                assistantName = it
                app.preferences.setAssistantName(it)
            },
            label = { Text("Assistant Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Personality Presets", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        personas.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                row.forEach { p ->
                    FilterChip(
                        selected = selectedPersona == p,
                        onClick = {
                            selectedPersona = p
                            app.preferences.setPersona(p)
                        },
                        label = { Text(p, fontSize = 12.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = customPrompt,
            onValueChange = {
                customPrompt = it
                app.preferences.setCustomPersonaPrompt(it)
            },
            label = { Text("Custom Persona Instructions") },
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MemorySettingsView() {
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()
    var memoryEnabled by remember { mutableStateOf(app.preferences.isMemoryEnabled()) }
    var incognito by remember { mutableStateOf(app.preferences.isIncognito()) }
    var autoMemory by remember { mutableStateOf(app.preferences.isAutonomousMemory()) }

    val memories by app.database.memoryDao().getAllMemories().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Enable EVA Memory", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(checked = memoryEnabled, onCheckedChange = {
                memoryEnabled = it
                app.preferences.setMemoryEnabled(it)
            })
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Incognito Mode (No memories logged)", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(checked = incognito, onCheckedChange = {
                incognito = it
                app.preferences.setIncognito(it)
            })
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Let EVA remember autonomously", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(checked = autoMemory, onCheckedChange = {
                autoMemory = it
                app.preferences.setAutonomousMemory(it)
            })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stored Memories (${memories.size})", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                coroutineScope.launch { app.database.memoryDao().clearAllMemories() }
            }) {
                Text("Clear All", color = StatusError)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(memories) { mem ->
                ElevatedCard(shape = RoundedCornerShape(10.dp), colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                        Text(text = mem.content, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            coroutineScope.launch { app.database.memoryDao().deleteMemory(mem) }
                        }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceSettingsView() {
    val app = EvaApplication.instance
    var selectedVoice by remember { mutableStateOf(app.preferences.getSelectedVoice()) }
    var speed by remember { mutableStateOf(app.preferences.getVoiceSpeed()) }
    var pitch by remember { mutableStateOf(app.preferences.getVoicePitch()) }
    var autoSpeak by remember { mutableStateOf(app.preferences.isAutoSpeak()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Auto-Speak Voice Responses", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(checked = autoSpeak, onCheckedChange = {
                autoSpeak = it
                app.preferences.setAutoSpeak(it)
            })
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Select Voice (${EvaTtsHelper.AVAILABLE_VOICES.size} options)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        EvaTtsHelper.AVAILABLE_VOICES.forEach { voice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedVoice = voice
                        app.preferences.setSelectedVoice(voice)
                        app.ttsHelper.speak("Hello! This is $voice speaking.")
                    }
                    .padding(vertical = 8.dp)
            ) {
                RadioButton(selected = selectedVoice == voice, onClick = {
                    selectedVoice = voice
                    app.preferences.setSelectedVoice(voice)
                })
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = voice, color = TextPrimary, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Voice Speed: ${String.format("%.1fx", speed)}", color = TextSecondary, fontSize = 13.sp)
        Slider(value = speed, onValueChange = {
            speed = it
            app.preferences.setVoiceSpeed(it)
        }, valueRange = 0.5f..2.0f)

        Text("Voice Pitch: ${String.format("%.1fx", pitch)}", color = TextSecondary, fontSize = 13.sp)
        Slider(value = pitch, onValueChange = {
            pitch = it
            app.preferences.setVoicePitch(it)
        }, valueRange = 0.5f..1.5f)
    }
}

@Composable
fun VoiceEngineSettingsView() {
    val app = EvaApplication.instance
    var engineMode by remember { mutableStateOf(app.preferences.getVoiceEngineMode()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Voice Engine Architecture", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        ElevatedCard(shape = RoundedCornerShape(14.dp), colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = engineMode == VoiceEngineMode.SWIFT, onClick = {
                        engineMode = VoiceEngineMode.SWIFT
                        app.preferences.setVoiceEngineMode(VoiceEngineMode.SWIFT)
                    })
                    Text("EVA Swift", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text("Ultra-low latency streaming for fast voice answers.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 32.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ElevatedCard(shape = RoundedCornerShape(14.dp), colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = engineMode == VoiceEngineMode.SOUL, onClick = {
                        engineMode = VoiceEngineMode.SOUL
                        app.preferences.setVoiceEngineMode(VoiceEngineMode.SOUL)
                    })
                    Text("EVA Soul", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text("Rich emotional nuances and empathetic prosody.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 32.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ElevatedCard(shape = RoundedCornerShape(14.dp), colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = engineMode == VoiceEngineMode.AGENTIC, onClick = {
                        engineMode = VoiceEngineMode.AGENTIC
                        app.preferences.setVoiceEngineMode(VoiceEngineMode.AGENTIC)
                    })
                    Text("Advanced Agentic (Ear • Brain • Voice)", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text("EAR: Wake word + Noise suppression + VAD\nBRAIN: Gemini + OmniRoute + Memory + Planning\nVOICE: TTS + Interrupts + Streaming", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 32.dp, top = 4.dp))
            }
        }
    }
}

@Composable
fun VoiceProfileSettingsView() {
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()
    val profile by app.voiceProfileManager.currentProfile.collectAsState()
    val isMatchEnabled by app.voiceProfileManager.isVoiceMatchEnabled.collectAsState()
    val sensitivity by app.voiceProfileManager.sensitivity.collectAsState()

    LaunchedEffect(Unit) {
        app.voiceProfileManager.loadProfile()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Recognize My Voice (Voice Match)", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(checked = isMatchEnabled, onCheckedChange = {
                app.voiceProfileManager.setVoiceMatchEnabled(it)
            })
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Voice Sensitivity: ${(sensitivity * 100).toInt()}%", color = TextSecondary, fontSize = 13.sp)
        Slider(value = sensitivity, onValueChange = { app.voiceProfileManager.setSensitivity(it) })

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(shape = RoundedCornerShape(14.dp), colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Voice Profile Status", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (profile?.isTrained == true) "Trained (${profile?.sampleCount} samples recorded)" else "Not trained yet (Requires 3 samples)",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    app.voiceProfileManager.recordSample(samplePitchHz = 142f, sampleEnergy = 65f)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Record Voice Sample", fontWeight = FontWeight.Bold)
        }

        if (profile != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    coroutineScope.launch { app.voiceProfileManager.deleteProfile() }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Voice Profile")
            }
        }
    }
}

@Composable
fun SecretSettingsView() {
    val app = EvaApplication.instance
    var secretMode by remember { mutableStateOf(app.preferences.getSecretMode()) }
    var confirmed18 by remember { mutableStateOf(app.preferences.isIntenseModeConfirmed()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Personality Intensity", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        listOf("Friendly", "Romantic", "Intense").forEach { mode ->
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        if (mode == "Intense" && !confirmed18) {
                            // prompt verification
                        } else {
                            secretMode = mode
                            app.preferences.setSecretMode(mode)
                        }
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                    RadioButton(selected = secretMode == mode, onClick = {
                        if (mode != "Intense" || confirmed18) {
                            secretMode = mode
                            app.preferences.setSecretMode(mode)
                        }
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(mode, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (mode == "Intense") {
                            Text("18+ Only • Age verification and license required", color = StatusError, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (secretMode == "Intense" || !confirmed18) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = confirmed18, onCheckedChange = {
                    confirmed18 = it
                    app.preferences.setIntenseModeConfirmed(it)
                })
                Text("I certify that I am 18 years or older.", color = TextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SubscriptionSettingsView() {
    val app = EvaApplication.instance
    var currentPlan by remember { mutableStateOf(app.preferences.getSubscriptionPlan()) }
    var licenseKeyInput by remember { mutableStateOf(app.preferences.getLicenseKey()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Subscription Plans", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        SubscriptionPlan.values().forEach { plan ->
            val isCurrent = currentPlan == plan
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = if (isCurrent) DarkSurfaceVariant else DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(plan.name, color = if (isCurrent) NeonCyan else TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (isCurrent) {
                            Surface(shape = RoundedCornerShape(6.dp), color = NeonCyan.copy(alpha = 0.2f)) {
                                Text("ACTIVE", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(
                        text = when (plan) {
                            SubscriptionPlan.FREE -> "Core Assistant • Local Reminders • Standard Voice"
                            SubscriptionPlan.PREMIUM -> "Coding Agent • Website Builder • Cloud Connectors"
                            SubscriptionPlan.PRO -> "Agentic Voice • Pro WhatsApp & Phone Calls • Unlimited"
                            SubscriptionPlan.MAX -> "Max Automation • Priority Routing • All Skills"
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = licenseKeyInput,
            onValueChange = { licenseKeyInput = it },
            label = { Text("Activate License Key") },
            placeholder = { Text("Enter EVA-PRO license key") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (licenseKeyInput.isNotBlank()) {
                    app.preferences.setLicenseKey(licenseKeyInput.trim())
                    app.preferences.setSubscriptionPlan(SubscriptionPlan.PRO)
                    currentPlan = SubscriptionPlan.PRO
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Activate License", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AboutView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NeonCyan.copy(alpha = 0.15f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(40.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("EVA AI", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("“Your intelligent personal AI companion.”", color = NeonCyan, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))

        ElevatedCard(shape = RoundedCornerShape(14.dp), colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Developer / Creator", color = TextSecondary, fontSize = 13.sp)
                    Text("Aura RIFAT", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Version", color = TextSecondary, fontSize = 13.sp)
                    Text("1.0.0", color = TextPrimary, fontSize = 13.sp)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("AI Providers", color = TextSecondary, fontSize = 13.sp)
                    Text("Gemini & OmniRoute", color = NeonCyan, fontSize = 13.sp)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Wake Words", color = TextSecondary, fontSize = 13.sp)
                    Text("Hey EVA • Wake EVA", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun EdgeGlowSettingsView() {
    val context = LocalContext.current
    val app = EvaApplication.instance

    var edgeGlowEnabled by remember { mutableStateOf(app.preferences.isEdgeGlowEnabled()) }
    var showWhenClosed by remember { mutableStateOf(app.preferences.isShowEdgeGlowWhenClosed()) }
    var wakeAnim by remember { mutableStateOf(app.preferences.isWakeAnimationEnabled()) }
    var listeningAnim by remember { mutableStateOf(app.preferences.isListeningAnimationEnabled()) }
    var thinkingAnim by remember { mutableStateOf(app.preferences.isThinkingAnimationEnabled()) }
    var speakingAnim by remember { mutableStateOf(app.preferences.isSpeakingAnimationEnabled()) }
    var idleAnim by remember { mutableStateOf(app.preferences.isIdleAnimationEnabled()) }
    var intensity by remember { mutableFloatStateOf(app.preferences.getEdgeGlowIntensity()) }
    var batterySaver by remember { mutableStateOf(app.preferences.isEdgeGlowBatterySaver()) }

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(context)
            } else true
        )
    }

    val currentEvaState by com.example.voice.EvaStateManager.state.collectAsState()
    val currentAmp by com.example.voice.EvaStateManager.amplitude.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- DISPLAY OVER OTHER APPS PERMISSION CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasOverlayPermission) DarkSurface else StatusWarning.copy(alpha = 0.12f)
            ),
            border = BorderStroke(
                1.dp,
                if (hasOverlayPermission) StatusSuccess.copy(alpha = 0.4f) else StatusWarning.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasOverlayPermission) StatusSuccess else StatusWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Display Over Other Apps",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (hasOverlayPermission) "Granted" else "Required",
                        color = if (hasOverlayPermission) StatusSuccess else StatusWarning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (hasOverlayPermission) {
                        "EVA has permission to render futuristic edge glowing lines and corner animations above other apps and the home screen."
                    } else {
                        "To show the EVA Edge Glow overlay when other apps are open or the screen is minimized, grant 'Display over other apps' in Android settings."
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (!hasOverlayPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grant Overlay Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- INTERACTIVE LIVE PREVIEW CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicDarkBackground),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Live Interactive Preview",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current State: ${currentEvaState.name}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Corner glow preview box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    val previewOrbState = when (currentEvaState) {
                        com.example.voice.EvaState.SPEAKING -> com.example.ui.components.OrbState.SPEAKING
                        com.example.voice.EvaState.LISTENING -> com.example.ui.components.OrbState.LISTENING
                        com.example.voice.EvaState.WAKE -> com.example.ui.components.OrbState.WAKE_DETECTED
                        com.example.voice.EvaState.THINKING -> com.example.ui.components.OrbState.THINKING
                        com.example.voice.EvaState.ERROR -> com.example.ui.components.OrbState.ERROR
                        com.example.voice.EvaState.OFFLINE -> com.example.ui.components.OrbState.OFFLINE
                        com.example.voice.EvaState.IDLE -> com.example.ui.components.OrbState.IDLE
                    }

                    com.example.ui.components.ScreenCornerGlow(
                        isActive = currentEvaState != com.example.voice.EvaState.IDLE,
                        orbState = previewOrbState,
                        amplitude = currentAmp,
                        modifier = Modifier.fillMaxSize()
                    )

                    Text(
                        text = "EVA EDGE GLOW",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Test Animation State:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        com.example.voice.EvaState.IDLE,
                        com.example.voice.EvaState.WAKE,
                        com.example.voice.EvaState.LISTENING,
                        com.example.voice.EvaState.THINKING,
                        com.example.voice.EvaState.SPEAKING
                    ).forEach { st ->
                        FilterChip(
                            selected = currentEvaState == st,
                            onClick = {
                                when (st) {
                                    com.example.voice.EvaState.IDLE -> com.example.voice.EvaStateManager.setIdle()
                                    com.example.voice.EvaState.WAKE -> com.example.voice.EvaStateManager.setWake()
                                    com.example.voice.EvaState.LISTENING -> com.example.voice.EvaStateManager.setListening()
                                    com.example.voice.EvaState.THINKING -> com.example.voice.EvaStateManager.setThinking()
                                    com.example.voice.EvaState.SPEAKING -> com.example.voice.EvaStateManager.setSpeaking()
                                    else -> {}
                                }
                            },
                            label = { Text(st.name, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // --- OVERLAY CONTROLS ---
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Behavior & Display Options",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                // Enable Edge Glow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Edge Glow", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Display glowing neon corner lines and particles", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = edgeGlowEnabled,
                        onCheckedChange = {
                            edgeGlowEnabled = it
                            app.preferences.setEdgeGlowEnabled(it)
                            com.example.ui.components.EvaEdgeGlowOverlayManager.getInstance(context).updateOverlayVisibility()
                        }
                    )
                }

                HorizontalDivider(color = DarkOutline)

                // Show when app is closed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show When App Is Closed", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Maintain floating corner animation over other apps & home screen", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = showWhenClosed,
                        onCheckedChange = {
                            showWhenClosed = it
                            app.preferences.setShowEdgeGlowWhenClosed(it)
                            if (it) {
                                com.example.service.EvaVoiceService.startService(context)
                            }
                            com.example.ui.components.EvaEdgeGlowOverlayManager.getInstance(context).updateOverlayVisibility()
                        }
                    )
                }

                HorizontalDivider(color = DarkOutline)

                // Wake Animation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wake Animation", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Radial wave and corner brightening on 'Hey EVA'", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = wakeAnim,
                        onCheckedChange = {
                            wakeAnim = it
                            app.preferences.setWakeAnimationEnabled(it)
                        }
                    )
                }

                HorizontalDivider(color = DarkOutline)

                // Listening Animation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Listening Animation", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Dynamic emerald audio expansion responding to voice", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = listeningAnim,
                        onCheckedChange = {
                            listeningAnim = it
                            app.preferences.setListeningAnimationEnabled(it)
                        }
                    )
                }

                HorizontalDivider(color = DarkOutline)

                // Thinking Animation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thinking Animation", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Rotating light particles & arcs during AI processing", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = thinkingAnim,
                        onCheckedChange = {
                            thinkingAnim = it
                            app.preferences.setThinkingAnimationEnabled(it)
                        }
                    )
                }

                HorizontalDivider(color = DarkOutline)

                // Speaking Animation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Speaking Animation", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Rhythmic speech wave pulses synchronized with TTS voice", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = speakingAnim,
                        onCheckedChange = {
                            speakingAnim = it
                            app.preferences.setSpeakingAnimationEnabled(it)
                        }
                    )
                }

                HorizontalDivider(color = DarkOutline)

                // Idle Animation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Idle Animation", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Slow breathing pulse and ambient corner halos", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = idleAnim,
                        onCheckedChange = {
                            idleAnim = it
                            app.preferences.setIdleAnimationEnabled(it)
                        }
                    )
                }

                HorizontalDivider(color = DarkOutline)

                // Animation Intensity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Animation Intensity", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(intensity * 100).toInt()}%", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = intensity,
                        onValueChange = {
                            intensity = it
                            app.preferences.setEdgeGlowIntensity(it)
                        },
                        valueRange = 0.2f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }

                HorizontalDivider(color = DarkOutline)

                // Battery Saver Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Battery Saver Mode", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Limits rendering rate & pauses idle particle computations", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = batterySaver,
                        onCheckedChange = {
                            batterySaver = it
                            app.preferences.setEdgeGlowBatterySaver(it)
                        }
                    )
                }
            }
        }
    }
}
