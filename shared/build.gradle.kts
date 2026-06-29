import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("filmax.kmp.library")
}

android {
    namespace = "com.filmax.shared"
}

kotlin {
    // iOS-фреймворк (линкуется только на macOS; SKIE/CocoaPods подключаются в macOS-фазе).
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
            if (HostManager.hostIsMac) {
                export(project(":core:domain"))
                export(project(":core:network"))
                export(project(":data:auth"))
                export(project(":data:catalog"))
                export(project(":data:search"))
                export(project(":data:user"))
                export(project(":data:watching"))
            }
        }
    }

    val koinBom = project.dependencies.platform(libs.koin.bom)
    sourceSets {
        commonMain.dependencies {
            // api-реэкспорт общего кода — виден из Swift через framework.
            api(project(":core:domain"))
            api(project(":core:network"))
            api(project(":data:auth"))
            api(project(":data:catalog"))
            api(project(":data:search"))
            api(project(":data:user"))
            api(project(":data:watching"))

            api(koinBom)
            api(libs.koin.core)
        }
    }
}
