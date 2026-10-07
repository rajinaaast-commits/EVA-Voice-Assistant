package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    val fastRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "FastRotation"
    )

    val primaryColors = when (state) {
        OrbState.IDLE -> listOf(NeonCyan.copy(alpha = 0.8f), ElectricViolet.copy(alpha = 0.8f), HologramBlue)
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
        OrbState.LISTENING, OrbState.SPEAKING -> 1f + (amplitude * 0.45f)
        OrbState.THINKING, OrbState.PROCESSING -> pulse
        OrbState.WAKE_DETECTED -> 1.25f
        else -> pulse
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .clickable(onClick = onClick)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 3.2f) * dynamicScale

                // 1. Outer ambient glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColors[0].copy(alpha = 0.35f),
                            primaryColors[1].copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.6f
                    ),
                    radius = baseRadius * 1.6f,
                    center = center
                )

                // 2. Orbital holographic ring 1
                val angleRad1 = Math.toRadians((if (state == OrbState.THINKING) fastRotation else rotation).toDouble())
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColors[0].copy(alpha = 0.8f),
                            Color.Transparent,
                            primaryColors[1].copy(alpha = 0.8f),
                            Color.Transparent,
                            primaryColors[0].copy(alpha = 0.8f)
                        ),
                        center = center
                    ),
                    radius = baseRadius * 1.3f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // 3. Orbital nodes
                for (i in 0 until 4) {
                    val nodeAngle = angleRad1 + (i * Math.PI / 2)
                    val nodeX = center.x + (baseRadius * 1.3f * cos(nodeAngle)).toFloat()
                    val nodeY = center.y + (baseRadius * 1.3f * sin(nodeAngle)).toFloat()
                    drawCircle(
                        color = primaryColors[i % primaryColors.size],
                        radius = 4.dp.toPx(),
                        center = Offset(nodeX, nodeY)
                    )
                }

                // 4. Secondary Counter-rotating ring
                val angleRad2 = -angleRad1 * 1.4
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColors[1].copy(alpha = 0.6f),
                            primaryColors[2].copy(alpha = 0.9f),
                            Color.Transparent,
                            primaryColors[1].copy(alpha = 0.6f)
                        ),
                        center = center
                    ),
                    radius = baseRadius * 1.15f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 5. Core Energy Orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            primaryColors[0],
                            primaryColors[1],
                            primaryColors[2].copy(alpha = 0.8f)
                        ),
                        center = center,
                        radius = baseRadius
                    ),
                    radius = baseRadius,
                    center = center
                )

                // 6. Audio Waveform Ripples during Listening / Speaking
                if (state == OrbState.LISTENING || state == OrbState.SPEAKING) {
                    val rippleCount = 3
                    for (r in 1..rippleCount) {
                        val rippleRadius = baseRadius + (r * 18.dp.toPx() * amplitude)
                        drawCircle(
                            color = primaryColors[0].copy(alpha = (0.6f / r)),
                            radius = rippleRadius,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stateText,
            color = primaryColors[0],
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}
