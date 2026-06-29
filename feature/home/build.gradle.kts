// Логический слой фичи Home (без UI): HomeScreenModel + контракт + DI.
// UI живёт в :feature:home:mobile и :feature:home:tv.
plugins {
    id("filmax.android.library")
}

android {
    namespace = "com.filmax.feature.home"
}

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
