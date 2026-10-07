package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EvaApplication
import com.example.service.EVAAccessibilityService
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeviceControlSettingsView() {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val history by app.deviceControlManager.actionHistory.collectAsState()

    var isAccessibilityActive by remember {
        mutableStateOf(EVAAccessibilityService.isEnabledInSystem(context))
    }

    val isTermuxInstalled = remember {
        try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    var testStatusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "DEVICE CONTROL CENTER",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Actions status list
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CapabilityRow(
                    title = "App Launching",
                    subtitle = "Package Manager & Launcher Intents",
                    isEnabled = true,
                    isOptional = false,
                    onOpenSettings = null
                )

                HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f))

                CapabilityRow(
                    title = "Android System Intents",
                    subtitle = "Wi-Fi, Bluetooth, Battery, Display",
                    isEnabled = true,
                    isOptional = false,
                    onOpenSettings = null
                )

                HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f))

                CapabilityRow(
                    title = "Accessibility Service",
                    subtitle = if (isAccessibilityActive) "Active • Global Navigation & Gestures" else "Optional • Required for Back, Recents, Scroll",
                    isEnabled = isAccessibilityActive,
                    isOptional = true,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f))

                CapabilityRow(
                    title = "Termux Integration",
                    subtitle = if (isTermuxInstalled) "Termux App Installed • Ready" else "Termux not installed on this device",
                    isEnabled = isTermuxInstalled,
                    isOptional = true,
                    onOpenSettings = {
                        if (isTermuxInstalled) {
                            app.deviceControlManager.openApp("termux")
                        } else {
                            Toast.makeText(context, "Install Termux from F-Droid or GitHub releases", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AccessibilityNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enable Controls", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    // Actual test: Adjust volume and report real result
                    val res = app.deviceControlManager.adjustVolume(increase = true, originalCommand = "Self Test")
                    testStatusMessage = "Device Control Test: ${res.message} (Volume adjusted)"
                    Toast.makeText(context, testStatusMessage, Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Controls", fontSize = 12.sp)
            }
        }

        testStatusMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StatusSuccess.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    color = StatusSuccess,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action History Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ACTION HISTORY (${history.size})",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (history.isNotEmpty()) {
                TextButton(onClick = { app.deviceControlManager.clearHistory() }) {
                    Text("Clear", color = StatusError, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (history.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(20.dp)) {
                    Text("No device actions executed yet. Say \"Hey EVA, open YouTube\" or \"Hey EVA, open Termux\" to begin.", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                items(history, key = { it.id }) { item ->
                    ElevatedCard(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                imageVector = if (item.success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (item.success) StatusSuccess else StatusError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.command,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${item.actionType} • ${item.details}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp)),
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    isOptional: Boolean,
    onOpenSettings: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isEnabled) StatusSuccess.copy(alpha = 0.2f) else DarkSurfaceVariant,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = if (isEnabled) StatusSuccess else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isEnabled) StatusSuccess.copy(alpha = 0.15f) else DarkSurfaceVariant
                ) {
                    Text(
                        text = if (isEnabled) "Enabled" else if (isOptional) "Optional" else "Required",
                        color = if (isEnabled) StatusSuccess else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp)
        }

        if (onOpenSettings != null) {
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NeonCyan, modifier = Modifier.size(16.dp))
            }
        }
    }
}
