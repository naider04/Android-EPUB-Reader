package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.VoiceSettings
import com.example.data.model.VoiceSource
import java.util.Locale

enum class PronunciationAccent {
    UK,
    US
}

class PronunciationPlayer(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeech: Pair<String, PronunciationAccent>? = null

    init {
        initEngine()
    }

    private fun initEngine() {
        val settings = VoiceSettings.load(context)
        tts = when (settings.voiceSource) {
            VoiceSource.GOOGLE_TTS -> {
                // Try initializing with Google Speech Services engine package
                try {
                    TextToSpeech(context.applicationContext, this, "com.google.android.tts")
                } catch (e: Exception) {
                    TextToSpeech(context.applicationContext, this)
                }
            }
            else -> {
                TextToSpeech(context.applicationContext, this)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })
            pendingSpeech?.let { (word, accent) ->
                speak(word, accent)
                pendingSpeech = null
            }
        }
    }

    fun speak(rawText: String, accent: PronunciationAccent = PronunciationAccent.US) {
        val textToSpeak = cleanWordForSpeech(rawText)
        if (textToSpeak.isBlank()) return

        if (!isInitialized || tts == null) {
            pendingSpeech = Pair(rawText, accent)
            return
        }

        val settings = VoiceSettings.load(context)
        val locale = when (accent) {
            PronunciationAccent.UK -> Locale.UK
            PronunciationAccent.US -> Locale.US
        }

        try {
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }

            val pitch = when {
                settings.voiceSource == VoiceSource.OFFLINE_SYNTH && accent == PronunciationAccent.UK -> 1.08f
                settings.voiceSource == VoiceSource.OFFLINE_SYNTH && accent == PronunciationAccent.US -> 0.96f
                accent == PronunciationAccent.UK -> settings.ukPitch
                else -> settings.usPitch
            }
            tts?.setPitch(pitch)
            tts?.setSpeechRate(settings.speechRate)
            val utteranceId = "pronounce_${accent.name}_${System.currentTimeMillis()}"
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cleanWordForSpeech(text: String): String {
        var clean = text
            .replace("·", "")
            .replace("—", ", ")
            .replace("–", ", ")
            .replace("[", "")
            .replace("]", "")
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("‘", "'")
            .replace("’", "'")

        // If it's a single dictionary pronunciation entry with part-of-speech parentheses like "ephemeral (adj.)"
        if (clean.lines().size == 1 && clean.contains("(") && clean.contains(")") && !clean.contains(" ")) {
            clean = clean.split("(")[0].trim()
        }
        return clean.trim()
    }
}

@Composable
fun rememberPronunciationPlayer(): PronunciationPlayer {
    val context = LocalContext.current
    val player = remember { PronunciationPlayer(context) }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    return player
}
