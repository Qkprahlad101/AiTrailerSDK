package com.example.aitrailersdk.core.impl

import android.util.Log
import com.example.aitrailersdk.core.config.TrailerAiConfig
import com.example.aitrailersdk.core.exception.TrailerException
import com.example.aitrailersdk.core.model.TrailerRequest
import com.example.aitrailersdk.core.model.TrailerResult
import com.example.aitrailersdk.core.model.TrailerSource
import com.example.aitrailersdk.core.service.MovieValidator
import com.example.aitrailersdk.core.service.TrailerService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.supervisorScope
import java.util.Collections

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
                        modelName = GEMINI_MODEL,
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
                    Log.d(TAG, "Gemini Trailer Response: '$text'")
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
                Log.e(TAG, "Gemini Error (${e.javaClass.simpleName}): ${e.message}")
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

    /**
     * Suggests relevant movies based on a list of input movies.
     * Returns partial results if some validations fail or if the process is timed out/cancelled.
     * Optimised to fetch trailer URLs in the same AI call to reduce API usage.
     */
    suspend fun suggestRelevantMovies(
        inputMovies: List<TrailerRequest>,
        validator: MovieValidator
    ): List<Pair<TrailerRequest, TrailerResult>> {
        if (config.geminiApiKey.isNullOrBlank() && aiProvider == null) {
            return emptyList()
        }
        
        val results = Collections.synchronizedList(mutableListOf<Pair<TrailerRequest, TrailerResult>>())
        
        return try {
            val promptText = buildSuggestionPrompt(inputMovies)

            val text = try {
                withTimeout(config.timeOut) {
                    if (aiProvider != null) {
                        aiProvider.invoke(promptText)
                    } else {
                        val model = GenerativeModel(
                            modelName = GEMINI_MODEL,
                            apiKey = config.geminiApiKey!!,
                            generationConfig = generationConfig {
                                temperature = 0.7f
                            }
                        )
                        val response = model.generateContent(content { text(promptText) })
                        response.text?.trim()
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                if (config.enableLogging) Log.e(TAG, "Gemini suggestion request failed: ${e.message}")
                null
            }

            if (text.isNullOrBlank()) return emptyList()

            if (config.enableLogging) {
                Log.d(TAG, "Gemini Suggestion Response: '$text'")
            }

            // Parse suggestions containing both Title and Trailer URL
            val suggestions = parseSuggestionResponseWithTrailers(text)
            Log.d(TAG, "Parsed suggestions: $suggestions")

            supervisorScope {
                suggestions.take(5).forEach { (title, trailerUrl) ->
                    launch {
                        try {
                            val fullDetails = validator.validateAndGetDetails(title)
                            if (fullDetails != null) {
                                // Use trailer URL from suggestion if available, otherwise fallback to finding it
                                val trailerResult = if (!trailerUrl.isNullOrBlank() && trailerUrl != "NO_TRAILER") {
                                    TrailerResult.Success(trailerUrl, TrailerSource.GEMINI_AI, 0.9f)
                                } else {
                                    findTrailer(fullDetails)
                                }
                                
                                synchronized(results) {
                                    if (results.size < 5) {
                                        results.add(fullDetails to trailerResult)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            if (config.enableLogging) Log.e(TAG, "Error processing suggested movie '$title': ${e.message}")
                        }
                    }
                }
            }
            
            synchronized(results) { results.toList().take(5) }
            
        } catch (e: Exception) {
            if (config.enableLogging) {
                Log.e(TAG, "Suggestion process interrupted: ${e.message}")
            }
            synchronized(results) { results.toList().take(5) }
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
        return """
            Based on these movies: $inputMovies
            Suggest 10 similar highly-rated movies. 
            For each suggested movie, find its official YouTube trailer URL.
            Respond ONLY with a list where each line is in this format:
            Movie: [Movie Title] | Trailer: [YouTube URL]
            If no trailer is found, use "NO_TRAILER" for the URL.
            No numbering, no descriptions.
        """.trimIndent()
    }

    private fun parseSuggestionResponseWithTrailers(text: String): List<Pair<String, String>> {
        return text.lines()
            .filter { it.contains("|") }
            .mapNotNull { line ->
                try {
                    val parts = line.split("|")
                    if (parts.size >= 2) {
                        val title = parts[0].replace("Movie:", "", ignoreCase = true).trim()
                        val trailer = parts[1].replace("Trailer:", "", ignoreCase = true).trim()
                        if (title.isNotEmpty()) title to trailer else null
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
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

    companion object{
        private const val TAG = "GeminiTrailerService"
        private const val GEMINI_MODEL = "gemini-2.5-flash"
    }
}
