// Связь телефона с ТВ в одной сети: протокол, проверка аккаунта и сокеты — в commonMain (iOS
// подключится к тем же ТВ без переделки протокола), поиск по mDNS и криптография — платформенные.
plugins {
    id("filmax.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmax.data.tvlink"
    // Поиск по mDNS проверяется только на устройстве: NsdManager — системный сервис, на JVM его нет.
    defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
}

kotlin {
    val koinBom = project.dependencies.platform(libs.koin.bom)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.network)

            implementation(koinBom)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        // Профиль аккаунта в тестах — из фейкового data-слоя, как у мок-сборки.
        commonTest.dependencies {
            implementation(project(":data:fake"))
        }
        // `./gradlew :data:tvlink:connectedDebugAndroidTest` на телефоне или эмуляторе с Wi-Fi.
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.junit)
            implementation(libs.kotlin.test.junit)
        }
    }
}
