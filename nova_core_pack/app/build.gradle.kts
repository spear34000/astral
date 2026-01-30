plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "pics.spear.astral"
    compileSdk = 34

    defaultConfig {
        applicationId = "pics.spear.astral"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            pickFirsts.add("**/libnode.so")
        }
    }

    lint {
        disable.add("InvalidPackage")
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Apache Commons Compress - for extracting tar.xz archives
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("org.tukaani:xz:1.9")

    // J2V8 - V8 JavaScript Engine for Android (deprecated, keeping for fallback)
    // implementation("com.eclipsesource.j2v8:j2v8:6.2.1@aar")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Sora Code Editor
    implementation(platform("io.github.rosemoe:editor-bom:0.23.7"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:language-textmate")
}
