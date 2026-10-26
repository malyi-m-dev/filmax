// Логический слой связи телефона с ТВ (без UI): выбор ТВ и пульт на телефоне, приёмник на ТВ.
plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.tvlink.common" }

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:navigation"))
    implementation(project(":core:domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    // toRoute<TvPickerRoute>() + SavedStateHandle
    implementation(libs.navigation.compose)

    // ScreenModel тестируется на фейковом data-слое — тот же, что у мок-сборки.
    testImplementation(project(":data:fake"))
    // Маршрут из SavedStateHandle (toRoute) читается через Bundle — нужен Android-рантайм.
    testImplementation(libs.robolectric)
}
