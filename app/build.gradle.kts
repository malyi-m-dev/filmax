import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("filmax.detekt")
}

android {
    namespace   = "com.filmax.app"
    compileSdk  = 35

    defaultConfig {
        applicationId = "com.filmax.app"
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

    // Features
    implementation(project(":feature:onboarding:mobile"))
    implementation(project(":feature:home:mobile"))
    implementation(project(":feature:search:mobile"))
    implementation(project(":feature:collections:mobile"))
    implementation(project(":feature:library:mobile"))
    implementation(project(":feature:profile:mobile"))
    implementation(project(":feature:details:mobile"))
    implementation(project(":feature:player:mobile"))
    implementation(project(":feature:designsystem"))

    // TV UI (выбирается в MainActivity по FEATURE_LEANBACK)
    implementation(project(":feature:onboarding:tv"))
    implementation(project(":feature:home:tv"))
    implementation(project(":feature:search:tv"))
    implementation(project(":feature:collections:tv"))
    implementation(project(":feature:library:tv"))
    implementation(project(":feature:profile:tv"))
    implementation(project(":feature:details:tv"))
    implementation(project(":feature:player:tv"))

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
