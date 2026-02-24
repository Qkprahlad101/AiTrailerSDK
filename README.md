# AiTrailerSDK 🎬

An AI-powered, robust YouTube trailer fetching library for Android applications.

## 🚀 Version 2.0.0 Highlights

- **Gemini 1.5 Flash Integration**: Leverages the latest, high-speed Gemini 1.5 Flash model for near-instant trailer discovery.
- **Provider Pattern Architecture**: Highly testable design allowing developers to easily mock AI responses in their own test suites.
- **Modern Tech Stack**: Built with Kotlin 2.0, Coroutines 1.8.1, and Retrofit 2.11.0 for maximum performance and stability.
- **Improved Fallback Logic**: Resilient multi-source chain that handles network failures and API limits gracefully.

## 📦 Features

- **Triple-Source Detection**: 
    1. **Gemini AI**: Understands natural language and identifies official theatrical trailers.
    2. **YouTube Data API**: High-reliability fallback using direct search queries.
    3. **Pattern Matching**: Offline fallback using a local database of known movie patterns.
- **Intelligent URL Extraction**: Robustly parses YouTube links even from "chatty" AI responses.
- **Comprehensive Error Handling**: Detailed reporting of network, API, and parsing issues.
- **Minimal Footprint**: Lightweight integration with efficient resource usage.

## 🛠️ Installation

### 1. Publish Locally
The SDK is currently configured for local publishing (Maven Local).

```bash
./gradlew publishReleasePublicationToLocalRepoRepository
```

### 2. Configure Your Project

#### settings.gradle.kts:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("/path/to/AiTrailerSDK/app/build/localRepo")
        }
    }
}
```

#### build.gradle.kts:
```kotlin
dependencies {
    implementation("com.example.aitrailersdk:trailerai-core:2.0.0")
}
```

## 🎯 Quick Start

```kotlin
// 1. Initialize the SDK
val config = TrailerAiConfig(
    geminiApiKey = "YOUR_GEMINI_API_KEY",
    youtubeApiKey = "YOUR_YOUTUBE_API_KEY", // Optional Fallback
    enableLogging = true
)
val trailerAi = TrailerAi.initialize(config)

// 2. Fetch a Trailer
lifecycleScope.launch {
    val request = TrailerRequest(movieTitle = "Inception", year = "2010")
    val result = trailerAi.findTrailer(request)
    
    when (result) {
        is TrailerResult.Success -> playVideo(result.url) // https://youtube.com/watch?v=...
        is TrailerResult.NotFound -> showMessage("Trailer not found")
        is TrailerResult.Error -> logError(result.exception)
    }
}
```

## ⚙️ Dependencies Used

| Library | Version | Purpose |
| :--- | :--- | :--- |
| **GenerativeAI** | `0.9.0` | Google Gemini AI integration |
| **Coroutines** | `1.8.1` | Asynchronous operations & Flow |
| **Retrofit** | `2.11.0` | YouTube Data API networking |
| **OkHttp** | `4.12.0` | Network layer & Interceptors |
| **Mockito-Kotlin** | `5.3.1` | Unit testing & Mocking |

## 🧪 Testing

The SDK is designed to be testable. You can verify the parsing logic without making real network calls by using a provider:

```kotlin
@Test
fun testWithFakeAI() = runBlocking {
    val fakeProvider: suspend (String) -> String? = { "https://youtube.com/watch?v=VIDEO_ID" }
    val service = GeminiTrailerService(config, fakeProvider)
    
    val result = service.findTrailer(TrailerRequest("Help!"))
    assert(result is TrailerResult.Success)
}
```

## 📄 License
Apache License 2.0

---
**Last Updated**: 2025-02-22  
**Status**: Stable Release v2.0.0
