plugins {
    id("filmax.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.filmax.feature.search.tv" }

dependencies {
    api(project(":feature:search:common"))

    implementation(project(":core:ui"))
    implementation(project(":core:tv-designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:presentation"))
    implementation(project(":core:domain"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    // BackHandler: «Назад» закрывает поиск и голосовой оверлей, а не уводит с вкладки.
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)

    // UI-тест фокуса гоняет настоящий SearchScreenModel на фейковом бэкенде — данных mock-сборки.
    testImplementation(project(":data:fake"))
}
