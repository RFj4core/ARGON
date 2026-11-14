plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.argon.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.argon.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 53
        versionName = "0.0.53b"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
}
