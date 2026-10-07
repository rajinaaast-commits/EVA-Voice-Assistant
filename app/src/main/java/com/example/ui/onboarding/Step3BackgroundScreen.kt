package com.example.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import com.example.EvaApplication
import com.example.service.EvaVoiceService
import com.example.ui.theme.*

data class BackgroundConfigItem(
    val title: String,
    val description: String,
    val reason: String,
    val icon: ImageVector,
    val actionIntent: Intent
)

@Composable
fun Step3BackgroundScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val scrollState = rememberScrollState()

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isBatteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } else true

    val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true

    val configItems = remember {
        listOf(
            BackgroundConfigItem(
                title = "Battery Optimization",
                description = "Unrestricted background battery execution",
                reason = "Prevents Android OS from killing EVA's background wake detection when device is in deep sleep.",
                icon = Icons.Default.BatteryChargingFull,
                actionIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            ),
            BackgroundConfigItem(
                title = "Display Over Other Apps",
                description = "Voice Orb overlay above any application",
                reason = "Allows EVA's Voice Orb to appear over games, videos, or web browsers when you say 'Hey EVA'.",
                icon = Icons.Default.Layers,
                actionIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                } else Intent(Settings.ACTION_SETTINGS)
            ),
            BackgroundConfigItem(
                title = "Default Digital Assistant",
                description = "Set EVA as primary phone assistant",
                reason = "Enables holding the home button or power button to instantly invoke EVA.",
                icon = Icons.Default.Assistant,
                actionIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            ),
            BackgroundConfigItem(
                title = "Notification Access",
                description = "Read WhatsApp & system alerts",
                reason = "Allows EVA to announce incoming calls, WhatsApp messages, and auto-reply during driving mode.",
                icon = Icons.Default.NotificationsActive,
                actionIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            ),
            BackgroundConfigItem(
                title = "Accessibility Service",
                description = "Automate device workflows & actions",
                reason = "Empowers EVA to perform automation commands, open apps, and navigate on your behalf.",
                icon = Icons.Default.AccessibilityNew,
                actionIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        )
    }

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

        configItems.forEach { item ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = item.description,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalButton(
                            onClick = {
                                try {
                                    context.startActivity(item.actionIntent)
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
