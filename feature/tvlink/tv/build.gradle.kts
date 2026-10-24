plugins {
    id("filmax.android.feature")
}

android { namespace = "com.filmax.feature.tvlink.tv" }

dependencies {
    api(project(":feature:tvlink:common"))

    implementation(project(":core:tv-designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)
}
