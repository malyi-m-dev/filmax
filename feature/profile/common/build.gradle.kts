// Логический слой фичи profile (без UI): ScreenModel + контракт + DI.
plugins {
    id("filmax.android.library")
}

android { namespace = "com.filmax.feature.profile.common" }

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
