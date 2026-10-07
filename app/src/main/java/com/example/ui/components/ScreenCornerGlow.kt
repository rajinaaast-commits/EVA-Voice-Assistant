package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan

@Composable
fun ScreenCornerGlow(
    isActive: Boolean,
    orbState: OrbState = OrbState.IDLE,
    amplitude: Float = 0.2f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CornerGlowTransition")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CornerPulse"
    )

    val travelOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing)
        ),
        label = "ParticleTravel"
    )

    val isElevated = orbState == OrbState.LISTENING ||
            orbState == OrbState.WAKE_DETECTED ||
            orbState == OrbState.THINKING ||
            orbState == OrbState.SPEAKING ||
            isActive

    val currentAlpha = if (isElevated) {
        (0.6f + (pulse * 0.4f) + (amplitude * 0.2f)).coerceIn(0.4f, 1f)
    } else {
        (0.15f + (pulse * 0.15f))
    }

    val primaryColor = when (orbState) {
        OrbState.LISTENING -> Color(0xFF00FFB2)
        OrbState.WAKE_DETECTED -> Color(0xFF00FFFF)
        OrbState.THINKING, OrbState.PROCESSING -> ElectricViolet
        OrbState.SPEAKING -> NeonCyan
        OrbState.ERROR -> Color(0xFFFF5252)
        else -> NeonCyan
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cornerArm = 42.dp.toPx()
        val cornerDepth = 12.dp.toPx()
        val strokeWidth = (if (isElevated) 2.dp else 1.2.dp).toPx()

        val glowBrushTL = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = currentAlpha * 0.7f), Color.Transparent),
            center = Offset(0f, 0f),
            radius = cornerArm * 1.6f
        )
        val glowBrushTR = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = currentAlpha * 0.7f), Color.Transparent),
            center = Offset(w, 0f),
            radius = cornerArm * 1.6f
        )
        val glowBrushBL = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = currentAlpha * 0.7f), Color.Transparent),
            center = Offset(0f, h),
            radius = cornerArm * 1.6f
        )
        val glowBrushBR = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = currentAlpha * 0.7f), Color.Transparent),
            center = Offset(w, h),
            radius = cornerArm * 1.6f
        )

        // Draw soft ambient corner glow
        drawCircle(brush = glowBrushTL, radius = cornerArm * 1.4f, center = Offset(0f, 0f))
        drawCircle(brush = glowBrushTR, radius = cornerArm * 1.4f, center = Offset(w, 0f))
        drawCircle(brush = glowBrushBL, radius = cornerArm * 1.4f, center = Offset(0f, h))
        drawCircle(brush = glowBrushBR, radius = cornerArm * 1.4f, center = Offset(w, h))

        val stroke = Stroke(width = strokeWidth)

        // Top-Left Corner Bracket
        val pathTL = Path().apply {
            moveTo(0f, cornerArm)
            lineTo(0f, cornerDepth)
            cubicTo(0f, 0f, 0f, 0f, cornerDepth, 0f)
            lineTo(cornerArm, 0f)
        }
        drawPath(pathTL, color = primaryColor.copy(alpha = currentAlpha), style = stroke)

        // Top-Right Corner Bracket
        val pathTR = Path().apply {
            moveTo(w, cornerArm)
            lineTo(w, cornerDepth)
            cubicTo(w, 0f, w, 0f, w - cornerDepth, 0f)
            lineTo(w - cornerArm, 0f)
        }
        drawPath(pathTR, color = primaryColor.copy(alpha = currentAlpha), style = stroke)

        // Bottom-Left Corner Bracket
        val pathBL = Path().apply {
            moveTo(0f, h - cornerArm)
            lineTo(0f, h - cornerDepth)
            cubicTo(0f, h, 0f, h, cornerDepth, h)
            lineTo(cornerArm, h)
        }
        drawPath(pathBL, color = primaryColor.copy(alpha = currentAlpha), style = stroke)

        // Bottom-Right Corner Bracket
        val pathBR = Path().apply {
            moveTo(w, h - cornerArm)
            lineTo(w, h - cornerDepth)
            cubicTo(w, h, w, h, w - cornerDepth, h)
            lineTo(w - cornerArm, h)
        }
        drawPath(pathBR, color = primaryColor.copy(alpha = currentAlpha), style = stroke)

        // Animated Corner Quantum Particles
        if (isElevated) {
            val pRadius = 2.dp.toPx()
            val particleAlpha = (currentAlpha * 0.9f).coerceIn(0f, 1f)

            // Particle TL
            val ptTL = Offset(cornerArm * travelOffset, (1f - travelOffset) * cornerArm * 0.2f)
            drawCircle(color = Color.White.copy(alpha = particleAlpha), radius = pRadius, center = ptTL)

            // Particle TR
            val ptTR = Offset(w - cornerArm * travelOffset, (1f - travelOffset) * cornerArm * 0.2f)
            drawCircle(color = Color.White.copy(alpha = particleAlpha), radius = pRadius, center = ptTR)

            // Particle BL
            val ptBL = Offset(cornerArm * travelOffset, h - (1f - travelOffset) * cornerArm * 0.2f)
            drawCircle(color = Color.White.copy(alpha = particleAlpha), radius = pRadius, center = ptBL)

            // Particle BR
            val ptBR = Offset(w - cornerArm * travelOffset, h - (1f - travelOffset) * cornerArm * 0.2f)
            drawCircle(color = Color.White.copy(alpha = particleAlpha), radius = pRadius, center = ptBR)
        }
    }
}
