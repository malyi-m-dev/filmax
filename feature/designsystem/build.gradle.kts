plugins {
    id("filmax.android.feature")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.filmax.feature.designsystem" }
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)
}
