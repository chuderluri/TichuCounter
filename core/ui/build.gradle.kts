plugins {
    alias(libs.plugins.tichu.android.library)
    alias(libs.plugins.tichu.android.compose)
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
}
