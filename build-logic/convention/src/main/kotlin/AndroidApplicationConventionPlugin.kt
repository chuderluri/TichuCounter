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

            // Two AGP outputs break F-Droid reproducible builds:
//
// baseline.prof/profm embed data that varies with CPU count and toolchain.
// Trade-off: slightly slower cold start without ART profile warmup.
//
// extract<Variant>VersionControlInfo writes META-INF/version-control-info.textproto,
// which pins the APK to the commit it was built from. AGP publishes it as its own
// artifact kind, so packaging.resources.excludes cannot remove it. F-Droid
// compares the entry, so the APK only matches when it is built after tagging.
            tasks.whenTaskAdded {
                if (name.contains("ArtProfile") || name.contains("VersionControlInfo")) {
                    enabled = false
                }
            }
        }
    }
}
