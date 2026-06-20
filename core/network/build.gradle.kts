plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.filmax.core.network"
}

dependencies {
    api(libs.okhttp.core)
    api(libs.okhttp.logging)
    api(libs.retrofit.core)
    api(libs.retrofit.serialization)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    implementation(libs.datastore.preferences)
    ksp(libs.hilt.compiler)
}
