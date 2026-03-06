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
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Wearable DataLayer (phone ↔ watch communication)
    implementation(libs.play.services.wearable)

    // Wear Tiles
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)

    // Protolayout (layout building for Tiles in tiles 1.3+)
    implementation(libs.wear.protolayout)

    // Watchface Complications data source
    implementation(libs.watchface.complications.datasource)

    // DataStore (local state on the watch)
    implementation(libs.androidx.datastore.preferences)

    // Coroutines (android + guava bridge for ListenableFuture from coroutines)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.wear.tiles.testing)
}
