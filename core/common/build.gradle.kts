plugins {
    alias(libs.plugins.tichu.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
}
