plugins {
    id("filmax.kmp.library")
}

android {
    namespace = "com.filmax.core.domain"
}

kotlin {
    // JVM — для core:navigation: адрес плеера (VideoRef) — доменный тип, а навигация — чистый
    // JVM-модуль без Android.
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
