import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("filmax.kmp.library")
    // SKIE — дружелюбный Swift-интероп: Flow → AsyncSequence, sealed → enum, suspend → async.
    // Активен только в macOS-фазе линковки фреймворка; на Android-сборку не влияет.
    alias(libs.plugins.skie)
}

android {
    namespace = "com.filmax.shared"
}

kotlin {
    // XCFramework-бандл: объединяет все iOS-архитектуры (device + simulator) в один .xcframework.
    // Создаёт таски assembleShared{,Debug,Release}XCFramework (реально линкуются только на macOS).
    val xcframework = XCFramework("Shared")

    // iOS-фреймворк (линкуется только на macOS; SKIE подключается в macOS-фазе).
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcframework.add(this)
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

// Собрать release-XCFramework и положить его в iosApp/Frameworks/Shared.xcframework —
// готовый бандл для линковки в Xcode (альтернатива per-build embedAndSignAppleFrameworkForXcode).
// Запуск: ./gradlew :shared:syncSharedXCFramework (только на macOS).
tasks.register<Copy>("syncSharedXCFramework") {
    group = "filmax"
    description = "Собрать release Shared.xcframework и скопировать в iosApp/Frameworks"
    dependsOn("assembleSharedReleaseXCFramework")
    from(layout.buildDirectory.dir("XCFrameworks/release"))
    into(rootProject.layout.projectDirectory.dir("iosApp/Frameworks"))
}
