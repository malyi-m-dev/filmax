plugins {
    id("filmax.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.filmax.data.auth"
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
    }
}
