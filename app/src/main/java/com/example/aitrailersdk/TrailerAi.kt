package com.example.aitrailersdk

import com.example.aitrailersdk.core.config.TrailerAiConfig
import com.example.aitrailersdk.core.impl.CompositeTrailerService
import com.example.aitrailersdk.core.model.TrailerRequest
import com.example.aitrailersdk.core.model.TrailerResult
import com.example.aitrailersdk.core.service.MovieValidator
import com.example.aitrailersdk.core.service.TrailerService

/**
 * Main entry point for TrailerAI SDK
 *
 * This is the public API that consumers will interact with.
 * It follows a builder pattern with simple initialization.
 *
 * Usage:
 * val trailerAI = TrailerAi.initialize(TrailerAiConfig(enableLogging = true))
 * val result = trailerAI.findTrailer(TrailerRequest("Inception", "2010"))
 */
class TrailerAi private constructor(
    private val config: TrailerAiConfig,
    private val service: TrailerService
){

    /**
     * Find trailer for a single movie.
     */
    suspend fun findTrailer(request: TrailerRequest): TrailerResult {
        return service.findTrailer(request)
    }

    /**
     * Suggest relevant movies based on an input list and provide their trailers.
     * It uses a validator (e.g., OMDB) to ensure suggestions are real and fetch full details.
     * 
     * @param request Input movies to base suggestions on.
     * @param validator Implementation to verify movies against your app's data source.
     * @return List of Pairs containing the full movie details and its trailer result.
     */
    suspend fun suggestRelevantMovies(
        request: List<TrailerRequest>,
        validator: MovieValidator
    ): List<Pair<TrailerRequest, TrailerResult>> {
        return service.suggestRelevantMovies(request, validator)
    }

    companion object {
        /**
         * Initialize TrailerAI with configuration
         * @param config SDK configuration options
         * @return TrailerAI instance ready to use
         */
        fun initialize(config: TrailerAiConfig = TrailerAiConfig()) : TrailerAi {
            val service = CompositeTrailerService(config)
            return TrailerAi(config, service)
        }
    }
}
