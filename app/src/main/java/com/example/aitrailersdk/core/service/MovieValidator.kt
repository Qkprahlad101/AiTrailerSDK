package com.example.aitrailersdk.core.service

import com.example.aitrailersdk.core.model.TrailerRequest

/**
 * Interface for validating movies against an external source (like OMDB).
 * The client application should implement this to provide movie details.
 */
interface MovieValidator {
    /**
     * Validates a movie title and returns full details if it exists.
     * @param title The movie title to validate.
     * @return A TrailerRequest with full details, or null if the movie is not found.
     */
    suspend fun validateAndGetDetails(title: String): TrailerRequest?
}
