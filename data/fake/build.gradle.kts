// Фейковый data-слой: те же контракты core:domain, что у data:auth/catalog/…, но без сети kino.pub.
// Нужен сборке `mock` (UI на эмуляторе без входа и живого бэкенда) и юнит-тестам ScreenModel'ей,
// поэтому KMP, как и остальные data-модули, и без зависимостей от core:network.
plugins {
    id("filmax.kmp.library")
}

android {
    namespace = "com.filmax.data.fake"
}

kotlin {
    val koinBom = project.dependencies.platform(libs.koin.bom)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(libs.kotlinx.coroutines.core)

            implementation(koinBom)
            implementation(libs.koin.core)
        }
        // FakeNavigator для тестов ScreenModel: контракт навигации — JVM-модуль, Apple-таргетам
        // он не нужен (iOS-презентейшен на SwiftUI со своей навигацией).
        androidMain.dependencies {
            implementation(project(":core:navigation"))
        }
    }
}
