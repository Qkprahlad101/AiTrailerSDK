package com.example.aitrailersdk

import com.example.aitrailersdk.core.config.TrailerAiConfig
import com.example.aitrailersdk.core.model.TrailerRequest
import com.example.aitrailersdk.core.model.TrailerResult
import com.example.aitrailersdk.core.model.TrailerSource
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TrailerAITest {

    @Test
    fun testInitialization() {
        val trailerAI = TrailerAi.initialize(TrailerAiConfig())
        // Test passes if no exception thrown
        assert(true)
    }

    @Test
    fun testTrailerRequestValidation() {
        // Valid request
        val validRequest = TrailerRequest("Inception", "2010", "Christopher Nolan")
        assert(validRequest.movieTitle.isNotBlank())

        // Invalid request (blank title)
        try {
            TrailerRequest("")
            assert(false) { "Should have thrown exception for blank title" }
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testConfigValidation() = runTest {
        // Valid config
        val validConfig = TrailerAiConfig(enableLogging = true)
        assert(validConfig.maxRetries >= 0)
        assert(validConfig.timeOut.isPositive())

        // Invalid config (negative retries)
        try {
            TrailerAiConfig(maxRetries = -1)
            assert(false) { "Should have thrown exception for negative retries" }
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testBasicSearch() = runTest {
        val trailerAI = TrailerAi.initialize(
            TrailerAiConfig(
                enableLogging = true,
                geminiApiKey = "test-key" // This will fail but test the flow
            )
        )

        val result = trailerAI.findTrailer(
            TrailerRequest("Inception", "2010", "Christopher Nolan")
        )

        // Should return NotFound since we're using a fake API key
        assert(result is com.example.aitrailersdk.core.model.TrailerResult.NotFound ||
                result is com.example.aitrailersdk.core.model.TrailerResult.Error)
    }

    @Test
    fun testPatternMatchingFallback() = runTest {
        // Test without API keys to test pattern matching
        val trailerAi = TrailerAi.initialize(
            TrailerAiConfig(
                enableLogging = true,
                geminiApiKey = null, // No Gemini
                youtubeApiKey = null   // No YouTube API
            )
        )

        // Test movies we know in pattern matching
        val knownMovies = listOf(
            "Inception",
            "The Dark Knight",
            "Interstellar",
            "Avatar",
            "Avengers",
            "Joker"
        )

        knownMovies.forEach { movieTitle ->
            println("\n=== Testing Pattern Matching: $movieTitle ===")

            val result = trailerAi.findTrailer(TrailerRequest(movieTitle))

            when (result) {
                is TrailerResult.Success -> {
                    println("✅ PATTERN MATCH: ${result.url}")
                    println("   Source: ${result.source}")
                    println("   Confidence: ${result.confidence}")

                    // Should be pattern matching with low confidence
                    assert(result.source == TrailerSource.PATTERN_MATCHING)
                    assert(result.confidence == 0.4f)
                }
                is TrailerResult.Error -> {
                    println("❌ ERROR: ${result.exception.message}")
                }
                TrailerResult.NotFound -> {
                    println("❌ NOT FOUND")
                }
            }
        }
    }
}