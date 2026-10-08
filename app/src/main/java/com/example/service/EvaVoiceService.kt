package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.EvaApplication
import com.example.MainActivity
import com.example.ui.components.EvaEdgeGlowOverlayManager
import com.example.voice.EvaState
import com.example.voice.EvaStateManager
import com.example.voice.WakeWordDetector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Android Foreground Service for EVA AI Background Assistant & Edge Glow Overlay.
 * Manages background wake phrase detection ("Hey EVA", "Wake EVA"), connects
 * to the speech pipeline, and runs the system-level Edge Glow overlay.
 */
class EvaVoiceService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeListeningJob: Job? = null
    private var isCurrentlyListeningForCommand = false

    companion object {
        const val CHANNEL_ID = "eva_voice_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_STOP_SERVICE = "com.example.service.STOP_EVA_VOICE_SERVICE"
        const val ACTION_TRIGGER_ASSIST = "com.example.service.TRIGGER_EVA_ASSIST"

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, EvaVoiceService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EvaVoiceService::class.java)
            try {
                context.stopService(intent)
            } catch (_: Exception) {}
        }

        fun isRunning(context: Context): Boolean = isServiceRunning
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()

        // Immediately promote to foreground to satisfy Android requirements
        promoteToForeground(buildForegroundNotification(EvaStateManager.currentState()))

        // Start Edge Glow overlay manager
        EvaEdgeGlowOverlayManager.getInstance(this).start()

        // Sync service state with notification updates
        serviceScope.launch {
            EvaStateManager.state.collectLatest { state ->
                updateNotificationForState(state)
            }
        }

        // Start wake-word loop if microphone permission is granted
        startBackgroundWakeDetection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        } else if (intent?.action == ACTION_TRIGGER_ASSIST) {
            triggerAssistantWake()
        }

        val notification = buildForegroundNotification(EvaStateManager.currentState())
        promoteToForeground(notification)
        return START_STICKY
    }

    override fun onDestroy() {
        isServiceRunning = false
        wakeListeningJob?.cancel()
        serviceScope.cancel()

        // Stop Edge Glow overlay
        EvaEdgeGlowOverlayManager.getInstance(this).stop()

        super.onDestroy()
    }

    private fun startBackgroundWakeDetection() {
        wakeListeningJob?.cancel()
        val app = EvaApplication.instance

        if (!app.preferences.isWakeWordEnabled()) {
            return
        }

        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            return
        }

        wakeListeningJob = serviceScope.launch {
            while (isActive) {
                // If not currently executing a command or speaking, listen for wake phrase
                if (!isCurrentlyListeningForCommand && EvaStateManager.currentState() == EvaState.IDLE) {
                    delay(2500L)
                    // Periodic local speech recognizer probe for "Hey EVA"
                    if (isActive && !isCurrentlyListeningForCommand && EvaStateManager.currentState() == EvaState.IDLE) {
                        listenForWakePhrase()
                    }
                } else {
                    delay(1000L)
                }
            }
        }
    }

    private fun listenForWakePhrase() {
        val app = EvaApplication.instance
        if (isCurrentlyListeningForCommand || EvaStateManager.currentState() != EvaState.IDLE) return

        try {
            app.speechHelper.startListening(
                language = app.preferences.getLanguage(),
                onFinalResult = { text ->
                    val match = WakeWordDetector.detectAndStrip(text)
                    if (match.detected) {
                        handleWakeWordTriggered(match.cleanCommand)
                    }
                },
                onError = {
                    // Normal idle timeout
                }
            )
        } catch (_: Exception) {}
    }

    private fun triggerAssistantWake() {
        serviceScope.launch {
            handleWakeWordTriggered("")
        }
    }

    private fun handleWakeWordTriggered(commandText: String) {
        val app = EvaApplication.instance
        isCurrentlyListeningForCommand = true

        serviceScope.launch {
            // 1. Wake animation
            EvaStateManager.setWake()
            delay(500L)

            // If user already said a command with the wake phrase (e.g. "Hey EVA what time is it")
            if (commandText.isNotBlank()) {
                processUserCommand(commandText)
            } else {
                // 2. Transition to Listening for command
                EvaStateManager.setListening()
                app.speechHelper.startListening(
                    language = app.preferences.getLanguage(),
                    onFinalResult = { userSpokenCommand ->
                        serviceScope.launch {
                            processUserCommand(userSpokenCommand)
                        }
                    },
                    onError = {
                        EvaStateManager.setIdle()
                        isCurrentlyListeningForCommand = false
                    }
                )
            }
        }
    }

    private suspend fun processUserCommand(command: String) {
        val app = EvaApplication.instance
        if (command.isBlank()) {
            EvaStateManager.setIdle()
            isCurrentlyListeningForCommand = false
            return
        }

        // 3. Thinking state
        EvaStateManager.setThinking()

        try {
            val response = app.aiProviderManager.generateResponse(prompt = command)
            val replyText = response.text.ifBlank { "I'm here." }

            // 4. Speaking state
            EvaStateManager.setSpeaking()
            app.ttsHelper.speak(replyText)

            // Monitor when speech finishes
            val checkJob = serviceScope.launch {
                app.ttsHelper.isSpeaking.collectLatest { speaking ->
                    if (!speaking) {
                        EvaStateManager.setIdle()
                        isCurrentlyListeningForCommand = false
                        cancel()
                    }
                }
            }

            // Safety timeout after speaking
            delay(15000L)
            checkJob.cancel()
            EvaStateManager.setIdle()
            isCurrentlyListeningForCommand = false
        } catch (e: Exception) {
            EvaStateManager.setError(e.localizedMessage ?: "Processing error")
            delay(2500L)
            EvaStateManager.setIdle()
            isCurrentlyListeningForCommand = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EVA Background Voice & Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Enables EVA to listen for 'Hey EVA' wake phrases and display Edge Glow"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(state: EvaState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, EvaVoiceService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_IMMUTABLE
        )

        val stateText = when (state) {
            EvaState.WAKE -> "Wake phrase detected"
            EvaState.LISTENING -> "Listening to speech..."
            EvaState.THINKING -> "Thinking..."
            EvaState.SPEAKING -> "Speaking response..."
            EvaState.ERROR -> "Alert"
            EvaState.OFFLINE -> "Offline"
            EvaState.IDLE -> "Active • Edge Glow & Wake Word enabled"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EVA AI Assistant")
            .setContentText(stateText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotificationForState(state: EvaState) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            manager?.notify(NOTIFICATION_ID, buildForegroundNotification(state))
        } catch (_: Exception) {}
    }

    private fun promoteToForeground(notification: Notification) {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (targetSdk 34-36)
            var started = false
            if (hasMicPermission) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                    started = true
                } catch (_: SecurityException) {
                    // Restricted from starting microphone FGS from current caller state
                } catch (_: Exception) {}
            }
            if (!started) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (_: Exception) {}
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val type = if (hasMicPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, notification, type)
            } catch (e: Exception) {
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (_: Exception) {}
            }
        } else {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
