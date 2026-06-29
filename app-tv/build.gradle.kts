import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace   = "com.filmax.tv"
    compileSdk  = 35

    defaultConfig {
        applicationId = "com.filmax.tv"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = JvmTarget.JVM_17.target }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(project(":core:network"))
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:tv-designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:presentation"))

    // Data
    implementation(project(":data:auth"))
    implementation(project(":data:catalog"))
    implementation(project(":data:search"))
    implementation(project(":data:user"))
    implementation(project(":data:watching"))

    // Shared MVI logic (ScreenModels + DI modules reused from mobile features)
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:search"))
    implementation(project(":feature:library"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:details"))
    implementation(project(":feature:player"))

    // TV UI features
    implementation(project(":feature-tv:onboarding"))
    implementation(project(":feature:home:tv"))
    implementation(project(":feature-tv:search"))
    implementation(project(":feature-tv:categories"))
    implementation(project(":feature-tv:library"))
    implementation(project(":feature-tv:profile"))
    implementation(project(":feature-tv:details"))
    implementation(project(":feature-tv:player"))

    // Compose
    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.activity.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}
