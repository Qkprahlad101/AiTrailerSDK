package com.example.aitrailersdk.core.service

import com.example.aitrailersdk.core.model.TrailerRequest
import com.example.aitrailersdk.core.model.TrailerResult

/**
 * Interface for trailer fetching services
 */
interface TrailerService {

    /**
     * Find trailer for a given movie
     * @param request Trailer request with movie details
     * @return TrailerResult containing trailer URL or error information
     */
    suspend fun findTrailer(request: TrailerRequest) : TrailerResult

    /**
     * Suggest relevant Movies and provide trailers, based on a list of movies provided in the input.
     * This version uses a MovieValidator to verify suggested movies against an external source.
     * 
     * @param inputMovies List of movies to base suggestions on.
     * @param validator Implementation to validate and get full details for suggested movies.
     * @return List of Pairs containing movie details and their trailer results.
     */
    suspend fun suggestRelevantMovies(
        inputMovies: List<TrailerRequest>,
        validator: MovieValidator
    ) : List<Pair<TrailerRequest, TrailerResult>>
}
