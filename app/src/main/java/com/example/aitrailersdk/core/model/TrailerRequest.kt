package com.example.aitrailersdk.core.model

/**
 * Request model for finding movie trailers
 *
 * @param movieTitle The title of the movie (required)
 * @param year Release year for better accuracy (optional)
 * @param director Director name for better accuracy (optional)
 * @param genre Movie genre for better accuracy (optional)
 */
data class TrailerRequest(
    val movieTitle: String,
    val year: String? = null,
    val director: String? = null,
    val genre: String? = null
) {
    init {
        require(movieTitle.isNotBlank()) {"Movie Title cannot be blank"}
    }
}
