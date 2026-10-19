package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GeminiAiProvider {
    companion object {
        const val DEFAULT_MODEL = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"

        // Strictly prohibited models according to Gemini API guidance
        private val PROHIBITED_MODELS = setOf(
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-pro",
            "gemini-2.0-flash",
            "gemini-2.0-pro",
            "gemini-2.0-flash-thinking"
        )
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api: GeminiApi = retrofit.create(GeminiApi::class.java)

    /**
     * Resolves the active API key with fallback to BuildConfig.
     */
    fun resolveApiKey(configuredKey: String?): String {
        val trimmed = configuredKey?.trim().orEmpty()
        if (trimmed.isNotBlank() && trimmed != "MY_GEMINI_API_KEY") {
            return trimmed
        }
        val buildConfigKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()?.trim().orEmpty()
        if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey
        }
        return ""
    }

    /**
     * Validates and normalizes the model name to avoid prohibited legacy endpoints.
     */
    fun sanitizeModelName(model: String?): String {
        val clean = model?.trim().orEmpty()
        if (clean.isBlank() || PROHIBITED_MODELS.contains(clean)) {
            return DEFAULT_MODEL
        }
        return clean
    }

    /**
     * Generates text using Retrofit and the Gemini generateContent REST API.
     */
    suspend fun generateText(
        model: String,
        apiKey: String,
        prompt: String,
        systemInstruction: String? = null,
        temperature: Float = 0.7f
    ): Result<String> = withContext(Dispatchers.IO) {
        val effectiveApiKey = resolveApiKey(apiKey)
        if (effectiveApiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please enter your API key in Settings or configure GEMINI_API_KEY in the Secrets panel.")
            )
        }

        if (prompt.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Prompt cannot be empty."))
        }

        try {
            val targetModel = sanitizeModelName(model)

            val systemContent = if (!systemInstruction.isNullOrBlank()) {
                ContentDto(parts = listOf(PartDto(text = systemInstruction)))
            } else null

            val request = GenerateContentRequest(
                contents = listOf(
                    ContentDto(
                        parts = listOf(PartDto(text = prompt)),
                        role = "user"
                    )
                ),
                generationConfig = GenerationConfigDto(temperature = temperature),
                systemInstruction = systemContent
            )

            val response = api.generateContent(
                model = targetModel,
                apiKey = effectiveApiKey,
                request = request,
                apiKeyHeader = effectiveApiKey
            )

            if (response.error != null) {
                val errorMsg = response.error.message ?: "Gemini API error (code: ${response.error.code})"
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Check if blocked by safety filters
            val promptBlocked = response.promptFeedback?.blockReason
            if (!promptBlocked.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Request blocked by safety policy: $promptBlocked"))
            }

            val firstCandidate = response.candidates?.firstOrNull()
            val text = firstCandidate?.content?.parts?.firstOrNull()?.text

            if (!text.isNullOrBlank()) {
                Result.success(text.trim())
            } else {
                val finishReason = firstCandidate?.finishReason ?: "No response content generated"
                Result.failure(Exception("Generation ended without content ($finishReason)."))
            }
        } catch (e: Exception) {
            val safeMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                        e.message?.contains("No address associated with hostname", ignoreCase = true) == true ->
                    "Network unreachable. Please check your internet connection."

                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Gemini API request timed out. Please try again."

                e.message?.contains("400") == true ->
                    "Invalid request parameters or model format."

                e.message?.contains("403") == true ->
                    "Gemini API key is invalid or unauthorized."

                e.message?.contains("429") == true || e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ->
                    "Gemini API quota exceeded or rate limited. Please try again in a few moments."

                e.message?.contains("404") == true ->
                    "Model not found. Switched to standard model ($DEFAULT_MODEL)."

                else -> e.localizedMessage ?: "Failed to generate content."
            }
            Result.failure(Exception(safeMessage))
        }
    }
}
