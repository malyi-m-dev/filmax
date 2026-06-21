plugins {
    id("filmax.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmax.data.search"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:domain"))
    implementation(project(":data:catalog"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
