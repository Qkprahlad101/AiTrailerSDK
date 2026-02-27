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

/**
 * Pattern matching service that uses common trailer URL patterns
 *
 * This is our last resort fallback service.
 * It uses a small database of known trailer URLs.
 */
class PatternMatchingService(
    private val config: TrailerAiConfig
) : TrailerService {

    // Common trailer video IDs for popular movies (this would be expanded)
    private val knownTrailers = mapOf(
        "inception" to "YoHD9XEInc0",
        "the dark knight" to "EXeTwQWrcwY",
        "interstellar" to "2LqzF5WauAw",
        "avatar" to "5PSNL1qE6VY",
        "avengers" to "eOrNdBpGMv8",
        "joker" to "zAGVQLHvwOY",
        "parasite" to "5xH0HfJHsaY",
        "1917" to "UcmZNQ_8y3Y",
        "dune" to "Way9Dexny3w"
    )

    override suspend fun findTrailer(request: TrailerRequest): TrailerResult {
        return try {
            withTimeout(config.timeOut) {
                val trailerUrl = findTrailerByPattern(request)
                if (trailerUrl != null) {
                    TrailerResult.Success(trailerUrl, TrailerSource.PATTERN_MATCHING, 0.4f)
                } else {
                    TrailerResult.NotFound
                }
            }
        } catch (e: Exception) {
            when (e) {
                is TrailerException -> TrailerResult.Error(e)
                is TimeoutCancellationException -> TrailerResult.Error(TrailerException.TimeoutException("Pattern matching timed out", e))
                else -> TrailerResult.Error(TrailerException.NetworkException("Pattern matching failed", e))
            }
        } as TrailerResult
    }

    private fun findTrailerByPattern(request: TrailerRequest): String? {
        val movieKey = request.movieTitle.lowercase().trim()

        // Try exact match first
        knownTrailers[movieKey]?.let { return "https://youtube.com/watch?v=$it" }

        // Try partial match
        knownTrailers.keys.forEach { key ->
            if (movieKey.contains(key) || key.contains(movieKey)) {
                knownTrailers[key]?.let { return "https://youtube.com/watch?v=$it" }
            }
        }

        return null
    }
}
