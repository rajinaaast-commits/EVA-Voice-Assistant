package com.example.voice

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioProcessor {
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var gainControl: AutomaticGainControl? = null

    private val _amplitude = MutableStateFlow(0.1f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isVADActive = MutableStateFlow(false)
    val isVADActive: StateFlow<Boolean> = _isVADActive.asStateFlow()

    fun attachToAudioSession(audioSessionId: Int) {
        try {
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply {
                    enabled = true
                }
            }
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply {
                    enabled = true
                }
            }
            if (AutomaticGainControl.isAvailable()) {
                gainControl = AutomaticGainControl.create(audioSessionId)?.apply {
                    enabled = true
                }
            }
        } catch (_: Exception) {
            // Hardware audiofx fallback
        }
    }

    fun release() {
        try {
            noiseSuppressor?.release()
            echoCanceler?.release()
            gainControl?.release()
            noiseSuppressor = null
            echoCanceler = null
            gainControl = null
        } catch (_: Exception) {}
    }

    fun updateAmplitude(rms: Float) {
        val clamped = (rms.coerceIn(0f, 100f) / 100f)
        _amplitude.value = clamped
        _isVADActive.value = clamped > 0.15f
    }
}
