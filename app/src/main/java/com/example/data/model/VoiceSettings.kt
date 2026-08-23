package com.example.data.model

import android.content.Context
import androidx.core.content.edit

enum class VoiceSource(val displayName: String, val description: String) {
    SYSTEM_TTS("System TTS Engine", "High compatibility, uses device voice synthesizer"),
    GOOGLE_TTS("Google Speech Services", "Enhanced clarity and natural accents if installed"),
    OFFLINE_SYNTH("High-Pitch Clarity Engine", "Optimized diction and pronunciation timing")
}

data class VoiceSettings(
    val voiceSource: VoiceSource = VoiceSource.SYSTEM_TTS,
    val ukPitch: Float = 1.0f,
    val usPitch: Float = 1.0f,
    val speechRate: Float = 0.9f
) {
    companion object {
        private const val PREFS_NAME = "voice_settings_prefs"
        private const val KEY_SOURCE = "voice_source"
        private const val KEY_UK_PITCH = "uk_pitch"
        private const val KEY_US_PITCH = "us_pitch"
        private const val KEY_SPEECH_RATE = "speech_rate"

        fun load(context: Context): VoiceSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val sourceName = prefs.getString(KEY_SOURCE, VoiceSource.SYSTEM_TTS.name) ?: VoiceSource.SYSTEM_TTS.name
            val source = try {
                VoiceSource.valueOf(sourceName)
            } catch (e: Exception) {
                VoiceSource.SYSTEM_TTS
            }
            return VoiceSettings(
                voiceSource = source,
                ukPitch = prefs.getFloat(KEY_UK_PITCH, 1.0f),
                usPitch = prefs.getFloat(KEY_US_PITCH, 1.0f),
                speechRate = prefs.getFloat(KEY_SPEECH_RATE, 0.9f)
            )
        }

        fun save(context: Context, settings: VoiceSettings) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                putString(KEY_SOURCE, settings.voiceSource.name)
                putFloat(KEY_UK_PITCH, settings.ukPitch)
                putFloat(KEY_US_PITCH, settings.usPitch)
                putFloat(KEY_SPEECH_RATE, settings.speechRate)
            }
        }
    }
}
