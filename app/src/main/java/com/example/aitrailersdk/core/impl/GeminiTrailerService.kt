package com.example.aitrailersdk.core.impl

import com.example.aitrailersdk.core.config.TrailerAiConfig
import com.example.aitrailersdk.core.exception.TrailerException
import com.example.aitrailersdk.core.model.TrailerRequest
import com.example.aitrailersdk.core.model.TrailerResult
import com.example.aitrailersdk.core.model.TrailerSource
import com.example.aitrailersdk.core.service.TrailerService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

/**
 * Gemini AI service for finding trailers using Google's Gemini model.
 * 
 * This service uses the 'gemini-1.5-flash' model which is optimized for speed
 * and has a generous free tier for development.
 */
class GeminiTrailerService(
    private val config: TrailerAiConfig,
    private val aiProvider: (suspend (String) -> String?)? = null // Provider for testing
): TrailerService {

    override suspend fun findTrailer(request: TrailerRequest): TrailerResult {
        if (config.geminiApiKey.isNullOrBlank() && aiProvider == null) {
            return TrailerResult.Error(TrailerException.APIKeyException("Gemini API key not provided"))
        }

        return try {
            withTimeout(config.timeOut) {
                val promptText = buildPrompt(request)

                val text = if (aiProvider != null) {
                    aiProvider.invoke(promptText)
                } else {
                    val model = GenerativeModel(
                        modelName = "gemini-2.5-flash",               // ← changed here (recommended)
                        // or "gemini-2.5-flash-lite"
                        // or "gemini-3-flash-preview" for newest fast model
                        apiKey = config.geminiApiKey!!,
                        generationConfig = generationConfig {
                            temperature = 0.1f
                            topK = 1
                            topP = 1f
                        }
                    )
                    val response = model.generateContent(content { text(promptText) })
                    response.text?.trim()
                }

                if (text.isNullOrBlank()) {
                    throw TrailerException.ParseException("Empty response from Gemini")
                }

                if (config.enableLogging) {
                    println("TrailerAI: Gemini Response: '$text'")
                }

                val trailerUrl = extractYouTubeUrl(text)
                if (trailerUrl != null) {
                    TrailerResult.Success(trailerUrl, TrailerSource.GEMINI_AI, 0.9f)
                } else {
                    TrailerResult.NotFound
                }
            }
        } catch (e: Exception) {
            if (config.enableLogging) {
                println("TrailerAI: Gemini Error (${e.javaClass.simpleName}): ${e.message}")
            }
            when (e) {
                is TrailerException -> TrailerResult.Error(e)
                is TimeoutCancellationException -> TrailerResult.Error(TrailerException.TimeoutException("Gemini request timed out", e))
                else -> {
                    val msg = e.message ?: "Unknown generative AI error"
                    TrailerResult.Error(TrailerException.NetworkException("Gemini service failed: $msg", e))
                }
            }
        }
    }
    private fun buildPrompt(request: TrailerRequest): String {
        val baseInfo = listOfNotNull(
            request.movieTitle,
            request.year?.let { "($it)" },
            request.director?.let { "directed by $it" },
            request.description?.let { "Summary: $it" }
        ).joinToString(" ")

        return """
            You are a movie trailer expert. Find the official YouTube trailer URL for the following movie:
            
            Movie: $baseInfo
            
            Respond with ONLY the YouTube URL in this format: https://www.youtube.com/watch?v=VIDEO_ID
            If no trailer is found, respond exactly with: NO_TRAILER_FOUND
        """.trimIndent()
    }

    internal fun extractYouTubeUrl(text: String): String? {
        if (text.contains("NO_TRAILER_FOUND", ignoreCase = true)) return null

        val youtubeRegex = Regex(
            "(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})",
            RegexOption.IGNORE_CASE
        )
        val match = youtubeRegex.find(text)
        if (match != null) {
            val videoId = match.groupValues[4]
            return "https://www.youtube.com/watch?v=$videoId"
        }
        return null
    }
}
