package com.example.data.ai

import com.example.data.local.SettingsRepository
import com.example.data.model.Note
import com.example.data.remote.GeminiAiProvider
import kotlinx.coroutines.flow.first

interface AiRepository {
    /**
     * Generates comprehensive new note content or drafts from a user topic/prompt.
     */
    suspend fun generateNoteContent(topicOrPrompt: String, tone: String = "informative and structured"): Result<String>

    /**
     * Generates a structured markdown outline for a given topic or meeting.
     */
    suspend fun generateOutline(topic: String): Result<String>

    /**
     * Brainstorms creative ideas, action items, or solutions for a given topic.
     */
    suspend fun brainstormIdeas(topic: String): Result<String>

    /**
     * Continues writing naturally from the existing note context.
     */
    suspend fun continueWriting(existingText: String): Result<String>

    /**
     * Summarizes note text into a clean executive summary.
     */
    suspend fun summarize(text: String): Result<String>

    /**
     * Summarizes note content into concise bullet points.
     */
    suspend fun summarizeInBullets(text: String): Result<String>

    /**
     * Summarizes note content into a single ultra-short punchy paragraph.
     */
    suspend fun summarizeConcise(text: String): Result<String>

    /**
     * Rewrites text in a specified style (e.g. professional, casual, concise).
     */
    suspend fun rewrite(text: String, style: String = "concise and clear"): Result<String>

    /**
     * Proofreads and corrects spelling, punctuation, and grammar.
     */
    suspend fun fixGrammar(text: String): Result<String>

    /**
     * Translates note text into the target language.
     */
    suspend fun translate(text: String, targetLanguage: String): Result<String>

    /**
     * Automatically extracts 3-5 relevant tags/hashtags from note title and content.
     */
    suspend fun autoTag(title: String, content: String): Result<List<String>>

    /**
     * Chooses the best fitting category for note content among existing categories.
     */
    suspend fun autoCategorize(title: String, content: String, existingCategories: List<String>): Result<String>

    /**
     * Executes arbitrary freeform natural language instructions on note text.
     */
    suspend fun executeNaturalLanguageInstruction(instruction: String, text: String): Result<String>

    /**
     * Semantically re-ranks local search candidate notes based on relevance to query.
     */
    suspend fun reRankCandidates(query: String, candidates: List<Note>): Result<List<String>>
}

