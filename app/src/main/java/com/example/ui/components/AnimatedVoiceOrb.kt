package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE,
    LISTENING,
    WAKE_DETECTED,
    PROCESSING,
    THINKING,
    SPEAKING,
    ERROR,
    OFFLINE
}

@Composable
fun AnimatedVoiceOrb(
    state: OrbState,
    amplitude: Float = 0.2f, // 0.0f to 1.0f
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbInfinite")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    val fastRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = "FastRotation"
    )

    val primaryColors = when (state) {
        OrbState.IDLE -> listOf(NeonCyan.copy(alpha = 0.85f), ElectricViolet.copy(alpha = 0.85f), HologramBlue)
        OrbState.LISTENING -> listOf(NeonCyan, Color(0xFF00FFB2), Color(0xFF76FF03))
        OrbState.WAKE_DETECTED -> listOf(Color(0xFF00FFFF), Color(0xFFE040FB), Color(0xFF00E5FF))
        OrbState.PROCESSING, OrbState.THINKING -> listOf(ElectricViolet, CyberPink, NeonCyan)
        OrbState.SPEAKING -> listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFF4081))
        OrbState.ERROR -> listOf(StatusError, Color(0xFFFF1744), Color(0xFFFF9100))
        OrbState.OFFLINE -> listOf(TextMuted, Color(0xFF37474F), Color(0xFF263238))
    }

    val stateText = when (state) {
        OrbState.IDLE -> "TAP TO TALK"
        OrbState.LISTENING -> "LISTENING..."
        OrbState.WAKE_DETECTED -> "WAKE DETECTED"
        OrbState.PROCESSING -> "PROCESSING..."
        OrbState.THINKING -> "THINKING..."
        OrbState.SPEAKING -> "SPEAKING..."
        OrbState.ERROR -> "SYSTEM ALERT"
        OrbState.OFFLINE -> "OFFLINE"
    }

    val dynamicScale = when (state) {
        OrbState.LISTENING, OrbState.SPEAKING -> 1f + (amplitude.coerceIn(0f, 1f) * 0.35f)
        OrbState.THINKING, OrbState.PROCESSING -> pulse
        OrbState.WAKE_DETECTED -> 1.2f
        else -> pulse
    }

    val coreSize = (140 * dynamicScale).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(260.dp)
                .clickable(onClick = onClick)
        ) {
            // Background Canvas: Ambient glow, holographic rings, orbital particles & waves
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 3.4f) * dynamicScale

                // 1. Ambient outer aura glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColors[0].copy(alpha = 0.45f),
                            primaryColors[1].copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.7f
                    ),
                    radius = baseRadius * 1.7f,
                    center = center
                )

                // 2. Primary holographic orbital ring
                val activeRotationAngle = if (state == OrbState.THINKING) fastRotation else rotation
                val angleRad1 = Math.toRadians(activeRotationAngle.toDouble())
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColors[0].copy(alpha = 0.85f),
                            Color.Transparent,
                            primaryColors[1].copy(alpha = 0.85f),
                            Color.Transparent,
                            primaryColors[0].copy(alpha = 0.85f)
                        ),
                        center = center
                    ),
                    radius = baseRadius * 1.35f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // 3. Orbital light nodes
                for (i in 0 until 4) {
                    val nodeAngle = angleRad1 + (i * Math.PI / 2)
                    val nodeX = center.x + (baseRadius * 1.35f * cos(nodeAngle)).toFloat()
                    val nodeY = center.y + (baseRadius * 1.35f * sin(nodeAngle)).toFloat()
                    drawCircle(
                        color = primaryColors[i % primaryColors.size],
                        radius = 4.5.dp.toPx(),
                        center = Offset(nodeX, nodeY)
                    )
                }

                // 4. Counter-rotating secondary ring
                val angleRad2 = -angleRad1 * 1.3
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColors[1].copy(alpha = 0.7f),
                            primaryColors[2].copy(alpha = 0.9f),
                            Color.Transparent,
                            primaryColors[1].copy(alpha = 0.7f)
                        ),
                        center = center
                    ),
                    radius = baseRadius * 1.18f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 5. Audio Waveform Ripples during Listening / Speaking
                if (state == OrbState.LISTENING || state == OrbState.SPEAKING) {
                    val rippleCount = 3
                    for (r in 1..rippleCount) {
                        val rippleRadius = baseRadius + (r * 18.dp.toPx() * amplitude.coerceIn(0f, 1f))
                        drawCircle(
                            color = primaryColors[0].copy(alpha = (0.65f / r)),
                            radius = rippleRadius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Core Voice Orb: User's EVA holographic art layered with glowing boundary
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(coreSize)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(
                            2.5.dp,
                            Brush.sweepGradient(
                                listOf(
                                    primaryColors[0],
                                    primaryColors[1],
                                    primaryColors[2],
                                    primaryColors[0]
                                )
                            )
                        ),
                        CircleShape
                    )
            ) {
                // Uploaded holographic EVA sphere artwork
                Image(
                    painter = painterResource(id = R.drawable.ic_eva_logo_1791388278820),
                    contentDescription = "EVA AI Voice Core",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )

                // Dynamic holographic state tint overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    primaryColors[0].copy(alpha = if (state == OrbState.IDLE) 0.15f else 0.35f),
                                    primaryColors[1].copy(alpha = if (state == OrbState.IDLE) 0.25f else 0.50f)
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // State indicator label
        Text(
            text = stateText,
            color = primaryColors[0],
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}
