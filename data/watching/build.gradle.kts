plugins {
    id("filmax.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmax.data.watching"
}

kotlin {
    val koinBom = project.dependencies.platform(libs.koin.bom)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:network"))
            implementation(project(":core:domain"))

            implementation(koinBom)
            implementation(libs.koin.core)
        }
        // «Буду смотреть» в тестах: папки — фейковый сервер из data:fake, флаг kino.pub —
        // подменённый движок Ktor, хранилище — в памяти.
        commonTest.dependencies {
            implementation(project(":data:fake"))
            implementation(libs.ktor.client.mock)
            implementation(libs.multiplatform.settings.test)
        }
    }
}
