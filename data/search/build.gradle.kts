plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.filmax.data.search"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:domain"))
    implementation(project(":data:catalog"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
