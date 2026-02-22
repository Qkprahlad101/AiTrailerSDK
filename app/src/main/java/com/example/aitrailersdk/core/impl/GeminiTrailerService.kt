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
/**
 * Gemini AI service for finding trailers using Google's Gemini model
 *
 * This is our primary source because:
 * - Free tier available (1,500 requests/day)
 * - Understands natural language queries
 * - Can distinguish between official and fan trailers
 */
class GeminiTrailerService(
    private val config: TrailerAiConfig
): TrailerService {
    override suspend fun findTrailer(request: TrailerRequest): TrailerResult {
        return try {
            withTimeout(config.timeOut) {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-pro",
                    apiKey = config.geminiApiKey ?: throw TrailerException.APIKeyException("Gemini API key not provided")
                )

                val prompt = buildPrompt(request)
                val response = generativeModel.generateContent(prompt)
                val text = response.text?.trim() ?: throw TrailerException.ParseException("Empty response from Gemini")

                val trailerUrl = extractYouTubeUrl(text)
                if (trailerUrl != null) {
                    TrailerResult.Success(trailerUrl, TrailerSource.GEMINI_AI, 0.8f)
                } else {
                    TrailerResult.NotFound
                }
            }
        } catch (e: Exception) {
            when (e) {
                is TrailerException -> TrailerResult.Error(e)
                is TimeoutCancellationException -> TrailerResult.Error(TrailerException.TimeoutException("Gemini request timed out", e))
                else -> TrailerResult.Error(TrailerException.NetworkException("Gemini service failed", e))
            }
        } as TrailerResult
    }

    private fun buildPrompt(request: TrailerRequest): String {
        val baseInfo = listOfNotNull(
            request.movieTitle,
            request.year?.let { "($it)" },
            request.director?.let { "directed by $it" }
        ).joinToString(" ")

        return """
            Given this movie: $baseInfo
            Find official YouTube trailer URL.
            
            Respond ONLY with YouTube URL in format: https://youtube.com/watch?v=VIDEO_ID
            If multiple trailers exist, prefer official theatrical trailer.
            If no trailer found, respond with: NO_TRAILER_FOUND
            
            Examples:
            - For "Inception (2010)" respond: https://youtube.com/watch?v=YoHD9XEInc0
            - For "The Dark Knight" respond: https://youtube.com/watch?v=EXeTwQWrcwY
        """.trimIndent()
    }

    private fun extractYouTubeUrl(text: String): String? {
        return when {
            text.contains("NO_TRAILER_FOUND") -> null
            text.contains("youtube.com/watch?v=") -> {
                val regex = Regex("https://youtube\\.com/watch\\?v=[a-zA-Z0-9_-]+")
                regex.find(text)?.value
            }
            text.contains("youtu.be/") -> {
                val regex = Regex("https://youtu\\.be/[a-zA-Z0-9_-]+")
                regex.find(text)?.value?.replace("youtu.be/", "youtube.com/watch?v=")
            }
            else -> null
        }
    }
}