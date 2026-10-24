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
    // Стекло (Glass.kt): размытие того, что под шапкой и навигацией. Наружу не торчит — экраны
    // берут GlassState и модификаторы дизайн-системы, а не API библиотеки.
    implementation(libs.haze)
}
