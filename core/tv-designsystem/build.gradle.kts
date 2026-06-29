plugins {
    id("filmax.android.compose")
}

android { namespace = "com.filmax.core.tv.designsystem" }

// tv-material3 1.0.0 — большинство API помечены @ExperimentalTvMaterial3Api;
// opt-in делаем через @file:OptIn в самих файлах (FilmaxTvTheme/TvComponents).

dependencies {
    api(project(":core:designsystem"))
    api(libs.tv.material)

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
}
