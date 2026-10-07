package com.example.ui.settings

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EvaApplication
import com.example.connectors.TelegramBotStatus
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TelegramBotSettingsView() {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()

    var botTokenInput by remember { mutableStateOf(app.telegramBotManager.getBotToken()) }
    var tokenVisible by remember { mutableStateOf(false) }
    var isPollingActive by remember { mutableStateOf(false) }

    val status by app.telegramBotManager.status.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("TELEGRAM BOT INTEGRATION", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Connect EVA AI to Telegram to receive queries, execute device actions, and chat via text or voice.", color = TextMuted, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(14.dp))

        ElevatedCard(shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = botTokenInput,
                    onValueChange = { botTokenInput = it },
                    label = { Text("Telegram Bot Token") },
                    placeholder = { Text("Paste Bot Token from @BotFather") },
                    singleLine = true,
                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = TextMuted)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Connection status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, color, text) = when (val s = status) {
                        TelegramBotStatus.Disconnected -> Triple(Icons.Default.LinkOff, TextMuted, "Disconnected")
                        TelegramBotStatus.Connecting -> Triple(Icons.Default.Sync, StatusWarning, "Testing...")
                        is TelegramBotStatus.Connected -> Triple(Icons.Default.CheckCircle, StatusSuccess, "Connected (@${s.username})")
                        is TelegramBotStatus.Error -> Triple(Icons.Default.ErrorOutline, StatusError, "Error: ${s.message}")
                    }
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            app.telegramBotManager.setBotToken(botTokenInput.trim())
                            coroutineScope.launch { app.telegramBotManager.testConnection() }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Connection")
                    }

                    Button(
                        onClick = {
                            app.telegramBotManager.setBotToken(botTokenInput.trim())
                            Toast.makeText(context, "Bot Token securely saved", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Token", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Polling Service", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Listen for incoming messages & voice chats", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isPollingActive,
                        onCheckedChange = { active ->
                            isPollingActive = active
                            if (active) {
                                app.telegramBotManager.startPolling(coroutineScope)
                                Toast.makeText(context, "Telegram Bot polling started", Toast.LENGTH_SHORT).show()
                            } else {
                                app.telegramBotManager.stopPolling()
                                Toast.makeText(context, "Telegram Bot stopped", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Text("Supported Features:", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Receive voice messages and audio from Telegram\n• Speech-to-Text conversion using EVA natural voice pipeline\n• Multilingual understanding (Bangla, Banglish, English, Hindi)\n• Generated voice audio replies\n• Commands: /start, /help, /newchat, /clear", color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}
