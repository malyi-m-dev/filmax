plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
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

    api(platform(libs.koin.bom))
    api(libs.koin.android)
    implementation(libs.datastore.preferences)

    // Network inspector — реальный Chucker в debug, пустышка в release
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.no.op)
}
