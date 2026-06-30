// Логический слой Collections (без UI): ScreenModel-и + контракты + DI + маршруты.
plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.collections.common" }

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    // toRoute<CollectionDetailRoute>() + SavedStateHandle
    implementation(libs.navigation.compose)
}
