package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.service.EvaVoiceService
import com.example.ui.components.EvaEdgeGlowOverlayManager
import com.example.ui.components.OrbState
import com.example.ui.components.ScreenCornerGlow
import com.example.ui.navigation.EvaNavGraph
import com.example.ui.theme.CosmicDarkBackground
import com.example.ui.theme.EvaTheme
import com.example.voice.EvaState
import com.example.voice.EvaStateManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = EvaApplication.instance

        // If configured and onboarded, ensure background service is active
        if (app.preferences.isOnboardingCompleted() &&
            (app.preferences.isWakeWordEnabled() || app.preferences.isEdgeGlowEnabled())) {
            try {
                EvaVoiceService.startService(this)
            } catch (_: Exception) {}
        }

        if (intent?.getBooleanExtra("trigger_assist", false) == true) {
            EvaStateManager.setWake()
        }

        setContent {
            val evaState by EvaStateManager.state.collectAsState()
            val amplitude by EvaStateManager.amplitude.collectAsState()

            val orbState = when (evaState) {
                EvaState.SPEAKING -> OrbState.SPEAKING
                EvaState.LISTENING -> OrbState.LISTENING
                EvaState.WAKE -> OrbState.WAKE_DETECTED
                EvaState.THINKING -> OrbState.THINKING
                EvaState.ERROR -> OrbState.ERROR
                EvaState.OFFLINE -> OrbState.OFFLINE
                EvaState.IDLE -> OrbState.IDLE
            }

            EvaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CosmicDarkBackground
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EvaNavGraph()
                        // In-app corner animation: thin neon cyan/blue glowing lines & particles
                        ScreenCornerGlow(
                            isActive = evaState != EvaState.IDLE,
                            orbState = orbState,
                            amplitude = amplitude,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // App is now in foreground: let in-app ScreenCornerGlow render
        EvaEdgeGlowOverlayManager.getInstance(this).onAppForegroundChanged(true)
    }

    override fun onResume() {
        super.onResume()
        EvaEdgeGlowOverlayManager.getInstance(this).onAppForegroundChanged(true)
    }

    override fun onStop() {
        super.onStop()
        // App is minimized / closed: activate system overlay if configured
        EvaEdgeGlowOverlayManager.getInstance(this).onAppForegroundChanged(false)
    }
}
