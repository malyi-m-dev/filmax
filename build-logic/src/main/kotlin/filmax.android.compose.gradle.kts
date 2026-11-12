import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("filmax.detekt")
    id("filmax.android.unit-test")
    id("filmax.android.ui-test")
}

android {
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = JvmTarget.JVM_17.target }
    buildFeatures { compose = true }
}

// Превью рисует Android Studio по ui-tooling. Он нужен только отладочной сборке: аннотации
// @Preview (ui-tooling-preview) уже в бандле compose, а релизу инструменты студии ни к чему.
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    "debugImplementation"(platform(catalog.findLibrary("compose-bom").get()))
    "debugImplementation"(catalog.findLibrary("compose-ui-tooling").get())
}
