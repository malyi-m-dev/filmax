// Логический слой Details (без UI): ScreenModel + контракт + DI + маршрут.
plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.details.common" }

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))
    // Куда ведут кнопки и карточки деталей, решает модель — экраны только шлют события.
    implementation(project(":core:navigation"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    // toRoute<DetailsRoute>() + SavedStateHandle
    implementation(libs.navigation.compose)

    // ScreenModel тестируется на фейковом data-слое — тот же, что у мок-сборки.
    testImplementation(project(":data:fake"))
    // Маршрут из SavedStateHandle (toRoute) читается через Bundle — нужен Android-рантайм.
    testImplementation(libs.robolectric)
}
