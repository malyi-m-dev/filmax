// Логический слой фичи onboarding (без UI): ScreenModel + контракт + DI.
plugins {
    id("filmax.android.library")
}

android { namespace = "com.filmax.feature.onboarding.common" }

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
