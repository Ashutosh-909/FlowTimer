plugins {
    alias(libs.plugins.android.application)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

android {
    namespace = "com.ashutosh.flowtimer.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ashutosh.flowtimer"
        minSdk = 30 // Wear OS 3.0+
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    lint {
        // Existing findings (e.g. WearStandaloneAppFlag) are baselined; new ones fail the build.
        baseline = file("lint-baseline.xml")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Wear dependencies will be added in Milestone 5
    implementation(libs.play.services.wearable)
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.wear.tiles.testing)
}
