package com.example.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TeluguTtsHelper(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var speakCompletionCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val teluguLocale = Locale("te", "IN")
            val result = tts?.setLanguage(teluguLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TeluguTtsHelper", "Telugu language is not supported or missing data!")
                isInitialized = false
                onInitComplete(false)
            } else {
                Log.d("TeluguTtsHelper", "Telugu language set successfully.")
                isInitialized = true
                configureFemaleVoice()
                setupProgressListener()
                onInitComplete(true)
            }
        } else {
            Log.e("TeluguTtsHelper", "TTS Initialization failed!")
            isInitialized = false
            onInitComplete(false)
        }
    }

    private fun configureFemaleVoice() {
        try {
            val voices = tts?.voices ?: return
            // Try to find a high-quality female Indian Telugu voice
            val teluguFemaleVoice = voices.firstOrNull { voice ->
                val locale = voice.locale
                locale.language == "te" && locale.country == "IN" && 
                (voice.name.contains("female", ignoreCase = true) || 
                 voice.name.contains("network", ignoreCase = true) || 
                 voice.name.contains("local", ignoreCase = true))
            } ?: voices.firstOrNull { voice ->
                voice.locale.language == "te"
            }

            if (teluguFemaleVoice != null) {
                tts?.voice = teluguFemaleVoice
                Log.d("TeluguTtsHelper", "Selected voice: ${teluguFemaleVoice.name}")
            } else {
                Log.w("TeluguTtsHelper", "No specific Telugu voice found, using default language voice.")
            }

            // Adjust speech characteristics to make it sound warm, friendly, and female
            tts?.setPitch(1.15f) // Slightly higher pitch for female virtual agent voice
            tts?.setSpeechRate(0.95f) // Slightly relaxed, professional receptionist speech rate
        } catch (e: Exception) {
            Log.e("TeluguTtsHelper", "Error configuring voice: ${e.message}")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("TeluguTtsHelper", "Speech started.")
            }

            override fun onDone(utteranceId: String?) {
                Log.d("TeluguTtsHelper", "Speech completed.")
                speakCompletionCallback?.invoke()
                speakCompletionCallback = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e("TeluguTtsHelper", "Speech error.")
                speakCompletionCallback?.invoke()
                speakCompletionCallback = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e("TeluguTtsHelper", "Speech error code: $errorCode")
                speakCompletionCallback?.invoke()
                speakCompletionCallback = null
            }
        })
    }

    fun speak(text: String, onComplete: () -> Unit) {
        if (!isInitialized || tts == null) {
            Log.e("TeluguTtsHelper", "TTS is not initialized yet!")
            onComplete()
            return
        }

        speakCompletionCallback = onComplete
        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MBConnectUtterance")
        }
        
        Log.d("TeluguTtsHelper", "Speaking Telugu text: $text")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "MBConnectUtterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("TeluguTtsHelper", "Error shutting down TTS: ${e.message}")
        }
    }
}
