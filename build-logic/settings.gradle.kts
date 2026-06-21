pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.library")                       version "8.7.3"
        id("com.android.application")                   version "8.7.3"
        id("org.jetbrains.kotlin.android")              version "2.0.21"
        id("org.jetbrains.kotlin.jvm")                  version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose")       version "2.0.21"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
