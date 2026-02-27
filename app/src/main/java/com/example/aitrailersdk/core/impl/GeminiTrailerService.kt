package com.example.aitrailersdk.core.impl

import com.example.aitrailersdk.core.config.TrailerAiConfig
import com.example.aitrailersdk.core.exception.TrailerException
import com.example.aitrailersdk.core.model.TrailerRequest
import com.example.aitrailersdk.core.model.TrailerResult
import com.example.aitrailersdk.core.model.TrailerSource
import com.example.aitrailersdk.core.service.MovieValidator
import com.example.aitrailersdk.core.service.TrailerService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

/**
 * Gemini AI service for finding trailers and suggesting movies using Google's Gemini model.
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
                val promptText = buildTrailerPrompt(request)

                val text = if (aiProvider != null) {
                    aiProvider.invoke(promptText)
                } else {
                    val model = GenerativeModel(
                        modelName = "gemini-2.5-flash",
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
                    println("TrailerAI: Gemini Trailer Response: '$text'")
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

    suspend fun suggestRelevantMovies(
        inputMovies: List<TrailerRequest>,
        validator: MovieValidator
    ): List<Pair<TrailerRequest, TrailerResult>> {
        if (config.geminiApiKey.isNullOrBlank() && aiProvider == null) {
            return emptyList()
        }

        return try {
            withTimeout(config.timeOut * 2) { // Allow more time for suggestions
                val promptText = buildSuggestionPrompt(inputMovies)

                val text = if (aiProvider != null) {
                    aiProvider.invoke(promptText)
                } else {
                    val model = GenerativeModel(
                        modelName = "gemini-2.5-flash",
                        apiKey = config.geminiApiKey!!,
                        generationConfig = generationConfig {
                            temperature = 0.7f // Slightly higher for creativity in suggestions
                        }
                    )
                    val response = model.generateContent(content { text(promptText) })
                    response.text?.trim()
                }

                if (text.isNullOrBlank()) return@withTimeout emptyList()

                if (config.enableLogging) {
                    println("TrailerAI: Gemini Suggestion Response: '$text'")
                }

                val suggestedTitles = parseSuggestionResponse(text)
                val results = mutableListOf<Pair<TrailerRequest, TrailerResult>>()

                for (title in suggestedTitles) {
                    if (results.size >= 10) break

                    // Validate against external source (OMDB via App)
                    val fullDetails = validator.validateAndGetDetails(title)
                    if (fullDetails != null) {
                        // Find trailer for the validated movie
                        val trailerResult = findTrailer(fullDetails)
                        results.add(fullDetails to trailerResult)
                    }
                }
                results
            }
        } catch (e: Exception) {
            if (config.enableLogging) {
                println("TrailerAI: Suggestion Error: ${e.message}")
            }
            emptyList()
        }
    }

    private fun buildTrailerPrompt(request: TrailerRequest): String {
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

    private fun buildSuggestionPrompt(inputMovies: List<TrailerRequest>): String {
        val titles = inputMovies.joinToString(", ") { it.movieTitle }
        return """
            Based on these movies: $titles
            Suggest 15 similar highly-rated movies. 
            Respond ONLY with a comma-separated list of movie titles.
            No numbering, no descriptions, just titles.
        """.trimIndent()
    }

    private fun parseSuggestionResponse(text: String): List<String> {
        return text.split(",")
            .map { it.trim().trimIndent() }
            .filter { it.isNotBlank() }
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
