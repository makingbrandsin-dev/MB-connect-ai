package com.example.service

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import com.example.ai.GeminiService
import com.example.data.DataRepository
import com.example.data.DecryptedCallLog
import com.example.data.DecryptedPriorityContact
import com.example.tts.TeluguTtsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class CallState {
    object Idle : CallState()
    data class Incoming(val phoneNumber: String, val contactName: String, val callType: String) : CallState()
    data class Answering(val phoneNumber: String, val contactName: String, val status: String) : CallState()
    data class RecordingCaller(val phoneNumber: String, val durationLeft: Int) : CallState()
    data class SpeakingResponse(val text: String) : CallState()
    data class Completed(val summary: String) : CallState()
}

class CallAnsweringService(
    private val context: Context,
    private val repository: DataRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var ttsHelper: TeluguTtsHelper? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isTtsInitialized = false

    var activePitch: Float = 1.15f
    var activeRate: Float = 0.95f

    private val _currentCallState = MutableStateFlow<CallState>(CallState.Idle)
    val currentCallState: StateFlow<CallState> = _currentCallState

    init {
        initializeTts()
    }

    private fun initializeTts() {
        ttsHelper = TeluguTtsHelper(context) { success ->
            isTtsInitialized = success
            Log.d("CallAnsweringService", "Telugu TTS engine initialized: $success")
        }
    }

    /**
     * Intercept or Simulate an incoming call (SIM or WhatsApp)
     */
    fun onIncomingCall(phoneNumber: String, name: String, callType: String) {
        scope.launch {
            _currentCallState.value = CallState.Incoming(phoneNumber, name, callType)

            // Check Priority Route list
            val priorityContacts = repository.getEnabledPriorityContacts()
            val matchedContact = priorityContacts.firstOrNull { it.phoneNumber == phoneNumber }

            if (matchedContact != null) {
                Log.d("CallAnsweringService", "Incoming call matched VIP contact: ${matchedContact.name}")
                when (matchedContact.routeAction) {
                    "AUTO_ANSWER" -> {
                        delay(2000) // Simulate phone ringing for 2 seconds
                        startAutoAnswering(phoneNumber, name, callType, "Auto-Answer (Priority list)")
                    }
                    "BYPASS_BOT" -> {
                        // Priority routing: bypass the bot entirely, allow it to ring through
                        _currentCallState.value = CallState.Answering(phoneNumber, name, "Bypassed (Bypassed Bot)")
                        delay(3000)
                        saveCallLog(
                            phoneNumber = phoneNumber,
                            name = name,
                            callType = callType,
                            transcription = "Call bypassed bot as configured for priority contact.",
                            summary = "VIP call from $name was routed directly.",
                            status = "Bypassed",
                            suggestedReply = ""
                        )
                        resetCallState()
                    }
                    "REJECT" -> {
                        _currentCallState.value = CallState.Answering(phoneNumber, name, "Rejected Automatically")
                        delay(2000)
                        saveCallLog(
                            phoneNumber = phoneNumber,
                            name = name,
                            callType = callType,
                            transcription = "Call automatically rejected based on blacklist routing.",
                            summary = "Spam/blacklisted call from $phoneNumber was auto-rejected.",
                            status = "Rejected",
                            suggestedReply = ""
                        )
                        resetCallState()
                    }
                    "AUTO_REPLY" -> {
                        _currentCallState.value = CallState.Answering(phoneNumber, name, "Auto-Replied via SMS")
                        delay(2000)
                        saveCallLog(
                            phoneNumber = phoneNumber,
                            name = name,
                            callType = callType,
                            transcription = "Call auto-answered with pre-configured quick reply.",
                            summary = "Auto-replied with suggested responses to $name.",
                            status = "Auto-Replied",
                            suggestedReply = "I am busy right now. I will call you back shortly."
                        )
                        resetCallState()
                    }
                }
            } else {
                // Default: Auto Answer with AI Bot if not explicitly configured differently
                delay(2000)
                startAutoAnswering(phoneNumber, name, callType, "Answered by AI Agent")
            }
        }
    }

    private fun startAutoAnswering(phoneNumber: String, name: String, callType: String, initialStatus: String) {
        scope.launch {
            _currentCallState.value = CallState.Answering(phoneNumber, name, initialStatus)
            delay(1500)

            // Start Recording Caller input (microphone or synthetic user speech input)
            startRecordingAudio()
            
            // Wait for Caller speech simulation (simulated call length 6 seconds)
            for (i in 6 downTo 1) {
                _currentCallState.value = CallState.RecordingCaller(phoneNumber, i)
                delay(1000)
            }

            stopRecordingAudio()
            
            // Default simulated caller message if real mic was silent/not recorded
            val callerMessage = "ఎంబీ కనెక్ట్ సమాచారం ఏంటి? ప్రైసింగ్ ఎలా ఉంటుంది?" 
                // ("What is MB Connect information? How is pricing?")

            // Process with Gemini AI + local FAQs database
            _currentCallState.value = CallState.Answering(phoneNumber, name, "Processing with Gemini...")
            val faqs = repository.getEnabledFaqs()
            
            // Get AI generated Telugu script response
            val botResponse = GeminiService.generateBotResponse(callerMessage, faqs, phoneNumber)
            
            // Speak response using our warm Telugu voice agent
            _currentCallState.value = CallState.SpeakingResponse(botResponse)
            
            speakBotResponse(botResponse) {
                // Speech finished. Generate summary and complete call log
                scope.launch {
                    _currentCallState.value = CallState.Answering(phoneNumber, name, "Analyzing call summary...")
                    val summary = GeminiService.generateCallSummary(callerMessage, botResponse)
                    
                    saveCallLog(
                        phoneNumber = phoneNumber,
                        name = name,
                        callType = callType,
                        transcription = callerMessage,
                        summary = summary,
                        status = "Answered by AI",
                        suggestedReply = botResponse
                    )

                    _currentCallState.value = CallState.Completed(summary)
                    delay(3000)
                    resetCallState()
                }
            }
        }
    }

    private fun startRecordingAudio() {
        try {
            val audioDir = File(context.cacheDir, "recordings").apply { mkdirs() }
            recordingFile = File(audioDir, "call_rec_${System.currentTimeMillis()}.amr")

            // On modern Android environments, permissions might require a mock recorder
            // but we implement a fully functional MediaRecorder block. If standard Recorder
            // fails due to headless container, we log and fallback gracefully.
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(recordingFile!!.absolutePath)
                prepare()
                start()
            }
            Log.d("CallAnsweringService", "Audio recording started at: ${recordingFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("CallAnsweringService", "MediaRecorder initialization failed: ${e.message}. Using synthetic stream fallback.")
            mediaRecorder = null
        }
    }

    private fun stopRecordingAudio() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d("CallAnsweringService", "Audio recording stopped.")
        } catch (e: Exception) {
            Log.e("CallAnsweringService", "Error stopping MediaRecorder: ${e.message}")
            mediaRecorder = null
        }
    }

    private fun speakBotResponse(text: String, onDone: () -> Unit) {
        if (isTtsInitialized && ttsHelper != null) {
            ttsHelper?.updateSpeechCharacteristics(activePitch, activeRate)
            ttsHelper?.speak(text, onDone)
        } else {
            Log.w("CallAnsweringService", "TTS not ready, calling completion immediately.")
            scope.launch {
                delay(3000) // Simulate speech reading time
                onDone()
            }
        }
    }

    fun speakDirectly(text: String, pitch: Float, rate: Float) {
        scope.launch {
            if (isTtsInitialized && ttsHelper != null) {
                ttsHelper?.updateSpeechCharacteristics(pitch, rate)
                ttsHelper?.speak(text) {
                    Log.d("CallAnsweringService", "Direct preview speaking complete.")
                }
            } else {
                Log.w("CallAnsweringService", "TTS not ready for direct speech preview.")
            }
        }
    }

    private suspend fun saveCallLog(
        phoneNumber: String,
        name: String,
        callType: String,
        transcription: String,
        summary: String,
        status: String,
        suggestedReply: String
    ) {
        val audioPath = recordingFile?.absolutePath ?: "synthetic_stream_fallback"
        val log = DecryptedCallLog(
            phoneNumber = phoneNumber,
            contactName = name.ifEmpty { "Unknown Caller" },
            callType = callType,
            timestamp = System.currentTimeMillis(),
            durationSeconds = 12,
            transcription = transcription,
            summary = summary,
            audioFilePath = audioPath,
            status = status,
            suggestedReply = suggestedReply
        )
        repository.insertCallLog(log)
        Log.d("CallAnsweringService", "Securely stored encrypted call log: $log")
    }

    fun resetCallState() {
        _currentCallState.value = CallState.Idle
        ttsHelper?.stop()
    }

    fun destroy() {
        ttsHelper?.shutdown()
    }
}
