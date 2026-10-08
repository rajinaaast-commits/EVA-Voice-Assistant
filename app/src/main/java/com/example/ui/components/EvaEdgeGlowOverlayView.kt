package com.example.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.data.preferences.EvaPreferences
import com.example.voice.EvaState
import com.example.voice.EvaStateManager
import kotlin.math.*

/**
 * Native, high-performance Android View for system-level EVA Edge Glow overlay.
 * Renders glowing neon lines, flowing quantum particles, and responsive audio waves
 * around all four corners of the device screen without consuming excessive battery.
 */
class EvaEdgeGlowOverlayView(context: Context) : View(context), Choreographer.FrameCallback {

    private val prefs = EvaPreferences(context)
    private var isAnimating = false
    private var lastFrameTimeNanos = 0L

    // Current EVA state
    var currentState: EvaState = EvaState.IDLE
        set(value) {
            if (field != value) {
                val oldState = field
                field = value
                if (value == EvaState.WAKE) {
                    triggerWakeAnimation()
                }
                lastStateChangeTime = SystemClock.uptimeMillis()
                invalidate()
            }
        }

    var currentAmplitude: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    // Animation progress registers
    private var pulsePhase = 0f
    private var particleOffset = 0f
    private var secondaryParticleOffset = 0f
    private var thinkingRotation = 0f
    private var wakeRippleProgress = 1f
    private var lastStateChangeTime = 0L

    // Paint and shader objects
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val pathTL = Path()
    private val pathTR = Path()
    private val pathBL = Path()
    private val pathBR = Path()

    init {
        setWillNotDraw(false)
    }

