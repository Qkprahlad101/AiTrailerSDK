package com.example.aitrailersdk.core.model

import com.example.aitrailersdk.core.exception.TrailerException


/**
 * Sealed class representing trailer search results
 *
 * This ensures all possible outcomes are handled by the consumer.
 */
sealed class TrailerResult {

    data class Success(
        val url: String,
        val source: TrailerSource, // Which source found it
        val confidence: Float
    ) : TrailerResult() {
        init {
            require(url.isNotBlank()) {"URL cannot be blank."}
            require(confidence in 0f..1f)
        }
    }

    /**
     * Error occured while searching
     */
    data class Error(val exception: TrailerException)

    /**
     * No Trailer found for the given movie
     */
    object NotFound: TrailerResult()
}

enum class TrailerSource{
    GEMINI_AI,
    YOUTUBE_API,
    PATTERN_MATCHING,
    USER_ASSSISTED
}