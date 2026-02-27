package com.example.aitrailersdk

import com.example.aitrailersdk.core.config.TrailerAiConfig
import com.example.aitrailersdk.core.impl.GeminiTrailerService
import com.example.aitrailersdk.core.impl.PatternMatchingService
import com.example.aitrailersdk.core.impl.YouTubeTrailerService
import com.example.aitrailersdk.core.model.TrailerRequest
import com.example.aitrailersdk.core.model.TrailerResult
import com.example.aitrailersdk.core.model.TrailerSource
import com.example.aitrailersdk.BuildConfig.GEMINI_API_KEY
import com.example.aitrailersdk.BuildConfig.YOUTUBE_API_KEY
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TrailerAITest {

    @Test
    fun testInitialization() {
        val trailerAI = TrailerAi.initialize(TrailerAiConfig())
        assert(true)
    }

    @Test
    fun testTrailerRequestValidation() {
        val validRequest = TrailerRequest("Inception", "2010", "Christopher Nolan")
        assert(validRequest.movieTitle.isNotBlank())

        try {
            TrailerRequest("")
            assert(false) { "Should have thrown exception for blank title" }
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testConfigValidation() {
        runBlocking {
            val validConfig = TrailerAiConfig(enableLogging = true)
            assert(validConfig.maxRetries >= 0)
            assert(validConfig.timeOut.isPositive())

            try {
                TrailerAiConfig(maxRetries = -1)
                assert(false) { "Should have thrown exception for negative retries" }
            } catch (e: IllegalArgumentException) {
                // Expected
            }
        }
    }

    @Test
    fun testGeminiSuccessWithProvider() {
        runBlocking {
            // 1. Create a fake provider that mimics Gemini's response
            val fakeProvider: suspend (String) -> String? = { 
                "Sure! Here is the official trailer link: https://www.youtube.com/watch?v=yR7A-Y_mYCc hope you enjoy it!" 
            }

            // 2. Initialize Service with the Fake Provider
            val config = TrailerAiConfig(geminiApiKey = GEMINI_API_KEY, enableLogging = true)
            val geminiService = GeminiTrailerService(config, fakeProvider)

            // 3. Test
            val result = geminiService.findTrailer(TrailerRequest("Help!", "1965"))

            // 4. Verify that our Regex correctly extracted the URL from the provider's text
            assert(result is TrailerResult.Success) { "Expected Success, but got $result" }
            val success = result as TrailerResult.Success
            assert(success.url == "https://www.youtube.com/watch?v=yR7A-Y_mYCc")
            assert(success.source == TrailerSource.GEMINI_AI)
            println("✅ Gemini Provider Success: ${success.url}")
        }
    }

    @Test
    fun testFallbackChain() {
        runBlocking {
            val trailerAi = TrailerAi.initialize(
                TrailerAiConfig(
                    enableLogging = true,
                    geminiApiKey = GEMINI_API_KEY,
                    youtubeApiKey = YOUTUBE_API_KEY
                )
            )

            val result = trailerAi.findTrailer(TrailerRequest("Inception"))

            assert(result is TrailerResult.Success)
            val success = result as TrailerResult.Success
            assert(success.source == TrailerSource.PATTERN_MATCHING)
            println("✅ Fallback Chain Success: ${success.url} via ${success.source}")
        }
    }

    @Test
    fun debugGeminiService() {
        runBlocking {
            val config = TrailerAiConfig(
                geminiApiKey = GEMINI_API_KEY,
                enableLogging = true
            )
            val service = GeminiTrailerService(config)

            println("\n--- Starting Gemini Real-Key Debug ---")
            try {
                val result = service.findTrailer(TrailerRequest("Help!", "1965"))
                when(result) {
                    is TrailerResult.Success -> println("✅ SUCCESS: ${result.url}")
                    is TrailerResult.Error -> {
                        println("❌ ERROR: ${result.exception.message}")
                        result.exception.cause?.printStackTrace()
                    }
                    TrailerResult.NotFound -> println("∅ NOT FOUND")
                }
            } catch (e: Exception) {
                println("💥 FATAL EXCEPTION: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    @Test
    fun debugYouTubeService() {
        runBlocking {
            val config = TrailerAiConfig(
                youtubeApiKey = YOUTUBE_API_KEY,
                enableLogging = true
            )
            val service = YouTubeTrailerService(config)
            val request = TrailerRequest("Inception", "2010")

            println("\n--- Starting YouTube Debug ---")
            val result = service.findTrailer(request)

            when(result) {
                is TrailerResult.Success -> {
                    println("SUCCESS: URL Found -> ${result.url}")
                    println("Confidence: ${result.confidence}")
                }
                is TrailerResult.Error -> println("ERROR: ${result.exception.message}")
                TrailerResult.NotFound -> println("NOT_FOUND: No official trailer matched the filter.")
            }
        }
    }

    @Test
    fun debugPatternMatchingService() {
        runBlocking {
            val service = PatternMatchingService(TrailerAiConfig(enableLogging = true))

            val testMovies = listOf("Inception", "The Dark Knight", "Dune", "Avatar")

            println("\n--- Starting Pattern Matching Debug ---")
            testMovies.forEach { title ->
                val result = service.findTrailer(TrailerRequest(title))
                if (result is TrailerResult.Success) {
                    println("MATCH: '$title' -> ${result.url}")
                } else {
                    println("FAIL: No pattern found for '$title'")
                }
            }
        }
    }

}
