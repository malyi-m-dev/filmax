// Логический слой фичи library (без UI): ScreenModel + контракт + DI + маршрут папки.
plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.library.common" }

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))
    // Переходы из «Моё» и папки (карточка, плеер, папка, поиск, каталог) решают модели.
    implementation(project(":core:navigation"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    // toRoute<FolderRoute>() + SavedStateHandle
    implementation(libs.navigation.compose)

    // ScreenModel'и тестируются на фейковом data-слое — том же, что у мок-сборки.
    testImplementation(project(":data:fake"))
    // Маршрут папки из SavedStateHandle (toRoute) читается через Bundle — нужен Android-рантайм.
    testImplementation(libs.robolectric)
}
