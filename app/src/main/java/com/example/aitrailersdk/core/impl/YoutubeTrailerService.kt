package com.example.aitrailersdk.core.impl

import com.example.aitrailersdk.core.config.TrailerAiConfig
import com.example.aitrailersdk.core.exception.TrailerException
import com.example.aitrailersdk.core.model.TrailerRequest
import com.example.aitrailersdk.core.model.TrailerResult
import com.example.aitrailersdk.core.model.TrailerSource
import com.example.aitrailersdk.core.service.TrailerService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * YouTube Data API service for finding trailers
 *
 * This is our fallback source when Gemini AI fails.
 * It's more reliable but less intelligent than AI.
 */
class YouTubeTrailerService(
    private val config: TrailerAiConfig
) : TrailerService {

    private val youtubeApi: YouTubeApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YouTubeApi::class.java)
    }

    override suspend fun findTrailer(request: TrailerRequest): TrailerResult {
        if (config.youtubeApiKey == null) {
            return TrailerResult.NotFound // Skip if no API key
        }

        return try {
            withTimeout(config.timeOut) {
                val query = buildSearchQuery(request)
                val response = youtubeApi.searchVideos(
                    apiKey = config.youtubeApiKey,
                    query = query,
                    maxResults = 5
                )

                val trailer = response.items.find { isOfficialTrailer(it.snippet.title) }
                if (trailer != null) {
                    val videoUrl = "https://youtube.com/watch?v=${trailer.id.videoId}"
                    TrailerResult.Success(videoUrl, TrailerSource.YOUTUBE_API, 0.7f)
                } else {
                    TrailerResult.NotFound
                }
            }
        } catch (e: Exception) {
            when (e) {
                is TrailerException -> TrailerResult.Error(e)
                is TimeoutCancellationException -> TrailerResult.Error(TrailerException.TimeoutException("YouTube API request timed out", e))
                else -> TrailerResult.Error(TrailerException.NetworkException("YouTube API service failed", e))
            }
        } as TrailerResult
    }

    private fun buildSearchQuery(request: TrailerRequest): String {
        val parts = mutableListOf(request.movieTitle)
        request.year?.let { parts.add(it) }
        parts.add("official trailer")
        return parts.joinToString(" ")
    }

    private fun isOfficialTrailer(title: String): Boolean {
        val lowerTitle = title.lowercase()
        return lowerTitle.contains("official trailer") ||
                lowerTitle.contains("theatrical trailer") ||
                lowerTitle.contains("main trailer")
    }
}

// YouTube API data classes and interface
data class YouTubeSearchResponse(
    val items: List<YouTubeVideo>
)

data class YouTubeVideo(
    val id: YouTubeVideoId,
    val snippet: YouTubeSnippet
)

data class YouTubeVideoId(
    val videoId: String
)

data class YouTubeSnippet(
    val title: String,
    val description: String
)

interface YouTubeApi {
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse
}