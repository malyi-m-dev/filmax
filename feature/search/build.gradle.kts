plugins {
    id("filmax.android.feature")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.filmax.feature.search" }
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":data:search"))
    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
