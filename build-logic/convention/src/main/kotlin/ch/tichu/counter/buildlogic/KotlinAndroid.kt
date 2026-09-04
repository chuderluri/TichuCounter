package ch.tichu.counter.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        compileSdk = TichuSdk.COMPILE

        defaultConfig {
            minSdk = TichuSdk.MIN
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        testOptions {
            unitTests {
                isIncludeAndroidResources = true
                isReturnDefaultValues = true
            }
        }
    }

    configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(commonCompilerArgs)
        }
    }

    configureJUnit5()
}

internal fun Project.configureKotlinJvm() {
    configure<KotlinJvmProjectExtension> {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(commonCompilerArgs)
        }
    }
    configureJUnit5()
}

internal fun Project.configureJUnit5() {
    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
    dependencies {
        add("testImplementation", platform(libs.findLibrary("junit5-bom").get()))
        add("testImplementation", libs.findLibrary("junit5-jupiter").get())
        add("testImplementation", libs.findLibrary("junit5-jupiter-params").get())
        add("testImplementation", libs.findLibrary("kotlin-test").get())
        add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        add("testRuntimeOnly", libs.findLibrary("junit5-platform-launcher").get())
    }
}

private val commonCompilerArgs = listOf(
    "-opt-in=kotlin.RequiresOptIn",
    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    "-Xcontext-receivers",
)
