plugins {
    id("filmax.android.feature")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.filmax.feature.profile" }
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":data:user"))
    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)
}
