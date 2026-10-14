plugins {
    id("filmax.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmax.data.user"
}

kotlin {
    val koinBom = project.dependencies.platform(libs.koin.bom)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:network"))
            implementation(project(":core:domain"))
            implementation(project(":data:catalog"))

            implementation(koinBom)
            implementation(libs.koin.core)
        }
        // Ответы kino.pub в тестах — подменённым движком Ktor, без сети.
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
    }
}
