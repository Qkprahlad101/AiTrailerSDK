package com.example.aitrailersdk.core.exception

sealed class TrailerException(message: String, cause: Throwable? = null) : Exception(
    message, cause
) {

    /**
     * Network-related exceptions (timeouts, connection issues)
     */
    class NetworkException(message: String, cause: Throwable? = null): TrailerException(message, cause)

    /**
     * API Key related issues(missing, invalid, expired)
     */
    class APIKeyException(message: String, cause: Throwable? = null) : TrailerException(message, cause)

    /**
     * API quota exceeded
     */
    class QuoteExceededException(message: String, cause: Throwable? = null): TrailerException(message, cause)

    /**
     * Response parsing issues
     */
    class ParseException(message: String, cause: Throwable? = null): TrailerException(message, cause)

    /**
     * Configuration problems
     */
    class ConfigurationException(message: String) : TrailerException(message)

    /**
     * Request Timeout
     */
    class TimeoutException(message: String, cause: Throwable? = null) : TrailerException(message, cause)
}