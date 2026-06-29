plugins {
    id("filmax.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmax.core.network"
}

kotlin {
    val koinBom = project.dependencies.platform(libs.koin.bom)
    sourceSets {
        commonMain.dependencies {
            api(libs.bundles.ktor.common)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
            api(libs.multiplatform.settings)
        }
        androidMain.dependencies {
            api(libs.ktor.client.okhttp)
            // Временно: Retrofit/OkHttp нужны data-модулям до миграции на Ktor (Фаза 3).
            api(libs.okhttp.core)
            api(libs.okhttp.logging)
            api(libs.retrofit.core)
            api(libs.retrofit.serialization)
            api(libs.kotlinx.coroutines.android)

            api(koinBom)
            api(libs.koin.android)
            implementation(libs.datastore.preferences)
        }
        iosMain.dependencies {
            api(libs.ktor.client.darwin)
        }
    }
}

// Network inspector — реальный Chucker в debug, пустышка в release (только Android).
dependencies {
    "debugImplementation"(libs.chucker)
    "releaseImplementation"(libs.chucker.no.op)
}
