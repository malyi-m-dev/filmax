plugins {
    id("filmax.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmax.data.tmdb"
}

kotlin {
    val koinBom = project.dependencies.platform(libs.koin.bom)
    sourceSets {
        commonMain.dependencies {
            // Движок HTTP берём из core:network, но клиент TMDB строим свой: другой хост и
            // свой api_key, БЕЗ Bearer-авторизации kino.pub. Ktor-плагины подключаем явно —
            // тем же бандлом, что и core:network (ContentNegotiation, json и т.д.).
            implementation(project(":core:network"))
            implementation(project(":core:domain"))
            implementation(libs.bundles.ktor.common)

            implementation(koinBom)
            implementation(libs.koin.core)
        }
    }
}
