package com.example.aitrailersdk.core.impl

import com.example.aitrailersdk.core.config.TrailerAiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiServiceTest {

    private val service = GeminiTrailerService(TrailerAiConfig())

    @Test
    fun `test exact URL extraction`() {
        val text = "https://www.youtube.com/watch?v=yR7A-Y_mYCc"
        assertEquals("https://www.youtube.com/watch?v=yR7A-Y_mYCc", service.extractYouTubeUrl(text))
    }

    @Test
    fun `test URL extraction with extra text`() {
        val text = "Sure! Here is the trailer: https://www.youtube.com/watch?v=yR7A-Y_mYCc hope you like it."
        assertEquals("https://www.youtube.com/watch?v=yR7A-Y_mYCc", service.extractYouTubeUrl(text))
    }

    @Test
    fun `test shortened URL extraction`() {
        val text = "Check this out: https://youtu.be/yR7A-Y_mYCc"
        assertEquals("https://www.youtube.com/watch?v=yR7A-Y_mYCc", service.extractYouTubeUrl(text))
    }

    @Test
    fun `test NO_TRAILER_FOUND handling`() {
        val text = "NO_TRAILER_FOUND"
        assertNull(service.extractYouTubeUrl(text))
    }

    @Test
    fun `test NO_TRAILER_FOUND with extra text`() {
        val text = "I searched everywhere but NO_TRAILER_FOUND for this movie."
        assertNull(service.extractYouTubeUrl(text))
    }

    @Test
    fun `test URL with query parameters`() {
        val text = "https://www.youtube.com/watch?v=yR7A-Y_mYCc&t=10s"
        assertEquals("https://www.youtube.com/watch?v=yR7A-Y_mYCc", service.extractYouTubeUrl(text))
    }

    @Test
    fun `test Help! movie case with chatty response`() {
        val text = "The trailer for Help! (1965) is https://www.youtube.com/watch?v=yR7A-Y_mYCc"
        assertEquals("https://www.youtube.com/watch?v=yR7A-Y_mYCc", service.extractYouTubeUrl(text))
    }
}
