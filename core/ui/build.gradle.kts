plugins {
    alias(libs.plugins.tichu.android.library)
    alias(libs.plugins.tichu.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ch.tichu.counter.core.ui"
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
