// Домашний медиасервер Jellyfin: те же контракты core:domain, что у kino.pub, поверх REST API
// сервера в локальной сети. KMP, как и остальные data-модули: протокол и DTO не привязаны к Android.
plugins {
    id("filmax.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmax.data.jellyfin"
}

kotlin {
    val koinBom = project.dependencies.platform(libs.koin.bom)
    sourceSets {
        commonMain.dependencies {
            // Движок HTTP, настройки устройства и Ktor — те же, что у kino.pub.
            implementation(project(":core:network"))
            implementation(project(":core:domain"))
            // Поиск сервера в сети — широковещательный UDP, как у официальных клиентов.
            implementation(libs.ktor.network)

            implementation(koinBom)
            implementation(libs.koin.core)
        }
        // Запросы к серверу в тестах — подменённым движком Ktor, без сети.
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.multiplatform.settings.test)
        }
    }
}
