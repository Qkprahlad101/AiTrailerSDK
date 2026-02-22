package com.example.aitrailersdk.core.config

import kotlin.time.Duration

/**
 * Configuration for TrailerAI SDK
 *
 * This class holds all configurable options for the SDK.
 * Users can customize API keys, caching, timeouts, etc.
 */
data class TrailerAiConfig(
    val geminiApiKey: String? = null,
    val youtubeApiKey: String? = null,
    val cacheStrategy: CacheStrategy = CacheStrategy.MEMORY,
    val timeOut: Duration = Duration.parse("PT10S"), //10 seconds timeOut
    val enableLogging : Boolean = false,
    val maxRetries: Int = 3
)

/**
 * Cache Strategy Options
 */
enum class CacheStrategy {
    MEMORY,      // Store in memory (fast, but lost on app restart)
    DATABASE,     // Store in local database (persistent)
    NONE          // No caching
}