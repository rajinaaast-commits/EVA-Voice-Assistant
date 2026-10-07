package com.example.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EvaApplication
import com.example.ui.components.AnimatedVoiceOrb
import com.example.ui.components.OrbState
import com.example.ui.theme.*
import com.example.voice.SpeechCleaner
import com.example.voice.WakeWordDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class HomeWidget(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val routeTarget: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (initialPrompt: String?) -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToCoding: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWebsite: () -> Unit
) {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var orbState by remember { mutableStateOf(OrbState.IDLE) }
    var textInput by remember { mutableStateOf("") }
    var assistantDialogue by remember { mutableStateOf("Ready to assist you.") }
    var wakeWordDetectedNotice by remember { mutableStateOf<String?>(null) }
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }

    // Real-time audio amplitude collection
    val isListening by app.speechHelper.isListening.collectAsState()
    val isSpeaking by app.ttsHelper.isSpeaking.collectAsState()
    val micAmplitude by app.speechHelper.rmsAmplitude.collectAsState()

    // Sync orb state with audio/TTS activity
    LaunchedEffect(isListening, isSpeaking) {
        if (isSpeaking) {
            orbState = OrbState.SPEAKING
        } else if (isListening) {
            orbState = OrbState.LISTENING
        } else if (orbState == OrbState.SPEAKING || orbState == OrbState.LISTENING) {
            orbState = OrbState.IDLE
        }
    }

    // Default widgets list with enable/reorder support
    val activeWidgets = remember {
        mutableStateListOf(
            HomeWidget("weather", "24°C Partly Cloudy", "High 27° • Humidity 62%", Icons.Default.WbSunny, Color(0xFFFFB300), "weather"),
            HomeWidget("study", "Study Focus", "2 tasks scheduled today", Icons.Default.MenuBook, NeonCyan, "study"),
            HomeWidget("music", "Deep Flow Beats", "Ambient Synthwave 112 BPM", Icons.Default.MusicNote, ElectricViolet, "music"),
            HomeWidget("journal", "Daily Memory", "3 memories recorded", Icons.Default.EditNote, HologramBlue, "journal"),
            HomeWidget("date", SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()), "All systems operational", Icons.Default.CalendarToday, StatusSuccess, "date"),
            HomeWidget("mood", "AI Resonance", "Calm & Focused", Icons.Default.Mood, CyberPink, "mood")
        )
    }

    var showAddWidgetDialog by remember { mutableStateOf(false) }

    // File / Image Picker Launcher
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedAttachmentUri = uri
        if (uri != null) {
            Toast.makeText(context, "Attachment loaded: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
        }
    }

    // Pipeline voice command handler
    fun processVoiceCommand(rawUtterance: String) {
        coroutineScope.launch {
            // 1. Wake word detection & phonetic variations
            val wakeResult = WakeWordDetector.detectAndStrip(rawUtterance)
            if (wakeResult.detected) {
                orbState = OrbState.WAKE_DETECTED
                wakeWordDetectedNotice = "Recognized: \"${wakeResult.matchedPhrase}\""
                delay(400)
            }

            // 2. Disfluency cleaning (umm, uh, contextual like)
            val cleanedCommand = SpeechCleaner.clean(wakeResult.cleanCommand)

            if (cleanedCommand.isBlank()) {
                assistantDialogue = "I heard you! How can I help you?"
                orbState = OrbState.SPEAKING
                app.ttsHelper.speak(assistantDialogue)
                return@launch
            }

            assistantDialogue = "Command: \"$cleanedCommand\""
            orbState = OrbState.THINKING

            // 3. Check for direct device launch commands (e.g. "open YouTube")
            if (cleanedCommand.lowercase(Locale.ROOT).contains("youtube")) {
                assistantDialogue = "Opening YouTube."
                orbState = OrbState.SPEAKING
                app.ttsHelper.speak("Opening YouTube.")
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                    context.startActivity(intent)
                } catch (_: Exception) {}
                delay(1200)
                orbState = OrbState.IDLE
                return@launch
            }

            // 4. Send to AI Provider Manager
            val response = app.aiProviderManager.generateResponse(prompt = cleanedCommand)

            if (response.isSuccess) {
                assistantDialogue = response.text
                orbState = OrbState.SPEAKING
                app.ttsHelper.speak(response.text)

                // Execute any tool calls
                for (tool in response.toolCalls) {
                    val toolResult = app.aiProviderManager.executeTool(tool)
                    if (toolResult.requiresConfirmation) {
                        Toast.makeText(context, toolResult.pendingActionDescription ?: "Confirmation required", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                assistantDialogue = response.text
                orbState = OrbState.ERROR
                app.ttsHelper.speak(response.text)
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicDarkBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = NeonCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "EVA Logo", tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "EVA AI", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Aura RIFAT", color = TextMuted, fontSize = 11.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onNavigateToChat(null) }, modifier = Modifier.testTag("nav_chat_button")) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat", tint = TextPrimary)
                    }
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("nav_settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Quick Action Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    val quickActions = listOf(
                        "Ask EVA" to { onNavigateToChat(null) },
                        "Search" to { onNavigateToChat("Search for recent tech innovations in 2026") },
                        "Study" to onNavigateToStudy,
                        "Write Code" to onNavigateToCoding,
                        "Create Site" to onNavigateToWebsite
                    )
                    items(quickActions) { (label, action) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier.clickable { action() }
                        ) {
                            Text(
                                text = label,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Input Bar: File, Camera, Text, Mic
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CosmicDarkBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = { filePicker.launch("*/*") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach File", tint = TextMuted)
                        }

                        IconButton(
                            onClick = { filePicker.launch("image/*") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = TextMuted)
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Ask EVA anything...", color = TextMuted, fontSize = 14.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (textInput.isNotBlank()) {
                                    val q = textInput.trim()
                                    textInput = ""
                                    processVoiceCommand(q)
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = NeonCyan
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_text_input")
                        )

                        // Voice Mic / Send button
                        Surface(
                            shape = CircleShape,
                            color = if (isListening) StatusError else NeonCyan,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    if (textInput.isNotBlank()) {
                                        val q = textInput.trim()
                                        textInput = ""
                                        processVoiceCommand(q)
                                    } else {
                                        if (isListening) {
                                            app.speechHelper.stopListening()
                                        } else {
                                            if (isSpeaking) app.ttsHelper.stop()
                                            app.speechHelper.startListening(
                                                onFinalResult = { recognized ->
                                                    processVoiceCommand(recognized)
                                                },
                                                onError = { err ->
                                                    assistantDialogue = err
                                                    orbState = OrbState.ERROR
                                                }
                                            )
                                        }
                                    }
                                }
                                .testTag("voice_trigger_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (textInput.isNotBlank()) Icons.Default.Send else if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = CosmicDarkBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = CosmicDarkBackground
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Wake Word hint banner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceVariant.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Hearing, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Say “Hey EVA” or “Wake EVA”",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            wakeWordDetectedNotice?.let { notice ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = notice, color = StatusSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Center Animated Voice Orb
            AnimatedVoiceOrb(
                state = orbState,
                amplitude = if (isListening) micAmplitude else if (isSpeaking) 0.65f else 0.2f,
                onClick = {
                    if (isSpeaking) {
                        app.ttsHelper.stop()
                        orbState = OrbState.IDLE
                    } else if (isListening) {
                        app.speechHelper.stopListening()
                    } else {
                        app.speechHelper.startListening(
                            onFinalResult = { recognized ->
                                processVoiceCommand(recognized)
                            },
                            onError = { err ->
                                assistantDialogue = err
                                orbState = OrbState.ERROR
                            }
                        )
                    }
                },
                modifier = Modifier.testTag("animated_voice_orb")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Assistant Live Dialogue Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "EVA Response",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (isSpeaking) {
                            TextButton(
                                onClick = { app.ttsHelper.stop() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Stop Speaking", color = StatusError, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = assistantDialogue,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Widgets Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ASSISTANT WIDGETS",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                IconButton(onClick = { showAddWidgetDialog = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "Manage Widgets", tint = NeonCyan, modifier = Modifier.size(18.dp))
                }
            }

            // Grid / List of Widgets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activeWidgets.chunked(2).forEach { rowWidgets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowWidgets.forEach { widget ->
                            ElevatedCard(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        when (widget.routeTarget) {
                                            "study" -> onNavigateToStudy()
                                            "weather" -> onNavigateToChat("What is today's detailed weather forecast?")
                                            "journal" -> onNavigateToChat("Summarize my recent memory journal")
                                            "music" -> onNavigateToChat("Play focus ambient soundscapes")
                                            else -> onNavigateToChat(null)
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = widget.icon,
                                            contentDescription = widget.title,
                                            tint = widget.accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowOutward,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = widget.title,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = widget.subtitle,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (rowWidgets.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddWidgetDialog) {
        AlertDialog(
            onDismissRequest = { showAddWidgetDialog = false },
            title = { Text("Customize Widgets", color = TextPrimary) },
            text = {
                Text(
                    "You can toggle widgets or tap any widget to launch its companion AI workspace (Study, Weather, Music, Journal).",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showAddWidgetDialog = false }) {
                    Text("Done", color = NeonCyan)
                }
            },
            containerColor = DarkSurface
        )
    }
}
