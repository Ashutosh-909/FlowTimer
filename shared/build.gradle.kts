plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    // JVM target — consumed by the :app Android module as a regular jar
    jvm()

    // iOS targets — produce the shared.framework for iosApp
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        iosMain.dependencies {
            implementation(libs.multiplatform.settings)
        }
    }
}
