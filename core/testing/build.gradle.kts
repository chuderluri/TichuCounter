plugins {
    alias(libs.plugins.tichu.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.domain)
    api(projects.core.common)
    api(libs.kotlinx.coroutines.test)
    api(platform(libs.junit5.bom))
    api(libs.junit5.jupiter)
    api(libs.turbine)
}
