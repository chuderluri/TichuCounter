package ch.tichu.counter.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project

private const val SIGNING_CONFIG_NAME = "tichu"
private const val STORE_FILE = "tichu.storeFile"
private const val STORE_PASSWORD = "tichu.storePassword"
private const val KEY_ALIAS = "tichu.keyAlias"
private const val KEY_PASSWORD = "tichu.keyPassword"

/**
 * Registers a signing config named [SIGNING_CONFIG_NAME] when the release key is
 * configured through Gradle properties. Without those properties no signing
 * config is created and `assembleRelease` stays unsigned, which is what the
 * F-Droid build needs.
 */
internal fun Project.configureReleaseSigning(commonExtension: ApplicationExtension) {
    val storeFile = findProperty(STORE_FILE) as String?
    val storePassword = findProperty(STORE_PASSWORD) as String?
    val keyAlias = findProperty(KEY_ALIAS) as String?
    val keyPassword = findProperty(KEY_PASSWORD) as String?
    if (storeFile.isNullOrBlank() ||
        storePassword.isNullOrBlank() ||
        keyAlias.isNullOrBlank() ||
        keyPassword.isNullOrBlank()
    ) {
        return
    }

    commonExtension.signingConfigs.create(SIGNING_CONFIG_NAME) {
        this.storeFile = file(storeFile)
        this.storePassword = storePassword
        this.keyAlias = keyAlias
        this.keyPassword = keyPassword
        enableV1Signing = true
        enableV2Signing = true
        enableV3Signing = true
    }
}
