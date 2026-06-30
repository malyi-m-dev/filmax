plugins {
    id("filmax.android.compose")
}

android {
    namespace = "com.filmax.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:domain"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
