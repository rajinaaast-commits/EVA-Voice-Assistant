package com.example.ui.components

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import com.example.data.preferences.EvaPreferences
import com.example.voice.EvaState
import com.example.voice.EvaStateManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * System-level manager for the floating EVA Edge Glow overlay window.
 * Attaches EvaEdgeGlowOverlayView to WindowManager with TYPE_APPLICATION_OVERLAY
 * and syncs with EvaStateManager's real state machine.
 */
class EvaEdgeGlowOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val prefs = EvaPreferences(context)
    private var overlayView: EvaEdgeGlowOverlayView? = null
    private var isAttached = false
    private var isAppInForeground = false

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateJob: Job? = null
    private var ampJob: Job? = null

    companion object {
        @Volatile
        private var instance: EvaEdgeGlowOverlayManager? = null

        fun getInstance(context: Context): EvaEdgeGlowOverlayManager {
            return instance ?: synchronized(this) {
                instance ?: EvaEdgeGlowOverlayManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Called by MainActivity when foreground status changes.
     */
    fun onAppForegroundChanged(inForeground: Boolean) {
        isAppInForeground = inForeground
        updateOverlayVisibility()
    }

    /**
     * Initializes state monitoring and evaluates if overlay should appear.
     */
    fun start() {
        if (stateJob == null) {
            stateJob = scope.launch {
                EvaStateManager.state.collectLatest { state ->
                    overlayView?.currentState = state
                    updateOverlayVisibility()
                }
            }
        }

        if (ampJob == null) {
            ampJob = scope.launch {
                EvaStateManager.amplitude.collectLatest { amp ->
                    overlayView?.currentAmplitude = amp
                }
            }
        }

        updateOverlayVisibility()
    }

    fun stop() {
        stateJob?.cancel()
        stateJob = null
        ampJob?.cancel()
        ampJob = null
        removeOverlay()
    }

    /**
     * Evaluates current system permissions, user preferences, and app state
     * to safely attach or detach the overlay window.
     */
    fun updateOverlayVisibility() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        val enabled = prefs.isEdgeGlowEnabled()
        val showWhenClosed = prefs.isShowEdgeGlowWhenClosed()

        // If app is closed, show only if showWhenClosed is true.
        // If app is open in foreground, MainActivity draws ScreenCornerGlow inside Compose to save battery.
        val shouldShow = hasPermission && enabled && (!isAppInForeground && showWhenClosed)

        if (shouldShow) {
            attachOverlay()
        } else {
            removeOverlay()
        }
    }

    private fun attachOverlay() {
        if (isAttached || windowManager == null) return

        try {
            if (overlayView == null) {
                overlayView = EvaEdgeGlowOverlayView(context).apply {
                    currentState = EvaStateManager.currentState()
                    currentAmplitude = EvaStateManager.amplitude.value
                }
            }

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            windowManager.addView(overlayView, params)
            isAttached = true
            overlayView?.startAnimation()
        } catch (_: Exception) {
            isAttached = false
        }
    }

    private fun removeOverlay() {
        if (!isAttached || windowManager == null || overlayView == null) return

        try {
            overlayView?.stopAnimation()
            windowManager.removeView(overlayView)
        } catch (_: Exception) {
        } finally {
            isAttached = false
            overlayView = null
        }
    }

    fun isOverlayActive(): Boolean = isAttached
}
