package com.example.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EvaState {
    IDLE,
    WAKE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR,
    OFFLINE
}

/**
 * Centralized, synchronized state machine for EVA AI.
 * Controls both in-app visuals and system-level background overlays.
 */
object EvaStateManager {
    private val _state = MutableStateFlow(EvaState.IDLE)
    val state: StateFlow<EvaState> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _statusText = MutableStateFlow("Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    fun setIdle() {
        _state.value = EvaState.IDLE
        _amplitude.value = 0f
        _statusText.value = "Ready"
    }

    fun setWake() {
        _state.value = EvaState.WAKE
        _amplitude.value = 0.8f
        _statusText.value = "Wake phrase detected"
    }

    fun setListening() {
        _state.value = EvaState.LISTENING
        _statusText.value = "Listening..."
    }

    fun setThinking() {
        _state.value = EvaState.THINKING
        _amplitude.value = 0.4f
        _statusText.value = "Thinking..."
    }

    fun setSpeaking() {
        _state.value = EvaState.SPEAKING
        _statusText.value = "Speaking..."
    }

    fun setError(message: String = "Error") {
        _state.value = EvaState.ERROR
        _amplitude.value = 0.5f
        _statusText.value = message
    }

    fun setOffline() {
        _state.value = EvaState.OFFLINE
        _amplitude.value = 0f
        _statusText.value = "Offline"
    }

    fun updateAmplitude(rms: Float) {
        _amplitude.value = rms.coerceIn(0f, 1f)
    }

    fun currentState(): EvaState = _state.value
}
