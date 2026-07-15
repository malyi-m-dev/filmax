plugins {
    id("filmax.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.library.tv" }

dependencies {
    api(project(":feature:library:common"))

    implementation(project(":core:ui"))
    implementation(project(":core:tv-designsystem"))
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    // BackHandler: «Назад» внутри папки-закладки возвращает к списку папок, а не из раздела.
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)
}
