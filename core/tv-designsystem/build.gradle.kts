plugins {
    id("filmax.android.compose")
}

android { namespace = "com.filmax.core.tv.designsystem" }

// tv-material3 1.0.0 — большинство API помечены @ExperimentalTvMaterial3Api;
// opt-in делаем через @file:OptIn в самих файлах (FilmaxTvTheme/TvComponents).

dependencies {
    api(project(":core:designsystem"))
    // Форматтеры подписей (ratingLabel, posterMeta, typeLabel, continueMeta) и PosterImage —
    // общие с телефоном: их место одно на приложение, а не по копии на платформу.
    api(project(":core:ui"))
    api(libs.tv.material)

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
}