    fun startAnimation() {
        if (!isAnimating) {
            isAnimating = true
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun stopAnimation() {
        if (isAnimating) {
            isAnimating = false
            Choreographer.getInstance().removeFrameCallback(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    private fun triggerWakeAnimation() {
        if (!prefs.isWakeAnimationEnabled()) return
        wakeRippleProgress = 0f
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                wakeRippleProgress = it.animatedValue as Float
                invalidate()
            }
        }
        animator.start()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isAnimating) return

        val dt = if (lastFrameTimeNanos > 0L) {
            ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
        } else {
            0.016f
        }
        lastFrameTimeNanos = frameTimeNanos

        // Update animation timers based on state and user preferences
        val speedMultiplier = when (currentState) {
            EvaState.WAKE -> 3.0f
            EvaState.LISTENING -> 1.8f
            EvaState.THINKING -> 2.2f
            EvaState.SPEAKING -> 2.0f
            EvaState.ERROR -> 1.0f
            else -> if (prefs.isEdgeGlowBatterySaver()) 0.5f else 1.0f
        }

        pulsePhase = (pulsePhase + dt * speedMultiplier * 1.5f) % (2f * Math.PI.toFloat())
        particleOffset = (particleOffset + dt * speedMultiplier * 0.4f) % 1f
        secondaryParticleOffset = (secondaryParticleOffset + dt * speedMultiplier * 0.28f) % 1f
        thinkingRotation = (thinkingRotation + dt * 140f) % 360f

        invalidate()

        if (isAnimating) {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!prefs.isEdgeGlowEnabled()) return

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        val userIntensity = prefs.getEdgeGlowIntensity()

        val cornerArm = 48f * density
        val cornerDepth = 16f * density

        // Pulse factor between 0.35 and 1.0
        val sinPulse = (sin(pulsePhase.toDouble()).toFloat() + 1f) / 2f
        val pulseFactor = 0.4f + sinPulse * 0.6f

        // State-specific color & alpha
        val (primaryColorInt, secondaryColorInt) = when (currentState) {
            EvaState.LISTENING -> {
                if (!prefs.isListeningAnimationEnabled()) return
                Color.rgb(0, 255, 178) to Color.rgb(0, 229, 255) // Emerald Cyan & Neon Cyan
            }
            EvaState.WAKE -> {
                if (!prefs.isWakeAnimationEnabled()) return
                Color.rgb(0, 255, 255) to Color.rgb(224, 64, 251) // Electric Cyan & Cyber Violet
            }
            EvaState.THINKING -> {
                if (!prefs.isThinkingAnimationEnabled()) return
                Color.rgb(124, 77, 255) to Color.rgb(0, 229, 255) // Electric Violet & Cyan
            }
            EvaState.SPEAKING -> {
                if (!prefs.isSpeakingAnimationEnabled()) return
                Color.rgb(0, 229, 255) to Color.rgb(124, 77, 255) // Cyan & Violet
            }
            EvaState.ERROR -> {
                Color.rgb(255, 82, 82) to Color.rgb(255, 23, 68) // Warning Red
            }
            EvaState.OFFLINE -> {
                Color.rgb(69, 90, 100) to Color.rgb(38, 50, 56) // Muted Slate
            }
            EvaState.IDLE -> {
                if (!prefs.isIdleAnimationEnabled()) return
                Color.rgb(0, 229, 255) to Color.rgb(41, 121, 255) // Hologram Cyan / Blue
            }
        }

        val isElevated = currentState != EvaState.IDLE && currentState != EvaState.OFFLINE

        val baseAlpha = when {
            currentState == EvaState.LISTENING || currentState == EvaState.SPEAKING -> {
                (0.55f + pulseFactor * 0.30f + currentAmplitude * 0.20f) * userIntensity
            }
            currentState == EvaState.WAKE -> {
                (0.70f + (1f - wakeRippleProgress) * 0.30f) * userIntensity
            }
            currentState == EvaState.THINKING -> {
                (0.60f + pulseFactor * 0.35f) * userIntensity
            }
            currentState == EvaState.ERROR -> {
                (0.50f + pulseFactor * 0.30f) * userIntensity
            }
            else -> {
                // IDLE: very subtle, battery-efficient breathing
                (0.20f + pulseFactor * 0.15f) * userIntensity
            }
        }.coerceIn(0.12f, 1f)

        val strokeWidth = (if (isElevated) 2.4f else 1.4f) * density * (0.8f + currentAmplitude * 0.4f)
        bracketPaint.strokeWidth = strokeWidth

        // 1. RADIAL CORNER AMBIENT GLOWS
        val glowRadius = cornerArm * (if (isElevated) 2.2f else 1.4f) * (1f + currentAmplitude * 0.25f)
        val glowAlpha = (baseAlpha * (if (isElevated) 0.55f else 0.25f) * 255f).toInt().coerceIn(0, 255)

        drawCornerAmbientGlow(canvas, 0f, 0f, glowRadius, primaryColorInt, glowAlpha)
        drawCornerAmbientGlow(canvas, w, 0f, glowRadius, primaryColorInt, glowAlpha)
        drawCornerAmbientGlow(canvas, 0f, h, glowRadius, primaryColorInt, glowAlpha)
        drawCornerAmbientGlow(canvas, w, h, glowRadius, primaryColorInt, glowAlpha)

        // 2. CORNER BRACKETS (PATH DRAWING)
        // Top-Left
        pathTL.reset()
        pathTL.moveTo(0f, cornerArm)
        pathTL.lineTo(0f, cornerDepth)
        pathTL.quadTo(0f, 0f, cornerDepth, 0f)
        pathTL.lineTo(cornerArm, 0f)

        bracketPaint.shader = LinearGradient(
            0f, cornerArm, cornerArm, 0f,
            adjustAlpha(primaryColorInt, baseAlpha),
            adjustAlpha(secondaryColorInt, baseAlpha * 0.85f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(pathTL, bracketPaint)

        // Top-Right
        pathTR.reset()
        pathTR.moveTo(w, cornerArm)
        pathTR.lineTo(w, cornerDepth)
        pathTR.quadTo(w, 0f, w - cornerDepth, 0f)
        pathTR.lineTo(w - cornerArm, 0f)

        bracketPaint.shader = LinearGradient(
            w, cornerArm, w - cornerArm, 0f,
            adjustAlpha(primaryColorInt, baseAlpha),
            adjustAlpha(secondaryColorInt, baseAlpha * 0.85f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(pathTR, bracketPaint)

        // Bottom-Left
        pathBL.reset()
        pathBL.moveTo(0f, h - cornerArm)
        pathBL.lineTo(0f, h - cornerDepth)
        pathBL.quadTo(0f, h, cornerDepth, h)
        pathBL.lineTo(cornerArm, h)

        bracketPaint.shader = LinearGradient(
            0f, h - cornerArm, cornerArm, h,
            adjustAlpha(primaryColorInt, baseAlpha),
            adjustAlpha(secondaryColorInt, baseAlpha * 0.85f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(pathBL, bracketPaint)

        // Bottom-Right
        pathBR.reset()
        pathBR.moveTo(w, h - cornerArm)
        pathBR.lineTo(w, h - cornerDepth)
        pathBR.quadTo(w, h, w - cornerDepth, h)
        pathBR.lineTo(w - cornerArm, h)

        bracketPaint.shader = LinearGradient(
            w, h - cornerArm, w - cornerArm, h,
            adjustAlpha(primaryColorInt, baseAlpha),
            adjustAlpha(secondaryColorInt, baseAlpha * 0.85f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(pathBR, bracketPaint)

        // 3. THINKING ORBITAL FLOWING ARCS
        if (currentState == EvaState.THINKING && prefs.isThinkingAnimationEnabled()) {
            drawThinkingArcs(canvas, 0f, 0f, cornerArm * 0.8f, thinkingRotation, primaryColorInt, secondaryColorInt, density)
            drawThinkingArcs(canvas, w, 0f, cornerArm * 0.8f, -thinkingRotation, primaryColorInt, secondaryColorInt, density)
            drawThinkingArcs(canvas, 0f, h, cornerArm * 0.8f, -thinkingRotation, primaryColorInt, secondaryColorInt, density)
            drawThinkingArcs(canvas, w, h, cornerArm * 0.8f, thinkingRotation, primaryColorInt, secondaryColorInt, density)
        }

        // 4. FLOWING QUANTUM PARTICLES ALONG CORNERS
        if (!prefs.isEdgeGlowBatterySaver() || currentState != EvaState.IDLE) {
            val particleAlpha = (baseAlpha * 0.95f * 255f).toInt().coerceIn(0, 255)
            val pRadius = (2.2f + currentAmplitude * 1.5f) * density
            particlePaint.color = primaryColorInt
            particlePaint.alpha = particleAlpha

            // Compute positions along corner brackets for each corner
            drawCornerParticles(canvas, 0f, 0f, 1f, 1f, cornerArm, cornerDepth, pRadius)
            drawCornerParticles(canvas, w, 0f, -1f, 1f, cornerArm, cornerDepth, pRadius)
            drawCornerParticles(canvas, 0f, h, 1f, -1f, cornerArm, cornerDepth, pRadius)
            drawCornerParticles(canvas, w, h, -1f, -1f, cornerArm, cornerDepth, pRadius)
        }

        // 5. WAKE RIPPLE / EDGE WAVE EFFECT
        if (wakeRippleProgress < 1f && prefs.isWakeAnimationEnabled()) {
            val rippleDist = (wakeRippleProgress * cornerArm * 3f)
            val rippleAlpha = ((1f - wakeRippleProgress) * 255f).toInt().coerceIn(0, 255)
            wavePaint.strokeWidth = 3f * density
            wavePaint.color = Color.rgb(0, 255, 255)
            wavePaint.alpha = rippleAlpha

            // Concentric expansion from 4 corners
            canvas.drawCircle(0f, 0f, rippleDist, wavePaint)
            canvas.drawCircle(w, 0f, rippleDist, wavePaint)
            canvas.drawCircle(0f, h, rippleDist, wavePaint)
            canvas.drawCircle(w, h, rippleDist, wavePaint)
        }

        // 6. SPEAKING AUDIO SYNCHRONIZED WAVE
        if (currentState == EvaState.SPEAKING && prefs.isSpeakingAnimationEnabled() && currentAmplitude > 0.05f) {
            val waveAlpha = (currentAmplitude * 180f).toInt().coerceIn(0, 255)
            wavePaint.strokeWidth = (1.5f + currentAmplitude * 2.5f) * density
            wavePaint.color = primaryColorInt
            wavePaint.alpha = waveAlpha

            val waveRadius = cornerArm * (1.1f + currentAmplitude * 0.5f)
            canvas.drawCircle(0f, 0f, waveRadius, wavePaint)
            canvas.drawCircle(w, 0f, waveRadius, wavePaint)
            canvas.drawCircle(0f, h, waveRadius, wavePaint)
            canvas.drawCircle(w, h, waveRadius, wavePaint)
        }
    }

    private fun drawCornerAmbientGlow(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        colorInt: Int,
        alpha: Int
    ) {
        glowPaint.shader = RadialGradient(
            cx, cy, radius,
            adjustAlpha(colorInt, alpha / 255f),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, glowPaint)
    }

    private fun drawThinkingArcs(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        rotationDeg: Float,
        primaryColor: Int,
        secondaryColor: Int,
        density: Float
    ) {
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            strokeCap = Paint.Cap.ROUND
            color = primaryColor
            alpha = 200
        }
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(oval, rotationDeg, 45f, false, arcPaint)
        arcPaint.color = secondaryColor
        canvas.drawArc(oval, rotationDeg + 180f, 45f, false, arcPaint)
    }

    private fun drawCornerParticles(
        canvas: Canvas,
        originX: Float,
        originY: Float,
        dirX: Float,
        dirY: Float,
        armLength: Float,
        depth: Float,
        radius: Float
    ) {
        // Particle 1 travels from vertical arm to corner to horizontal arm
        val (p1x, p1y) = interpolateCornerPosition(particleOffset, armLength, depth)
        canvas.drawCircle(originX + p1x * dirX, originY + p1y * dirY, radius, particlePaint)

        // Particle 2 follows at offset
        val (p2x, p2y) = interpolateCornerPosition(secondaryParticleOffset, armLength, depth)
        canvas.drawCircle(originX + p2x * dirX, originY + p2y * dirY, radius * 0.75f, particlePaint)
    }

    private fun interpolateCornerPosition(progress: Float, arm: Float, depth: Float): Pair<Float, Float> {
        return if (progress < 0.5f) {
            val t = progress / 0.5f
            Pair(depth * (1f - cos(t * Math.PI.toFloat() * 0.5f)), arm * (1f - t))
        } else {
            val t = (progress - 0.5f) / 0.5f
            Pair(arm * t, depth * (1f - sin(t * Math.PI.toFloat() * 0.5f)))
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor.coerceIn(0f, 1f)).roundToInt()
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
