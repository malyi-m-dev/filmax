plugins {
    id("filmax.kmp.library")
}

android {
    namespace = "com.filmax.core.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
