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
import com.example.ui.components.OrbState
import com.example.ui.components.ScreenCornerGlow
import com.example.ui.navigation.EvaNavGraph
import com.example.ui.theme.CosmicDarkBackground
import com.example.ui.theme.EvaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = EvaApplication.instance
            val isListening by app.speechHelper.isListening.collectAsState()
            val isSpeaking by app.ttsHelper.isSpeaking.collectAsState()
            val amplitude by app.speechHelper.rmsAmplitude.collectAsState()

            val orbState = when {
                isSpeaking -> OrbState.SPEAKING
                isListening -> OrbState.LISTENING
                else -> OrbState.IDLE
            }

            EvaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CosmicDarkBackground
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EvaNavGraph()
                        // Futuristic corner animation: thin neon cyan/blue glowing lines & particles
                        ScreenCornerGlow(
                            isActive = isListening || isSpeaking,
                            orbState = orbState,
                            amplitude = amplitude,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
