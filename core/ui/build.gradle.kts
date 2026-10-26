plugins {
    id("filmax.android.compose")
}

android {
    namespace = "com.filmax.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    // Подписи карточек (ratingLabel, typeLabel, titlesLabel…) — в core:presentation: их считают и
    // модели экранов; вёрстка видит их через core:ui транзитивно.
    api(project(":core:presentation"))
    implementation(project(":core:domain"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
