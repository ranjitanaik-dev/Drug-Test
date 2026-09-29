package com.ncb.drugtestcompanion.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TextToSpeech Audio & Voice Announcement Manager for Niriksh.
 * Provides welcome voice greeting on app launch and voice result announcements
 * (e.g. "Welcome to Niriksh", "Test result: Positive", "Test result: Negative", "Test result: Inconclusive").
 */
@Singleton
class VoiceAnnouncementManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Throwable) {
            Log.e("VOICE_TTS", "Error initializing TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
                tts?.setSpeechRate(0.92f)
                pendingText?.let { text ->
                    speak(text)
                    pendingText = null
                }
            } else {
                Log.e("VOICE_TTS", "Locale.US not supported for TextToSpeech")
            }
        } else {
            Log.e("VOICE_TTS", "TextToSpeech initialization failed with status $status")
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (isInitialized && tts != null) {
            try {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_${System.currentTimeMillis()}")
            } catch (e: Throwable) {
                Log.e("VOICE_TTS", "Error executing speak", e)
            }
        } else {
            pendingText = text
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Throwable) {}
    }
}
