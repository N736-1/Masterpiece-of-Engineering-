package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// --- Moshi Data Classes for Gemini REST API ---

data class Part(
    val text: String? = null
)

data class Content(
    val parts: List<Part>
)

data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
)

data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

data class Candidate(
    val content: Content? = null
)

data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

// --- Gemini SQL Assistant Service ---

object GeminiService {
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(GenerateContentRequest::class.java)
    private val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateSql(userPrompt: String, schemaContext: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: API Key is missing. Please configure GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val systemPrompt = """
            You are a master PostgreSQL database developer and SQL developer assistant.
            The user wants to generate a PostgreSQL SQL statement.
            Active Database Schemas Context:
            $schemaContext
            
            Instructions:
            - Provide ONLY a clean, valid, standard PostgreSQL query.
            - Wrap the query in standard markdown code block: ```sql ... ```
            - If the prompt is not about generating SQL or asks conversational queries, try your best to assist but still relate it to PostgreSQL queries.
            - Be concise, clean, and write optimized queries.
        """.trimIndent()

        val fullPrompt = "User Prompt: $userPrompt"

        val requestPayload = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = fullPrompt)))
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                maxOutputTokens = 1000
            )
        )

        try {
            val jsonString = jsonAdapter.toJson(requestPayload)
            val mediaType = "application/json".toMediaType()
            val requestBody = jsonString.toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            
            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error connection level: ${response.code} ${response.message}"
                }
                val bodyString = response.body?.string() ?: return@withContext "Error: Empty response body"
                val parsedResponse = responseAdapter.fromJson(bodyString)
                val responseText = parsedResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                responseText ?: "Error: Received empty suggestion from AI service."
            }
        } catch (e: Exception) {
            "Error API Call: ${e.message ?: e.toString()}"
        }
    }
}
