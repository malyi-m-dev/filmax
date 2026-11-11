// Переключатель источника контента: kino.pub или домашний сервер. Экранам отдаёт репозитории
// выбранного источника, сами реализации берёт из Koin по квалификатору — от data:catalog и
// data:jellyfin по коду не зависит.
plugins {
    id("filmax.kmp.library")
}

android {
    namespace = "com.filmax.data.source"
}

kotlin {
    val koinBom = project.dependencies.platform(libs.koin.bom)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.multiplatform.settings)

            implementation(koinBom)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.multiplatform.settings.test)
        }
    }
}
