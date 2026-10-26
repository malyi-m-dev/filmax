// Логический слой Feedback (без UI): ScreenModel-и + контракты + DI + маршруты.
plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.feedback.common" }

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:navigation"))
    implementation(project(":core:domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    // toRoute<FeedbackRoute>() + SavedStateHandle
    implementation(libs.navigation.compose)

    // ScreenModel тестируется на фейковом data-слое — тот же, что у мок-сборки.
    testImplementation(project(":data:fake"))
    // Маршрут из SavedStateHandle (toRoute) читается через Bundle — нужен Android-рантайм.
    testImplementation(libs.robolectric)
}
