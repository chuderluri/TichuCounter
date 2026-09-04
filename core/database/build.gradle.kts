plugins {
    alias(libs.plugins.tichu.android.library)
    alias(libs.plugins.tichu.android.hilt)
    alias(libs.plugins.tichu.android.room)
}

android {
    namespace = "ch.tichu.counter.core.database"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
