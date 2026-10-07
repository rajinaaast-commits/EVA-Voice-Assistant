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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.HologramBlue
import com.example.ui.theme.NeonCyan

/**
 * Subtle futuristic corner animation for EVA AI:
 * Thin neon cyan/blue glowing lines and particles moving smoothly around
 * the four screen corners, with a soft pulse when EVA is listening, thinking, or speaking.
 * Minimal, premium, battery-efficient, and unobtrusive to UI interactions.
 */
@Composable
fun ScreenCornerGlow(
    isActive: Boolean = false,
    orbState: OrbState = OrbState.IDLE,
    amplitude: Float = 0.1f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CornerGlowTransition")

    // Smooth subtle ambient pulse (slower when idle, rhythmic when active)
    val pulseDuration = if (isActive || orbState != OrbState.IDLE) 1600 else 3200
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CornerPulse"
    )

    // Smooth particle flow along the corner lines
    val travelOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing)
        ),
        label = "ParticleTravel"
    )

    // Secondary offset for trailing particle
    val secondaryTravelOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing)
        ),
        label = "SecondaryParticleTravel"
    )

    val isElevated = orbState == OrbState.LISTENING ||
            orbState == OrbState.WAKE_DETECTED ||
            orbState == OrbState.THINKING ||
            orbState == OrbState.PROCESSING ||
            orbState == OrbState.SPEAKING ||
            isActive

    // Adaptive alpha: soft & subtle when idle, gracefully glowing when elevated
    val baseAlpha = if (isElevated) {
        (0.55f + (pulse * 0.35f) + (amplitude * 0.15f)).coerceIn(0.4f, 1f)
    } else {
        (0.22f + (pulse * 0.18f)).coerceIn(0.15f, 0.45f)
    }

    val primaryColor = when (orbState) {
        OrbState.LISTENING -> Color(0xFF00FFB2)      // Bright emerald cyan
        OrbState.WAKE_DETECTED -> Color(0xFF00FFFF)   // Electric cyan
        OrbState.THINKING, OrbState.PROCESSING -> ElectricViolet // Futuristic violet
        OrbState.SPEAKING -> NeonCyan                 // Radiant cyan
        OrbState.ERROR -> Color(0xFFFF5252)          // Soft warning red
        else -> NeonCyan                             // Default neon cyan
    }

    val secondaryColor = when (orbState) {
        OrbState.THINKING, OrbState.PROCESSING -> NeonCyan
        OrbState.LISTENING -> HologramBlue
        else -> HologramBlue
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("corner_glow_animation")
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val cornerArm = 44.dp.toPx()
        val cornerDepth = 14.dp.toPx()
        val strokeWidth = (if (isElevated) 2.dp else 1.2.dp).toPx()

        // Radial ambient glows at all four corners
        val glowRadius = cornerArm * (if (isElevated) 1.8f else 1.3f)
        val glowAlpha = (baseAlpha * (if (isElevated) 0.55f else 0.25f)).coerceIn(0f, 1f)

        val glowBrushTL = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = glowAlpha), Color.Transparent),
            center = Offset(0f, 0f),
            radius = glowRadius
        )
        val glowBrushTR = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = glowAlpha), Color.Transparent),
            center = Offset(w, 0f),
            radius = glowRadius
        )
        val glowBrushBL = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = glowAlpha), Color.Transparent),
            center = Offset(0f, h),
            radius = glowRadius
        )
        val glowBrushBR = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = glowAlpha), Color.Transparent),
            center = Offset(w, h),
            radius = glowRadius
        )

        drawCircle(brush = glowBrushTL, radius = glowRadius, center = Offset(0f, 0f))
        drawCircle(brush = glowBrushTR, radius = glowRadius, center = Offset(w, 0f))
        drawCircle(brush = glowBrushBL, radius = glowRadius, center = Offset(0f, h))
        drawCircle(brush = glowBrushBR, radius = glowRadius, center = Offset(w, h))

        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        // 1. Top-Left Corner Bracket
        val pathTL = Path().apply {
            moveTo(0f, cornerArm)
            lineTo(0f, cornerDepth)
            cubicTo(0f, 0f, 0f, 0f, cornerDepth, 0f)
            lineTo(cornerArm, 0f)
        }
        val brushTL = Brush.linearGradient(
            colors = listOf(primaryColor.copy(alpha = baseAlpha), secondaryColor.copy(alpha = baseAlpha * 0.8f)),
            start = Offset(0f, cornerArm),
            end = Offset(cornerArm, 0f)
        )
        drawPath(pathTL, brush = brushTL, style = stroke)

        // 2. Top-Right Corner Bracket
        val pathTR = Path().apply {
            moveTo(w, cornerArm)
            lineTo(w, cornerDepth)
            cubicTo(w, 0f, w, 0f, w - cornerDepth, 0f)
            lineTo(w - cornerArm, 0f)
        }
        val brushTR = Brush.linearGradient(
            colors = listOf(primaryColor.copy(alpha = baseAlpha), secondaryColor.copy(alpha = baseAlpha * 0.8f)),
            start = Offset(w, cornerArm),
            end = Offset(w - cornerArm, 0f)
        )
        drawPath(pathTR, brush = brushTR, style = stroke)

        // 3. Bottom-Left Corner Bracket
        val pathBL = Path().apply {
            moveTo(0f, h - cornerArm)
            lineTo(0f, h - cornerDepth)
            cubicTo(0f, h, 0f, h, cornerDepth, h)
            lineTo(cornerArm, h)
        }
        val brushBL = Brush.linearGradient(
            colors = listOf(primaryColor.copy(alpha = baseAlpha), secondaryColor.copy(alpha = baseAlpha * 0.8f)),
            start = Offset(0f, h - cornerArm),
            end = Offset(cornerArm, h)
        )
        drawPath(pathBL, brush = brushBL, style = stroke)

        // 4. Bottom-Right Corner Bracket
        val pathBR = Path().apply {
            moveTo(w, h - cornerArm)
            lineTo(w, h - cornerDepth)
            cubicTo(w, h, w, h, w - cornerDepth, h)
            lineTo(w - cornerArm, h)
        }
        val brushBR = Brush.linearGradient(
            colors = listOf(primaryColor.copy(alpha = baseAlpha), secondaryColor.copy(alpha = baseAlpha * 0.8f)),
            start = Offset(w, h - cornerArm),
            end = Offset(w - cornerArm, h)
        )
        drawPath(pathBR, brush = brushBR, style = stroke)

        // Subtle moving neon quantum particles gliding along the four corners
        val pRadius = (if (isElevated) 2.2.dp else 1.6.dp).toPx()
        val particleAlpha = (baseAlpha * (if (isElevated) 0.95f else 0.7f)).coerceIn(0f, 1f)

        // Helper to interpolate point along L-bracket from vertical arm -> corner -> horizontal arm
        fun calcBracketPoint(
            t: Float,
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            cornerX: Float,
            cornerY: Float
        ): Offset {
            return if (t < 0.5f) {
                val subT = t * 2f
                Offset(
                    startX + (cornerX - startX) * subT,
                    startY + (cornerY - startY) * subT
                )
            } else {
                val subT = (t - 0.5f) * 2f
                Offset(
                    cornerX + (endX - cornerX) * subT,
                    cornerY + (endY - cornerY) * subT
                )
            }
        }

        // Particle TL
        val ptTL = calcBracketPoint(travelOffset, 0f, cornerArm, cornerArm, 0f, 0f, 0f)
        drawCircle(color = Color.White.copy(alpha = particleAlpha), radius = pRadius, center = ptTL)

        // Particle TR
        val ptTR = calcBracketPoint(travelOffset, w, cornerArm, w - cornerArm, 0f, w, 0f)
        drawCircle(color = Color.White.copy(alpha = particleAlpha), radius = pRadius, center = ptTR)

        // Particle BL
        val ptBL = calcBracketPoint(travelOffset, 0f, h - cornerArm, cornerArm, h, 0f, h)
        drawCircle(color = Color.White.copy(alpha = particleAlpha), radius = pRadius, center = ptBL)

        // Particle BR
        val ptBR = calcBracketPoint(travelOffset, w, h - cornerArm, w - cornerArm, h, w, h)
        drawCircle(color = Color.White.copy(alpha = particleAlpha), radius = pRadius, center = ptBR)

        // If elevated (listening/thinking/speaking), add secondary delayed particle for streaming trail
        if (isElevated) {
            val secRadius = 1.4.dp.toPx()
            val secAlpha = (particleAlpha * 0.7f).coerceIn(0f, 1f)

            val ptTL2 = calcBracketPoint(secondaryTravelOffset, cornerArm, 0f, 0f, cornerArm, 0f, 0f)
            drawCircle(color = primaryColor.copy(alpha = secAlpha), radius = secRadius, center = ptTL2)

            val ptTR2 = calcBracketPoint(secondaryTravelOffset, w - cornerArm, 0f, w, cornerArm, w, 0f)
            drawCircle(color = primaryColor.copy(alpha = secAlpha), radius = secRadius, center = ptTR2)

            val ptBL2 = calcBracketPoint(secondaryTravelOffset, cornerArm, h, 0f, h - cornerArm, 0f, h)
            drawCircle(color = primaryColor.copy(alpha = secAlpha), radius = secRadius, center = ptBL2)

            val ptBR2 = calcBracketPoint(secondaryTravelOffset, w - cornerArm, h, w, h - cornerArm, w, h)
            drawCircle(color = primaryColor.copy(alpha = secAlpha), radius = secRadius, center = ptBR2)
        }
    }
}
