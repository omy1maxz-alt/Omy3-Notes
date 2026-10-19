package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiServiceTest {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun testGenerateContentRequestSerialization() {
        val request = GenerateContentRequest(
            contents = listOf(
                ContentDto(
                    parts = listOf(PartDto(text = "Summarize this note.")),
                    role = "user"
                )
            ),
            generationConfig = GenerationConfigDto(temperature = 0.4f),
            systemInstruction = ContentDto(
                parts = listOf(PartDto(text = "You are a notes summarizer."))
            )
        )

        val adapter = moshi.adapter(GenerateContentRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("Summarize this note."))
        assertTrue(json.contains("You are a notes summarizer."))
        assertTrue(json.contains("0.4"))
    }

    @Test
    fun testGenerateContentResponseDeserialization() {
        val json = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "• Key point 1\n• Key point 2\n• Action item 1"
                                }
                            ],
                            "role": "model"
                        },
                        "finishReason": "STOP",
                        "index": 0
                    }
                ],
                "usageMetadata": {
                    "promptTokenCount": 25,
                    "candidatesTokenCount": 18,
                    "totalTokenCount": 43
                }
            }
        """.trimIndent()

        val adapter = moshi.adapter(GenerateContentResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertNotNull(response?.candidates)
        assertEquals(1, response?.candidates?.size)
        val text = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        assertNotNull(text)
        assertTrue(text!!.contains("Key point 1"))
        assertEquals(43, response?.usageMetadata?.totalTokenCount)
    }

    @Test
    fun testProviderSanitizeModelName() {
        val provider = GeminiAiProvider()

        // Prohibited model fallback
        assertEquals(GeminiAiProvider.DEFAULT_MODEL, provider.sanitizeModelName("gemini-1.5-flash"))
        assertEquals(GeminiAiProvider.DEFAULT_MODEL, provider.sanitizeModelName("gemini-2.0-flash"))
        assertEquals(GeminiAiProvider.DEFAULT_MODEL, provider.sanitizeModelName(""))

        // Valid model preservation
        assertEquals("gemini-3.5-flash", provider.sanitizeModelName("gemini-3.5-flash"))
        assertEquals("gemini-3.1-pro-preview", provider.sanitizeModelName("gemini-3.1-pro-preview"))
    }
}
