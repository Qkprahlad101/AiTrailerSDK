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
}
