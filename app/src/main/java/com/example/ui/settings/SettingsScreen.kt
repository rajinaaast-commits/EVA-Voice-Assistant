package com.example.ui.settings

import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EvaApplication
import com.example.data.preferences.AIProviderMode
import com.example.data.preferences.SubscriptionPlan
import com.example.data.preferences.VoiceEngineMode
import com.example.ui.theme.*
import com.example.voice.EvaTtsHelper
import kotlinx.coroutines.launch

enum class SettingsSubpage {
    MAIN,
    API_KEYS,
    EVA_PERSONA,
    MEMORY,
    VOICE_SETTINGS,
    VOICE_ENGINE,
    VOICE_PROFILE,
    SECRET_SETTINGS,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (activeSubpage) {
                            SettingsSubpage.MAIN -> "Settings"
                            SettingsSubpage.API_KEYS -> "AI Providers & API Keys"
                            SettingsSubpage.EVA_PERSONA -> "EVA Persona & Tone"
                            SettingsSubpage.MEMORY -> "Memory & Privacy"
                            SettingsSubpage.VOICE_SETTINGS -> "Voice & Audio"
                            SettingsSubpage.VOICE_ENGINE -> "Voice Engine Architecture"
                            SettingsSubpage.VOICE_PROFILE -> "Voice Recognition Profile"
                            SettingsSubpage.SECRET_SETTINGS -> "Secret Settings"
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
                SettingsSubpage.API_KEYS -> ApiKeysSettingsView()
                SettingsSubpage.EVA_PERSONA -> PersonaSettingsView()
                SettingsSubpage.MEMORY -> MemorySettingsView()
                SettingsSubpage.VOICE_SETTINGS -> VoiceSettingsView()
                SettingsSubpage.VOICE_ENGINE -> VoiceEngineSettingsView()
                SettingsSubpage.VOICE_PROFILE -> VoiceProfileSettingsView()
                SettingsSubpage.SECRET_SETTINGS -> SecretSettingsView()
                SettingsSubpage.SUBSCRIPTION -> SubscriptionSettingsView()
                SettingsSubpage.ABOUT -> AboutView()
            }
        }
    }
}

@Composable
fun MainSettingsMenu(onSelectSubpage: (SettingsSubpage) -> Unit) {
    val items = listOf(
        Triple("AI Providers & Keys", "Configure Gemini & OmniRoute keys and models", SettingsSubpage.API_KEYS),
        Triple("EVA Persona", "Personalize name, tone, custom instructions", SettingsSubpage.EVA_PERSONA),
        Triple("Memory & Privacy", "Persistent autonomous memories & incognito toggle", SettingsSubpage.MEMORY),
        Triple("Voice & Audio", "Select from 10 voices, speed, pitch, interrupt", SettingsSubpage.VOICE_SETTINGS),
        Triple("Voice Engine", "Swift, Soul, or Advanced Agentic Ear-Brain-Voice", SettingsSubpage.VOICE_ENGINE),
        Triple("Voice Profile", "Train your voice match & anti-spoof sensitivity", SettingsSubpage.VOICE_PROFILE),
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
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()
    var geminiKey by remember { mutableStateOf(app.aiProviderManager.getGeminiKey()) }
    var omniKey by remember { mutableStateOf(app.aiProviderManager.getOmniRouteKey()) }
    var omniUrl by remember { mutableStateOf(app.preferences.getOmniRouteBaseUrl()) }
    var geminiModel by remember { mutableStateOf(app.preferences.getGeminiModel()) }
    var omniModel by remember { mutableStateOf(app.preferences.getOmniRouteModel()) }
    var mode by remember { mutableStateOf(app.preferences.getProviderMode()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("AI Provider Mode", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = geminiKey,
            onValueChange = { geminiKey = it },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = geminiModel,
            onValueChange = {
                geminiModel = it
                app.preferences.setGeminiModel(it)
            },
            label = { Text("Gemini Model ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = omniKey,
            onValueChange = { omniKey = it },
            label = { Text("OmniRoute API Key") },
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                app.aiProviderManager.setGeminiKey(geminiKey.trim())
                app.aiProviderManager.setOmniRouteKey(omniKey.trim())
                app.preferences.setGeminiModel(geminiModel.trim())
                app.preferences.setOmniRouteModel(omniModel.trim())
                app.preferences.setOmniRouteBaseUrl(omniUrl.trim())
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save API Settings", fontWeight = FontWeight.Bold)
        }
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
