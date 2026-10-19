package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class GenerateContentRequest(
    val contents: List<ContentDto>,
    val generationConfig: GenerationConfigDto? = null,
    val systemInstruction: ContentDto? = null,
    val safetySettings: List<SafetySettingDto>? = null
)

@JsonClass(generateAdapter = false)
data class ContentDto(
    val parts: List<PartDto>,
    val role: String? = null
)

@JsonClass(generateAdapter = false)
data class PartDto(
    val text: String? = null,
    val inlineData: InlineDataDto? = null
)

@JsonClass(generateAdapter = false)
data class InlineDataDto(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = false)
data class GenerationConfigDto(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = false)
data class SafetySettingDto(
    val category: String,
    val threshold: String
)

@JsonClass(generateAdapter = false)
data class GenerateContentResponse(
    val candidates: List<CandidateDto>? = null,
    val promptFeedback: PromptFeedbackDto? = null,
    val usageMetadata: UsageMetadataDto? = null,
    val error: GeminiErrorDto? = null
)

@JsonClass(generateAdapter = false)
data class CandidateDto(
    val content: ContentDto? = null,
    val finishReason: String? = null,
    val index: Int? = null,
    val safetyRatings: List<SafetyRatingDto>? = null
)

@JsonClass(generateAdapter = false)
data class SafetyRatingDto(
    val category: String? = null,
    val probability: String? = null
)

@JsonClass(generateAdapter = false)
data class PromptFeedbackDto(
    val blockReason: String? = null,
    val safetyRatings: List<SafetyRatingDto>? = null
)

@JsonClass(generateAdapter = false)
data class UsageMetadataDto(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null
)

@JsonClass(generateAdapter = false)
data class GeminiErrorDto(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)
