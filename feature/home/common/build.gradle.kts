// Логический слой фичи Home (без UI): HomeScreenModel + контракт + DI.
// UI живёт в :feature:home:mobile и :feature:home:tv.
plugins {
    id("filmax.android.library")
}

android {
    namespace = "com.filmax.feature.home.common"
}

dependencies {
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))
    // «Продолжить» без известной серии ведёт в плеер с Destination.Player.UNKNOWN.
    implementation(project(":core:navigation"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    // ScreenModel тестируется на фейковом data-слое — тех же фейках, что у mock-сборки.
    testImplementation(project(":data:fake"))
}
