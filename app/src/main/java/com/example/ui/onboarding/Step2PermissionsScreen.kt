package com.example.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.ui.theme.*

data class PermissionItem(
    val title: String,
    val description: String,
    val permissionManifest: String,
    val icon: ImageVector,
    val isCrucial: Boolean = false
)

@Composable
fun Step2PermissionsScreen(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val permissionList = remember {
        mutableListOf(
            PermissionItem("Microphone", "Required for EVA voice interaction & wake detection", Manifest.permission.RECORD_AUDIO, Icons.Default.Mic, isCrucial = true),
            PermissionItem("Camera", "Allows visual question answering and image inspection", Manifest.permission.CAMERA, Icons.Default.Videocam),
            PermissionItem("Phone", "Enables voice-activated phone calls and call assistance", Manifest.permission.CALL_PHONE, Icons.Default.Phone),
            PermissionItem("Contacts", "Allows calling and messaging friends by name", Manifest.permission.READ_CONTACTS, Icons.Default.Contacts),
            PermissionItem("SMS", "Allows reading and drafting SMS messages", Manifest.permission.SEND_SMS, Icons.Default.Sms),
            PermissionItem("Location", "Provides localized weather, maps, and nearby answers", Manifest.permission.ACCESS_FINE_LOCATION, Icons.Default.LocationOn),
            PermissionItem("Notifications", "Enables reminders, alerts, and background updates", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else Manifest.permission.INTERNET, Icons.Default.Notifications)
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(PermissionItem("Bluetooth", "Connect to wireless earbuds and car audio", Manifest.permission.BLUETOOTH_CONNECT, Icons.Default.Bluetooth))
            }
        }
    }

    var permissionStates by remember {
        mutableStateOf(
            permissionList.associate { item ->
                item.permissionManifest to checkPermission(context, item.permissionManifest)
            }
        )
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionStates = permissionList.associate { item ->
            item.permissionManifest to (results[item.permissionManifest] ?: checkPermission(context, item.permissionManifest))
        }
    }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionStates = permissionList.associate { item ->
            item.permissionManifest to checkPermission(context, item.permissionManifest)
        }
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
                text = "Step 2 of 3 • System Permissions",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Text(
            text = "Grant System Permissions",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "EVA needs device access to act as your complete assistant. Microphone is required for voice commands.",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action row: Allow All & Open Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    multiplePermissionLauncher.launch(
                        permissionList.map { it.permissionManifest }.toTypedArray()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("allow_all_permissions_button")
            ) {
                Icon(Icons.Default.SecurityUpdateGood, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Allow All", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Settings")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permission Items List
        permissionList.forEach { item ->
            val isAllowed = permissionStates[item.permissionManifest] ?: false

            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAllowed) StatusSuccess.copy(alpha = 0.2f) else DarkSurfaceVariant,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isAllowed) StatusSuccess else NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.title,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (item.isCrucial) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Required",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = item.description,
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isAllowed) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Allowed",
                                color = StatusSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        FilledTonalButton(
                            onClick = {
                                singlePermissionLauncher.launch(item.permissionManifest)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = NeonCyan
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Continue Button
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("continue_to_step3_button")
        ) {
            Text("Continue to Background Setup", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun checkPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
