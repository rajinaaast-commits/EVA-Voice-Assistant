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
import com.example.connectors.SmtpConfig
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GmailSmtpSettingsView() {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()

    val initialConfig = remember { app.gmailSmtpManager.loadConfig() }

    var email by remember { mutableStateOf(initialConfig.email) }
    var appPassword by remember { mutableStateOf(initialConfig.appPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf(initialConfig.host) }
    var port by remember { mutableStateOf(initialConfig.port.toString()) }
    var senderName by remember { mutableStateOf(initialConfig.senderName) }
    var signature by remember { mutableStateOf(initialConfig.signature) }

    val isConnected by app.gmailSmtpManager.isConnected.collectAsState()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("GMAIL & CUSTOM SMTP CONNECTOR", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Send, draft, reply and forward emails directly through secure Gmail or SMTP server.", color = TextMuted, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(14.dp))

        ElevatedCard(shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Gmail / Email Address") },
                    placeholder = { Text("user@gmail.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = appPassword,
                    onValueChange = { appPassword = it },
                    label = { Text("App Password") },
                    placeholder = { Text("16-character Google App Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = TextMuted)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("SMTP Host") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Sender Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = signature,
                    onValueChange = { signature = it },
                    label = { Text("Email Signature") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.LinkOff,
                        contentDescription = null,
                        tint = if (isConnected) StatusSuccess else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isConnected) "Connected" else "Not Connected", color = if (isConnected) StatusSuccess else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                statusMessage?.let {
                    Text(it, color = if (isConnected) StatusSuccess else StatusError, fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val cfg = SmtpConfig(
                                email = email.trim(),
                                appPassword = appPassword.trim(),
                                host = host.trim(),
                                port = port.toIntOrNull() ?: 465,
                                senderName = senderName.trim(),
                                signature = signature.trim()
                            )
                            app.gmailSmtpManager.saveConfig(cfg)
                            isTesting = true
                            coroutineScope.launch {
                                val (ok, msg) = app.gmailSmtpManager.testConnection()
                                isTesting = false
                                statusMessage = msg
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTesting) CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp))
                        else Text("Test Connection")
                    }

                    Button(
                        onClick = {
                            val cfg = SmtpConfig(
                                email = email.trim(),
                                appPassword = appPassword.trim(),
                                host = host.trim(),
                                port = port.toIntOrNull() ?: 465,
                                senderName = senderName.trim(),
                                signature = signature.trim()
                            )
                            app.gmailSmtpManager.saveConfig(cfg)
                            Toast.makeText(context, "Gmail SMTP configuration saved", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Config", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CosmicDarkBackground.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Note: Gmail App Passwords require 2-Step Verification enabled on your Google Account. Credentials are encrypted securely via Android KeyStore.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
