plugins {
    id("filmax.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.home.mobile" }

dependencies {
    // Логика фичи (api — чтобы :app видел HomeScreenModel/HomeModule транзитивно).
    api(project(":feature:home:common"))

    implementation(project(":core:ui"))
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)
}
