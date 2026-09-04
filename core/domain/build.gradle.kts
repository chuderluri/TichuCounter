plugins {
    alias(libs.plugins.tichu.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.common)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)

    testImplementation(projects.core.testing)
    testImplementation(libs.turbine)
}
