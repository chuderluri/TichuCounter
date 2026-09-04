plugins {
    alias(libs.plugins.tichu.android.library)
    alias(libs.plugins.tichu.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ch.tichu.counter.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    testImplementation(projects.core.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
}
