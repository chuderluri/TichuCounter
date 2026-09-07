plugins {
    alias(libs.plugins.tichu.android.application)
    alias(libs.plugins.tichu.android.compose)
    alias(libs.plugins.tichu.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ch.tichu.counter"

    defaultConfig {
        applicationId = "ch.tichu.counter"
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.ui)
    implementation(projects.feature.groups)
    implementation(projects.feature.players)
    implementation(projects.feature.game)
    implementation(projects.feature.scoring)
    implementation(projects.feature.history)
    implementation(projects.feature.statistics)
    implementation(projects.feature.settings)
    implementation(projects.feature.bugreport)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
