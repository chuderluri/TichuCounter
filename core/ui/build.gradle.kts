plugins {
    alias(libs.plugins.tichu.android.library)
    alias(libs.plugins.tichu.android.compose)
    alias(libs.plugins.tichu.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ch.tichu.counter.core.ui"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "GIT_COMMIT_HASH", "\"${gitCommitHash()}\"")
    }
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
}

fun gitCommitHash(): String =
    try {
        providers
            .exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText
            .get()
            .trim()
            .ifBlank { "unknown" }
    } catch (e: Exception) {
        println("git commit hash unavailable: ${e.message}")
        "unknown"
    }
