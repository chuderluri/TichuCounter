import ch.tichu.counter.buildlogic.TichuSdk
import ch.tichu.counter.buildlogic.configureKotlinAndroid
import ch.tichu.counter.buildlogic.configureReleaseSigning
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                configureReleaseSigning(this)
                defaultConfig.targetSdk = TichuSdk.TARGET
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                buildTypes {
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        signingConfig = signingConfigs.findByName("tichu")
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                    }
                }
                packaging {
                    resources.excludes += setOf(
                        "META-INF/LICENSE*",
                        "META-INF/AL2.0",
                        "META-INF/LGPL2.1",
                        "DebugProbesKt.bin",
                        "kotlin-tooling-metadata.json",
                        "kotlin/**",
                    )
                }
            }

            // baseline.prof/profm embed data that varies with CPU count and toolchain,
            // which breaks F-Droid reproducible builds. Trade-off: slightly slower
            // cold start without ART profile warmup.
            tasks.whenTaskAdded {
                if (name.contains("ArtProfile")) {
                    enabled = false
                }
            }
        }
    }
}
