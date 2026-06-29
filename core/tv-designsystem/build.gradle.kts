plugins {
    id("filmax.android.compose")
}

android { namespace = "com.filmax.core.tv.designsystem" }

dependencies {
    implementation(project(":core:designsystem"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
}
