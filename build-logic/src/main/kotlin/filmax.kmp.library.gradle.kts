import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("filmax.detekt")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    // iOS (iPhone/iPad) + tvOS (Apple TV). Общий Apple-код живёт в appleMain
    // (создаётся default-иерархией KMP и покрывает и ios, и tvos).
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()
}

// Android-конфигурация общая для всех KMP-модулей; namespace задаётся в каждом модуле.
extensions.configure<LibraryExtension> {
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
