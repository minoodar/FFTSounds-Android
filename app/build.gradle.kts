plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Add the maven-publish plugin to enable publishing.
    id("maven-publish")
}

android {
    namespace = "ir.zarbang.FFTsounds"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Core dependencies
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")

    // Media3 libraries
    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")

    // Coroutines and FFT library
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("com.github.wendykierp:JTransforms:3.1")
}

// Publishing configuration to publish the AAR to a Maven repository.
afterEvaluate {
    publishing {
        publications {
            // CORRECTED: Use create<MavenPublication>("name") for Kotlin DSL syntax.
            create<MavenPublication>("release") {
                // This line specifies that the AAR from the "release" build variant should be used.
                from(components["release"])

                // The coordinates that the Unity team will use to find the library.
                groupId = "ir.zarbang.FFTsounds"
                artifactId = "audio-engine"
                version = "1.0.2-SNAPSHOT"
            }
        }
    }
}
