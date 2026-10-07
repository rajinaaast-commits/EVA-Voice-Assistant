package com.example.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EvaApplication
import com.example.ai.ConnectionStatus
import com.example.data.preferences.AIProviderMode
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun Step1ApiSetupScreen(
    onContinue: () -> Unit
) {
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()

    var geminiKeyInput by remember { mutableStateOf(app.aiProviderManager.getGeminiKey()) }
    var geminiKeyVisible by remember { mutableStateOf(false) }
    var geminiStatus by remember {
        mutableStateOf<ConnectionStatus>(
            if (app.aiProviderManager.isGeminiConfigured()) ConnectionStatus.Connected else ConnectionStatus.NotConfigured
        )
    }

    var omniKeyInput by remember { mutableStateOf(app.aiProviderManager.getOmniRouteKey()) }
    var omniKeyVisible by remember { mutableStateOf(false) }
    var omniBaseUrl by remember { mutableStateOf(app.preferences.getOmniRouteBaseUrl()) }
    var omniModel by remember { mutableStateOf(app.preferences.getOmniRouteModel()) }
    var omniStatus by remember {
        mutableStateOf<ConnectionStatus>(
            if (app.aiProviderManager.isOmniRouteConfigured()) ConnectionStatus.Connected else ConnectionStatus.NotConfigured
        )
    }

    var selectedMode by remember { mutableStateOf(app.preferences.getProviderMode()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Step indicator
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "Step 1 of 3 • AI API Setup",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Text(
            text = "Connect EVA's AI Brain",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Add your AI API keys to power EVA. Keys are encrypted securely in Android hardware KeyStore.",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Google Gemini Card
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gemini_api_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini",
                        tint = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Gemini",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = geminiKeyInput,
                    onValueChange = {
                        geminiKeyInput = it
                        errorMessage = null
                    },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("Paste your Gemini API key") },
                    singleLine = true,
                    visualTransformation = if (geminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { geminiKeyVisible = !geminiKeyVisible }) {
                            Icon(
                                imageVector = if (geminiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Gemini Key Visibility",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkOutline,
                        focusedLabelColor = NeonCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_key_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status indicator
                ConnectionStatusBadge(status = geminiStatus)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                geminiStatus = ConnectionStatus.Testing
                                val result = app.aiProviderManager.geminiProvider.testConnection(geminiKeyInput)
                                geminiStatus = result
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test")
                    }

                    Button(
                        onClick = {
                            app.aiProviderManager.setGeminiKey(geminiKeyInput.trim())
                            saveSuccessMessage = "Gemini key saved securely!"
                            coroutineScope.launch {
                                geminiStatus = app.aiProviderManager.testGeminiConnection()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }

                    if (geminiKeyInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                geminiKeyInput = ""
                                app.aiProviderManager.setGeminiKey("")
                                geminiStatus = ConnectionStatus.NotConfigured
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Key", tint = StatusError)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // OmniRoute Card
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("omniroute_api_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "OmniRoute",
                        tint = ElectricViolet
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OmniRoute",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = omniKeyInput,
                    onValueChange = {
                        omniKeyInput = it
                        errorMessage = null
                    },
                    label = { Text("OmniRoute API Key") },
                    placeholder = { Text("Paste your OmniRoute API key") },
                    singleLine = true,
                    visualTransformation = if (omniKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { omniKeyVisible = !omniKeyVisible }) {
                            Icon(
                                imageVector = if (omniKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle OmniRoute Key Visibility",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = DarkOutline,
                        focusedLabelColor = ElectricViolet
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("omniroute_key_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // OmniRoute base URL
                OutlinedTextField(
                    value = omniBaseUrl,
                    onValueChange = {
                        omniBaseUrl = it
                        app.preferences.setOmniRouteBaseUrl(it)
                    },
                    label = { Text("API Base URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = DarkOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // OmniRoute Model ID
                OutlinedTextField(
                    value = omniModel,
                    onValueChange = {
                        omniModel = it
                        app.preferences.setOmniRouteModel(it)
                    },
                    label = { Text("Model ID (default 'auto')") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = DarkOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                ConnectionStatusBadge(status = omniStatus)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                omniStatus = ConnectionStatus.Testing
                                val result = app.aiProviderManager.omniRouteProvider.testConnection(omniKeyInput, omniBaseUrl)
                                omniStatus = result
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricViolet),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test")
                    }

                    Button(
                        onClick = {
                            app.aiProviderManager.setOmniRouteKey(omniKeyInput.trim())
                            app.preferences.setOmniRouteBaseUrl(omniBaseUrl.trim())
                            app.preferences.setOmniRouteModel(omniModel.trim())
                            saveSuccessMessage = "OmniRoute configuration saved!"
                            coroutineScope.launch {
                                omniStatus = app.aiProviderManager.testOmniRouteConnection()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }

                    if (omniKeyInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                omniKeyInput = ""
                                app.aiProviderManager.setOmniRouteKey("")
                                omniStatus = ConnectionStatus.NotConfigured
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Key", tint = StatusError)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Provider Mode
        Text(
            text = "AI PROVIDER MODE",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AIProviderMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedMode = mode
                        app.preferences.setProviderMode(mode)
                    },
                    label = {
                        Text(
                            text = when (mode) {
                                AIProviderMode.AUTO -> "Auto"
                                AIProviderMode.GEMINI -> "Gemini"
                                AIProviderMode.OMNIROUTE -> "OmniRoute"
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = CosmicDarkBackground,
                        containerColor = DarkSurface,
                        labelColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = when (selectedMode) {
                AIProviderMode.AUTO -> "Auto: EVA automatically chooses the available configured provider with fallback."
                AIProviderMode.GEMINI -> "Gemini: EVA uses Google Gemini directly with rich multimodal capabilities."
                AIProviderMode.OMNIROUTE -> "OmniRoute: EVA routes requests through the OmniRoute layer with failover."
            },
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        errorMessage?.let {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StatusError.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(it, color = StatusError, fontSize = 13.sp)
                }
            }
        }

        saveSuccessMessage?.let {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StatusSuccess.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(it, color = StatusSuccess, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue Button
        Button(
            onClick = {
                if (!app.aiProviderManager.hasAnyConfigured()) {
                    errorMessage = "Add at least one AI API key to continue."
                } else {
                    onContinue()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("continue_to_step2_button")
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ConnectionStatusBadge(status: ConnectionStatus) {
    val (text, color, icon) = when (status) {
        ConnectionStatus.NotConfigured -> Triple("Not configured", TextMuted, Icons.Default.LinkOff)
        ConnectionStatus.Testing -> Triple("Testing...", StatusWarning, Icons.Default.Sync)
        ConnectionStatus.Connected -> Triple("Connected", StatusSuccess, Icons.Default.CheckCircle)
        ConnectionStatus.InvalidKey -> Triple("Invalid key", StatusError, Icons.Default.HighlightOff)
        ConnectionStatus.QuotaExceeded -> Triple("Quota exceeded", StatusWarning, Icons.Default.HourglassBottom)
        is ConnectionStatus.Error -> Triple("Error: ${status.message.take(28)}", StatusError, Icons.Default.ErrorOutline)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
