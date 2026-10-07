package com.example.voice

import com.example.data.local.daos.VoiceProfileDao
import com.example.data.local.entities.VoiceProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class VoiceMatchResult {
    KNOWN_VOICE,
    UNKNOWN_VOICE,
    UNCLEAR_VOICE
}

class VoiceProfileManager(private val voiceProfileDao: VoiceProfileDao) {
    private val _currentProfile = MutableStateFlow<VoiceProfileEntity?>(null)
    val currentProfile: StateFlow<VoiceProfileEntity?> = _currentProfile.asStateFlow()

    private val _isVoiceMatchEnabled = MutableStateFlow(true)
    val isVoiceMatchEnabled: StateFlow<Boolean> = _isVoiceMatchEnabled.asStateFlow()

    private val _sensitivity = MutableStateFlow(0.75f) // 0.0 to 1.0
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    suspend fun loadProfile() {
        val profile = voiceProfileDao.getProfile()
        _currentProfile.value = profile
    }

    suspend fun recordSample(samplePitchHz: Float, sampleEnergy: Float) {
        val existing = voiceProfileDao.getProfile()
        val newSampleCount = (existing?.sampleCount ?: 0) + 1
        val updatedPitch = if (existing != null && existing.sampleCount > 0) {
            (existing.averagePitchHz * existing.sampleCount + samplePitchHz) / newSampleCount
        } else {
            samplePitchHz
        }

        val updated = VoiceProfileEntity(
            id = "default_user",
            userName = existing?.userName ?: "User",
            sampleCount = newSampleCount,
            averagePitchHz = updatedPitch,
            isTrained = newSampleCount >= 3,
            updatedAt = System.currentTimeMillis()
        )
        voiceProfileDao.insertProfile(updated)
        _currentProfile.value = updated
    }

    fun verifyVoice(incomingPitchHz: Float, incomingRms: Float): VoiceMatchResult {
        if (!_isVoiceMatchEnabled.value) return VoiceMatchResult.KNOWN_VOICE
        val profile = _currentProfile.value ?: return VoiceMatchResult.KNOWN_VOICE
        if (!profile.isTrained) return VoiceMatchResult.KNOWN_VOICE

        if (incomingRms < 10f) {
            return VoiceMatchResult.UNCLEAR_VOICE
        }

        val diff = abs(profile.averagePitchHz - incomingPitchHz)
        val tolerance = (1.0f - _sensitivity.value) * 80f + 25f // dynamic tolerance range

        return if (diff <= tolerance) {
            VoiceMatchResult.KNOWN_VOICE
        } else {
            VoiceMatchResult.UNKNOWN_VOICE
        }
    }

    suspend fun deleteProfile() {
        voiceProfileDao.deleteProfile()
        _currentProfile.value = null
    }

    fun setVoiceMatchEnabled(enabled: Boolean) {
        _isVoiceMatchEnabled.value = enabled
    }

    fun setSensitivity(value: Float) {
        _sensitivity.value = value.coerceIn(0.1f, 1.0f)
    }
}