class AiRepositoryImpl(
    private val provider: GeminiAiProvider,
    private val settingsRepository: SettingsRepository
) : AiRepository {

    private suspend fun getCredentials(): Pair<String, String>? {
        val settings = settingsRepository.settingsFlow.first()
        val resolvedKey = provider.resolveApiKey(settings.geminiApiKey)

        // If user explicitly turned off Gemini and there's no API key configured
        if (!settings.geminiEnabled && resolvedKey.isBlank()) {
            return null
        }

        if (resolvedKey.isBlank()) {
            return null
        }

        val targetModel = provider.sanitizeModelName(settings.geminiModel)
        return Pair(targetModel, resolvedKey)
    }

    override suspend fun generateNoteContent(topicOrPrompt: String, tone: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is not configured. Please enable Gemini and enter your API key in Settings."))

        val systemInstruction = """
            You are a helpful, organized personal notes assistant.
            Generate a well-structured, clear note in clean Markdown format with headings, bullet points, and key takeaways.
            Tone: $tone.
            Return ONLY the note content itself without conversational chatter.
        """.trimIndent()

        val prompt = "Create a complete, organized note about the following topic:\n\n$topicOrPrompt"
        return provider.generateText(model, apiKey, prompt, systemInstruction = systemInstruction, temperature = 0.7f)
    }

    override suspend fun generateOutline(topic: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is not configured. Please enable Gemini and enter your API key in Settings."))

        val systemInstruction = "You are an expert organizer. Generate clean, logical, hierarchical bulleted outlines with checklists where applicable."
        val prompt = "Create a structured, comprehensive outline for:\n\n$topic"
        return provider.generateText(model, apiKey, prompt, systemInstruction = systemInstruction, temperature = 0.5f)
    }

    override suspend fun brainstormIdeas(topic: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is not configured. Please enable Gemini and enter your API key in Settings."))

        val prompt = "Brainstorm 5 to 8 creative, actionable ideas or solutions for the following topic/problem:\n\n$topic\n\nFormat with bold titles and brief descriptions."
        return provider.generateText(model, apiKey, prompt, temperature = 0.8f)
    }

    override suspend fun continueWriting(existingText: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is not configured. Please enable Gemini and enter your API key in Settings."))

        val prompt = """
            Continue writing the following note seamlessly and coherently, maintaining the same tone, style, and flow:
            
            $existingText
            
            Return ONLY the continuation text to append.
        """.trimIndent()
        return provider.generateText(model, apiKey, prompt, temperature = 0.7f)
    }

    override suspend fun summarize(text: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is disabled or API key is missing. Please check Settings."))
        val prompt = "Provide a clean, structured summary of the following note content with main highlights and action points:\n\n$text"
        return provider.generateText(model, apiKey, prompt, temperature = 0.4f)
    }

    override suspend fun summarizeInBullets(text: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is disabled or API key is missing. Please check Settings."))
        val prompt = "Summarize the following note content into 3 to 6 clear, concise bullet points:\n\n$text"
        return provider.generateText(model, apiKey, prompt, temperature = 0.3f)
    }

    override suspend fun summarizeConcise(text: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is disabled or API key is missing. Please check Settings."))
        val prompt = "Summarize the following text in 1 or 2 concise, impactful sentences:\n\n$text"
        return provider.generateText(model, apiKey, prompt, temperature = 0.3f)
    }

    override suspend fun rewrite(text: String, style: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is disabled or API key is missing. Please check Settings."))
        val prompt = "Rewrite the following note text to be $style while preserving all factual points and core meaning:\n\n$text"
        return provider.generateText(model, apiKey, prompt, temperature = 0.6f)
    }

    override suspend fun fixGrammar(text: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is disabled or API key is missing. Please check Settings."))
        val prompt = "Proofread and fix any grammatical, punctuation, or spelling errors in the following text. Return ONLY the corrected text without explanations:\n\n$text"
        return provider.generateText(model, apiKey, prompt, temperature = 0.2f)
    }

    override suspend fun translate(text: String, targetLanguage: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is disabled or API key is missing. Please check Settings."))
        val prompt = "Translate the following text accurately and naturally into $targetLanguage. Return ONLY the translated text:\n\n$text"
        return provider.generateText(model, apiKey, prompt, temperature = 0.3f)
    }

    override suspend fun autoTag(title: String, content: String): Result<List<String>> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is disabled or API key is missing in Settings."))
        val prompt = """
            Based on the following note title and content, generate 3 to 5 relevant short hashtags (comma-separated, without '#' prefix, e.g. productivity, work, project).
            Title: $title
            Content: ${content.take(600)}
            Output only the comma-separated words.
        """.trimIndent()

        val result = provider.generateText(model, apiKey, prompt, temperature = 0.3f)
        return result.map { raw ->
            raw.split(",")
                .map { it.trim().removePrefix("#").lowercase() }
                .filter { it.isNotBlank() && it.length < 25 }
        }
    }

    override suspend fun autoCategorize(title: String, content: String, existingCategories: List<String>): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is disabled or API key is missing in Settings."))
        val catListStr = existingCategories.joinToString(", ")
        val prompt = """
            Given the note title: "$title"
            And content snippet: "${content.take(400)}"
            Choose the single best matching category from this existing list: [$catListStr].
            If none fit well, output "General".
            Respond with ONLY the exact category name.
        """.trimIndent()

        val result = provider.generateText(model, apiKey, prompt, temperature = 0.2f)
        return result.map { it.lines().firstOrNull()?.trim() ?: "General" }
    }

    override suspend fun executeNaturalLanguageInstruction(instruction: String, text: String): Result<String> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is disabled or API key is missing. Please enable Gemini and enter your API key in Settings."))

        val systemInstruction = """
            You are an expert AI writing assistant integrated into a mobile notes application.
            Perform the requested operation accurately according to the user's natural language instruction.
            Return ONLY the transformed or generated text.
            Do NOT include conversational filler, meta-talk, or prefixes like "Here is the rewritten text:".
        """.trimIndent()

        val prompt = """
            === INPUT TEXT ===
            $text
            === END INPUT TEXT ===

            User instruction: "$instruction"
        """.trimIndent()

        return provider.generateText(model, apiKey, prompt, systemInstruction = systemInstruction, temperature = 0.7f)
    }

    override suspend fun reRankCandidates(query: String, candidates: List<Note>): Result<List<String>> {
        val (model, apiKey) = getCredentials()
            ?: return Result.failure(IllegalStateException("Gemini AI is disabled or API key is missing in Settings."))

        if (candidates.isEmpty()) return Result.success(emptyList())
        if (candidates.size == 1) return Result.success(listOf(candidates.first().id))

        val candidatePreviews = candidates.take(15).mapIndexed { idx, note ->
            val preview = note.blocks.joinToString(" ") { it.content }.take(100)
            "[$idx] ID:${note.id} Title:${note.title} Content:$preview"
        }.joinToString("\n")

        val prompt = """
            User search query: "$query"
            Below are candidate notes:
            $candidatePreviews
            Rank the candidate note IDs from most relevant to least relevant to the user query.
            Return ONLY a comma-separated list of IDs in order of relevance (e.g. id1, id2, id3).
        """.trimIndent()

        val result = provider.generateText(model, apiKey, prompt, temperature = 0.2f)
        return result.map { raw ->
            val idList = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val existingIds = candidates.map { it.id }.toSet()
            val validRanked = idList.filter { it in existingIds }
            val unranked = candidates.map { it.id }.filterNot { it in validRanked }
            validRanked + unranked
        }
    }
}
