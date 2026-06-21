plugins {
    id("filmax.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmax.feature.home"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))
    implementation(project(":data:catalog"))
    implementation(project(":data:watching"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)
}
