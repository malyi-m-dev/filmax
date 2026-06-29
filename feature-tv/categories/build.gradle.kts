plugins {
    id("filmax.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.tv.categories" }

dependencies {
    implementation(project(":core:tv-designsystem"))
    implementation(project(":core:designsystem"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)
}
