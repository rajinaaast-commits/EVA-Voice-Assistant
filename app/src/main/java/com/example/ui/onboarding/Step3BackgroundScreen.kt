package com.example.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.EvaApplication
import com.example.service.EVAAccessibilityService
import com.example.service.EvaVoiceService
import com.example.ui.theme.*

data class BackgroundConfigItem(
    val id: String,
    val title: String,
    val description: String,
    val reason: String,
    val icon: ImageVector,
    val isVerified: (Context) -> Boolean,
    val getActionIntent: (Context) -> Intent
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
                id = "battery",
                title = "Battery Optimization",
                description = "Unrestricted background battery execution",
                reason = "Prevents Android OS from killing EVA's background wake detection when device is in deep sleep.",
                icon = Icons.Default.BatteryChargingFull,
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
                description = "Voice Orb overlay above any application",
                reason = "Allows EVA's Voice Orb to appear over games, videos, or web browsers when you say 'Hey EVA'.",
                icon = Icons.Default.Layers,
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
                id = "assistant",
                title = "Default Digital Assistant",
                description = "Set EVA as primary phone assistant",
                reason = "Enables holding the home button or power button to instantly invoke EVA.",
                icon = Icons.Default.Assistant,
                isVerified = { ctx ->
                    val assist = Settings.Secure.getString(ctx.contentResolver, "voice_interaction_service")
                        ?: Settings.Secure.getString(ctx.contentResolver, "assistant")
                    assist?.contains(ctx.packageName) == true
                },
                getActionIntent = { _ ->
                    Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                }
            ),
            BackgroundConfigItem(
                id = "notification",
                title = "Notification Access",
                description = "Read WhatsApp & system alerts",
                reason = "Allows EVA to announce incoming calls, WhatsApp messages, and auto-reply during driving mode.",
                icon = Icons.Default.NotificationsActive,
                isVerified = { ctx ->
                    val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
                    flat?.contains(ctx.packageName) == true
                },
                getActionIntent = { _ ->
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                }
            ),
            BackgroundConfigItem(
                id = "accessibility",
                title = "Accessibility Service",
                description = "Automate device workflows & actions",
                reason = "Empowers EVA to perform automation commands, open apps, and navigate on your behalf.",
                icon = Icons.Default.AccessibilityNew,
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

    // Refresh permissions automatically when returning from Settings via Lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                verifiedStates = configItems.associate { it.id to it.isVerified(context) }
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
                text = "Step 3 of 3 • Background & Assistant",
                color = ElectricViolet,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Text(
            text = "Background Assistant Setup",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Configure system capabilities so EVA can wake up with 'Hey EVA', announce alerts, and assist you anywhere.",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Prominent Verified Banner (matches Step 2 verification styling)
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
                            text = "Permission Verified",
                            color = StatusSuccess,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (verifiedCount == totalCount) {
                                "All background capabilities verified and active!"
                            } else {
                                "$verifiedCount of $totalCount background permissions verified and active."
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

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isVerified) DarkSurface.copy(alpha = 0.95f) else DarkSurface
                ),
                border = if (isVerified) BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.45f)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.title,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StatusSuccess.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Verified",
                                            color = StatusSuccess,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
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
                                    try {
                                        context.startActivity(item.getActionIntent(context))
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
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
                                Text("Verified", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    try {
                                        context.startActivity(item.getActionIntent(context))
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
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
                            text = "Why needed: ${item.reason}",
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
            Text("Start EVA", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
