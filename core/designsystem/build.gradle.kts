plugins {
    id("filmax.android.compose")
}

android {
    namespace = "com.filmax.core.designsystem"
}

dependencies {
    val bom = platform(libs.compose.bom)
    api(bom)
    api(libs.bundles.compose)
}
