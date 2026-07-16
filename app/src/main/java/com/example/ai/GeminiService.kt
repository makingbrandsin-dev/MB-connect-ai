package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.DecryptedFaq
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- MOSHI-COMPATIBLE DATA CLASSES FOR GEMINI ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

// --- RETROFIT INTERFACE ---

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- GEMINI SERVICE IMPLEMENTATION ---

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Call the Gemini API to respond to a caller's voice input, incorporating FAQs
     * and outputting Telugu text.
     */
    suspend fun generateBotResponse(
        callerMessage: String,
        faqs: List<DecryptedFaq>,
        callerNumber: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is empty or default placeholder.")
            return@withContext getOfflineResponse(callerMessage, faqs)
        }

        // Construct FAQ context for prompt
        val faqContext = if (faqs.isEmpty()) {
            "No custom FAQs defined."
        } else {
            faqs.joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }
        }

        val prompt = """
            You are an AI automated call assistant for "MB Connect".
            A customer is calling from $callerNumber.
            
            Their spoken message is: "$callerMessage"
            
            Here is our business's official FAQ database:
            $faqContext
            
            Instructions:
            1. If the user's inquiry matches an FAQ question, respond directly using the FAQ answer.
            2. If there is no matching FAQ, generate a polite response stating that their query has been recorded, we will notify the business owner, and someone will call them back shortly.
            3. CRITICAL: You MUST write your entire response ONLY in Telugu script (with clear, natural Telugu phrasing) because the TTS voice reading it will speak Telugu. Do not include any English alphabet letters in your main response. Keep it concise, under 2-3 sentences.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            systemInstruction = Content(parts = listOf(Part(text = "You are a polite, helpful, female Indian call-answering receptionist representing MB Connect. Speak exclusively in native Telugu.")))
        )

        try {
            val response = api.generateContent(apiKey, request)
            val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!resultText.isNullOrBlank()) {
                resultText.trim()
            } else {
                getOfflineResponse(callerMessage, faqs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response via Gemini API: ${e.message}", e)
            getOfflineResponse(callerMessage, faqs)
        }
    }

    /**
     * Generate an AI summary and transcribe call speech.
     */
    suspend fun generateCallSummary(
        callerMessage: String,
        botResponse: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Offline Mode: Summary of call. Caller requested assistance regarding '$callerMessage'."
        }

        val prompt = """
            Please summarize the following phone call interaction into a single, punchy, professional sentence for a business dashboard.
            
            Caller said: "$callerMessage"
            AI Assistant responded in Telugu: "$botResponse"
            
            Response should be in English, professional, highlighting the key issue or request.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.3f),
            systemInstruction = Content(parts = listOf(Part(text = "You are a precise, business-oriented call analyzer.")))
        )

        try {
            val response = api.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() 
                ?: "Call completed. Message received: $callerMessage"
        } catch (e: Exception) {
            Log.e(TAG, "Error generating call summary: ${e.message}", e)
            "Call completed. Message received: $callerMessage"
        }
    }

    /**
     * Rule-based local responder for offline fallback.
     */
    fun getOfflineResponse(callerMessage: String, faqs: List<DecryptedFaq>): String {
        // Fallback Telugu message: "నమస్కారం, మీ సందేశం రికార్డ్ చేయబడింది. మేము త్వరలో మిమ్మల్ని సంప్రదిస్తాము."
        // (Hello, your message has been recorded. We will contact you soon.)
        val defaultTelugu = "నమస్కారం, మీ సందేశం రికార్డ్ చేయబడింది. ఎంబీ కనెక్ట్ తరపున మేము త్వరలో మిమ్మల్ని సంప్రదిస్తాము."
        
        if (faqs.isEmpty()) return defaultTelugu

        // Simple local keyword matching for FAQ offline capability
        val messageLower = callerMessage.lowercase()
        for (faq in faqs) {
            val qLower = faq.question.lowercase()
            // If overlapping keywords
            val keywords = qLower.split(" ", "?", ",").filter { it.length > 3 }
            if (keywords.isNotEmpty() && keywords.any { messageLower.contains(it) }) {
                // If the FAQ answer is already in Telugu or is configured, return it
                return faq.answer
            }
        }
        return defaultTelugu
    }
}
