plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.sahed.clipsave"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sahed.clipsave"
        // 29+ => scoped storage only (MediaStore), no legacy storage permissions.
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            // yt-dlp's embedded Python + ffmpeg are big; ship the ABIs real phones/emulators use.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Local installs only; swap for a real keystore before publishing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            // youtubedl-android unpacks python/ffmpeg from native-lib dir at runtime.
            useLegacyPackaging = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    // Backdrop blur for the floating glass dock (RenderEffect on 31+, tinted scrim below).
    implementation(libs.haze)
    implementation(libs.coil.network.okhttp)

    // yt-dlp for Android: extractors for YouTube, Instagram, Facebook, TikTok, X, Vimeo, ...
    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)
}
