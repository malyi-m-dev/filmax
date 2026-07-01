// Логический слой Details (без UI): ScreenModel + контракт + DI + маршрут.
plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.details.common" }

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    // toRoute<DetailsRoute>() + SavedStateHandle
    implementation(libs.navigation.compose)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
