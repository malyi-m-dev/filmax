// Логический слой фичи search (без UI): ScreenModel + контракт + DI + маршрут «Фильмографии».
plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.search.common" }

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    // toRoute<FilmographyRoute>() + @Serializable-маршрут «Фильмографии»
    implementation(libs.navigation.compose)
}
