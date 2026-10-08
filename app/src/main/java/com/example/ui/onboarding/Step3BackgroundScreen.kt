package com.example.ui.onboarding

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.EvaApplication
import com.example.service.EVAAccessibilityService
import com.example.service.EVANotificationListenerService
import com.example.service.EVAVoiceInteractionService
import com.example.service.EvaVoiceService
import com.example.ui.theme.*

data class BackgroundConfigItem(
    val id: String,
    val title: String,
    val description: String,
    val reason: String,
    val icon: ImageVector,
    val activeLabel: String,
    val inactiveLabel: String,
    val isVerified: (Context) -> Boolean,
    val getActionIntent: (Context) -> Intent? = { null },
    val onDirectAction: ((Context) -> Unit)? = null
)

@Composable
fun Step3BackgroundScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val scrollState = rememberScrollState()

    val configItems = remember {
        listOf(
            BackgroundConfigItem(
                id = "notification",
                title = "Notification Access",
                description = "Read WhatsApp, calls & system alerts",
                reason = "Allows EVA to announce incoming calls, WhatsApp messages, and auto-reply during driving mode.",
                icon = Icons.Default.NotificationsActive,
                activeLabel = "Allowed",
                inactiveLabel = "Not Allowed",
                isVerified = { ctx ->
                    EVANotificationListenerService.isNotificationAccessGranted(ctx)
                },
                getActionIntent = { _ ->
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                }
            ),
            BackgroundConfigItem(
                id = "default_assistant",
                title = "Default Assistant",
                description = "System-level Android digital assistant",
                reason = "Enables holding the home button or power button to invoke EVA instead of Google Assistant.",
                icon = Icons.Default.Assistant,
                activeLabel = "EVA",
                inactiveLabel = "Not Default",
                isVerified = { ctx ->
                    EVAVoiceInteractionService.isDefaultAssistant(ctx)
                },
                getActionIntent = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            val rm = ctx.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                                rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
                            } else {
                                Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                            }
                        } catch (_: Exception) {
                            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                        }
                    } else {
                        Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                    }
                }
            ),
            BackgroundConfigItem(
                id = "voice_interaction",
                title = "VoiceInteractionService",
                description = "Native Android voice assistant session service",
                reason = "Enables system-level session routing and seamless launch from lockscreen or gestures.",
                icon = Icons.Default.HeadsetMic,
                activeLabel = "Active",
                inactiveLabel = "Inactive",
                isVerified = { ctx ->
                    EVAVoiceInteractionService.isActive(ctx)
                },
                getActionIntent = { _ ->
                    Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                }
            ),
            BackgroundConfigItem(
                id = "microphone",
                title = "Microphone",
                description = "Record speech & ambient wake phrases",
                reason = "Required for local wake word detection and speech recognition.",
                icon = Icons.Default.Mic,
                activeLabel = "Allowed",
                inactiveLabel = "Restricted",
                isVerified = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                },
                getActionIntent = { ctx ->
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                }
            ),
            BackgroundConfigItem(
                id = "wake_word",
                title = "Wake Word",
                description = "Listen for 'Hey EVA' / 'Wake EVA'",
                reason = "Runs battery-efficient local keyword detection on device.",
                icon = Icons.Default.SpatialAudio,
                activeLabel = "Enabled",
                inactiveLabel = "Disabled",
                isVerified = { _ ->
                    app.preferences.isWakeWordEnabled()
                },
                onDirectAction = { _ ->
                    val next = !app.preferences.isWakeWordEnabled()
                    app.preferences.setWakeWordEnabled(next)
                    if (next) {
                        EvaVoiceService.startService(context)
                    }
                }
            ),
            BackgroundConfigItem(
                id = "battery",
                title = "Battery Optimization",
                description = "Unrestricted background execution",
                reason = "Prevents Android OS from killing EVA's background wake detection when device is in deep sleep.",
                icon = Icons.Default.BatteryChargingFull,
                activeLabel = "Unrestricted",
                inactiveLabel = "Restricted",
                isVerified = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
                        pm?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false
                    } else true
                },
                getActionIntent = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                        } catch (_: Exception) {
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        }
                    } else Intent(Settings.ACTION_SETTINGS)
                }
            ),
            BackgroundConfigItem(
                id = "overlay",
                title = "Display Over Other Apps",
                description = "EVA Edge Glow overlay above any app",
                reason = "Allows EVA's futuristic corner glow to remain visible over apps and games.",
                icon = Icons.Default.Layers,
                activeLabel = "Allowed",
                inactiveLabel = "Restricted",
                isVerified = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Settings.canDrawOverlays(ctx)
                    } else true
                },
                getActionIntent = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                    } else Intent(Settings.ACTION_SETTINGS)
                }
            ),
            BackgroundConfigItem(
                id = "background_assistant",
                title = "Background Assistant",
                description = "Always-ready foreground assistant service",
                reason = "Keeps EVA running reliably in the background with persistent status notification.",
                icon = Icons.Default.Bolt,
                activeLabel = "Active",
                inactiveLabel = "Inactive",
                isVerified = { ctx ->
                    EvaVoiceService.isRunning(ctx)
                },
                onDirectAction = { ctx ->
                    if (EvaVoiceService.isRunning(ctx)) {
                        EvaVoiceService.stopService(ctx)
                    } else {
                        EvaVoiceService.startService(ctx)
                    }
                }
            ),
            BackgroundConfigItem(
                id = "accessibility",
                title = "Accessibility Service",
                description = "Automate device workflows & actions",
                reason = "Empowers EVA to perform automation commands, open apps, and navigate on your behalf.",
                icon = Icons.Default.AccessibilityNew,
                activeLabel = "Active",
                inactiveLabel = "Not Enabled",
                isVerified = { ctx ->
                    EVAAccessibilityService.isEnabledInSystem(ctx)
                },
                getActionIntent = { _ ->
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                }
            )
        )
    }

    var verifiedStates by remember {
        mutableStateOf(configItems.associate { it.id to it.isVerified(context) })
    }

    fun refreshAllStates() {
        verifiedStates = configItems.associate { it.id to it.isVerified(context) }
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshAllStates()
    }

    // Refresh permissions automatically when returning from Settings via Lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAllStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val verifiedCount = verifiedStates.values.count { it }
    val totalCount = configItems.size
    val hasAnyVerified = verifiedCount > 0

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

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "Step 3 of 3 • Real System Services",
                color = ElectricViolet,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Text(
            text = "Background & Assistant Setup",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Configure native Android permissions so EVA can run in the background, detect 'Hey EVA', and display Edge Glow above all apps.",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Prominent Verified Banner
        if (hasAnyVerified) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StatusSuccess.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, StatusSuccess),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("permissions_verified_banner_step3")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = StatusSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "System Status Verified",
                            color = StatusSuccess,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (verifiedCount == totalCount) {
                                "All 9 native system capabilities active and operational!"
                            } else {
                                "$verifiedCount of $totalCount native Android capabilities active."
                            },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Configuration items list
        configItems.forEach { item ->
            val isVerified = verifiedStates[item.id] == true
            val currentStatusLabel = if (isVerified) item.activeLabel else item.inactiveLabel

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isVerified) DarkSurface.copy(alpha = 0.95f) else DarkSurface
                ),
                border = if (isVerified) BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.45f)) else BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isVerified) StatusSuccess.copy(alpha = 0.2f) else DarkSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isVerified) Icons.Default.CheckCircle else item.icon,
                                    contentDescription = item.title,
                                    tint = if (isVerified) StatusSuccess else ElectricViolet,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Status line: e.g. "✓ Notification Access — Allowed" or "⚠ Notification Access — Not Allowed"
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isVerified) "✓ ${item.title} — $currentStatusLabel" else "⚠ ${item.title} — $currentStatusLabel",
                                    color = if (isVerified) StatusSuccess else StatusWarning,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isVerified) {
                            FilledTonalButton(
                                onClick = {
                                    if (item.onDirectAction != null) {
                                        item.onDirectAction.invoke(context)
                                        refreshAllStates()
                                    } else {
                                        val intent = item.getActionIntent(context)
                                        if (intent != null) {
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = StatusSuccess.copy(alpha = 0.18f),
                                    contentColor = StatusSuccess
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(item.activeLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    if (item.id == "microphone") {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else if (item.onDirectAction != null) {
                                        item.onDirectAction.invoke(context)
                                        refreshAllStates()
                                    } else {
                                        val intent = item.getActionIntent(context)
                                        if (intent != null) {
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = DarkSurfaceVariant,
                                    contentColor = NeonCyan
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Configure", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CosmicDarkBackground.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Purpose: ${item.reason}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start EVA Button
        Button(
            onClick = {
                app.preferences.setOnboardingCompleted(true)
                try {
                    EvaVoiceService.startService(context)
                } catch (_: Exception) {}
                onFinish()
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_eva_button")
        ) {
            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Launch EVA Assistant", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
