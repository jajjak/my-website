plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val sdkVersion = providers.gradleProperty("androidSdk").orNull?.toIntOrNull() ?: 36

android {
    namespace = "com.linnan.girlvideos"
    compileSdk = sdkVersion

    defaultConfig {
        applicationId = "com.linnan.girlvideos"
        minSdk = 26
        targetSdk = sdkVersion
        versionCode = 3
        versionName = "1.1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.12.0")
}
