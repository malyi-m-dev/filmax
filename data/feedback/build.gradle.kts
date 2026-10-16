plugins {
    id("filmax.kmp.library")
}

android {
    namespace = "com.filmax.data.feedback"
}

kotlin {
    val koinBom = project.dependencies.platform(libs.koin.bom)
    sourceSets {
        commonMain.dependencies {
            // Движок HTTP — из core:network, клиент свой: Firestore — другой хост и без Bearer kino.pub.
            implementation(project(":core:network"))
            implementation(project(":core:domain"))
            implementation(libs.bundles.ktor.common)

            implementation(koinBom)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
    }
}
