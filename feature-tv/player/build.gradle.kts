plugins {
    id("filmax.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.tv.player" }

dependencies {
    implementation(project(":core:tv-designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))
    // Переиспользуем PlayerScreenModel (ExoPlayer) + PlayerRoute мобильной фичи.
    implementation(project(":feature:player"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)

    // ExoPlayer + PlayerView для отрисовки видео
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)
}
