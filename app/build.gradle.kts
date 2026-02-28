plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

android {
    namespace = "com.example.aitrailersdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("gemini.api.key") ?: ""}\"")
        buildConfigField("String", "YOUTUBE_API_KEY", "\"${project.findProperty("youtube.api.key") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.Qkprahlad101"
                artifactId = "aitrailer-sdk"
                version = "3.9.0"

                from(components["release"])

                pom {
                    name.set("AiTrailer SDK")
                    description.set("Android SDK for finding movie trailers using AI (Gemini) and YouTube APIs")
                    url.set("https://github.com/Qkprahlad101/AiTrailerSDK")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("Qkprahlad101")
                            name.set("Prahlad Kumar")
                            email.set("k.prahlad101@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/Qkprahlad101/AiTrailerSDK.git")
                        developerConnection.set("scm:git:ssh://github.com/Qkprahlad101/AiTrailerSDK.git")
                        url.set("https://github.com/Qkprahlad101/AiTrailerSDK")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "localRepo"
                url = uri("${project.layout.buildDirectory.get().asFile}/localRepo")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Core SDK Dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Gemini AI (Latest stable version)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Networking (Updated for stability)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
