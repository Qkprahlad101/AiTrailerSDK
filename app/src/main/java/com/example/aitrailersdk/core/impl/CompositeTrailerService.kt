package com.example.aitrailersdk.core.impl

import com.example.aitrailersdk.core.config.TrailerAiConfig
import com.example.aitrailersdk.core.model.TrailerRequest
import com.example.aitrailersdk.core.model.TrailerResult
import com.example.aitrailersdk.core.service.TrailerService
import kotlin.io.println

class CompositeTrailerService(
    private val config: TrailerAiConfig
): TrailerService {
    override suspend fun findTrailer(request: TrailerRequest): TrailerResult {
        if(config.enableLogging) {
            println("TrailerAI: Searching trailer for '${request.movieTitle}' (${request.year})")
        }

        val sources = listOf(
            GeminiTrailerService(config),
            YouTubeTrailerService(config),
            PatternMatchingService(config)
        )

        for (service in sources) {
            try {
                val result = service.findTrailer(request)
                when (result) {
                    is TrailerResult.Success -> {
                        if (config.enableLogging) {
                            println("TrailerAI: Found trailer via ${result.source}: ${result.url}")
                        }
                        return result
                    }
                    is TrailerResult.Error -> {
                        if (config.enableLogging) {
                            println("TrailerAI: Error in ${service.javaClass.simpleName}: ${result.exception.message}")
                        }
                        // Continue to next source
                    }
                    TrailerResult.NotFound -> {
                        if (config.enableLogging) {
                            println("TrailerAI: No trailer found via ${service.javaClass.simpleName}")
                        }
                        // Continue to next source
                    }
                }
            } catch (e: Exception) {
                if (config.enableLogging) {
                    println("TrailerAI: Exception in ${service.javaClass.simpleName}: ${e.message}")
                }
                // Continue to next source
            }
        }




        // For now, return NotFound until we implement all services
        return TrailerResult.NotFound
    }
}