// Логический слой Player (без UI): PlayerScreenModel (ExoPlayer) + контракт + DI + маршрут.
plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.player" }

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.navigation.compose)

    // ExoPlayer живёт в ScreenModel
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
}
