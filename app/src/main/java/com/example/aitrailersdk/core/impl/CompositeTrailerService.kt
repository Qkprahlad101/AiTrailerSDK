package com.example.aitrailersdk.core.impl

import com.example.aitrailersdk.core.config.TrailerAiConfig
import com.example.aitrailersdk.core.model.TrailerRequest
import com.example.aitrailersdk.core.model.TrailerResult
import com.example.aitrailersdk.core.service.TrailerService

class CompositeTrailerService(
    private val config: TrailerAiConfig
): TrailerService {
    override suspend fun findTrailer(request: TrailerRequest): TrailerResult {
        if(config.enableLogging) {
            println("TrailerAI: Searching trailer for '${request.movieTitle}' (${request.year})")
        }
        // For now, return NotFound until we implement all services
        return TrailerResult.NotFound
    }
}