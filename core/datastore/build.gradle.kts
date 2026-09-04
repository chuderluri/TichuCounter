plugins {
    alias(libs.plugins.tichu.android.library)
    alias(libs.plugins.tichu.android.hilt)
}

android {
    namespace = "ch.tichu.counter.core.datastore"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
