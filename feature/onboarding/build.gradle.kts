plugins {
    id("filmax.android.feature")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.filmax.feature.onboarding" }
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))
    implementation(project(":data:auth"))
    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)
}
