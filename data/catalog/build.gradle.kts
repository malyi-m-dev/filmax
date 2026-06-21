plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmax.data.catalog"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
