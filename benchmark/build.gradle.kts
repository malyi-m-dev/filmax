// Замеры производительности ТВ-интерфейса: Macrobenchmark гоняет сценарии пульта на сборке app
// `benchmark` (release + фейковые данные mock) и снимает время кадров; тот же модуль генерирует
// Baseline Profile. Запуск — на подключённом устройстве или эмуляторе Android TV:
// ./gradlew :benchmark:connectedBenchmarkAndroidTest
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    id("filmax.detekt")
}

android {
    namespace = "com.filmax.benchmark"
    compileSdk = 35

    defaultConfig {
        // Macrobenchmark снимает FrameTimingMetric начиная с Android 10 (API 29).
        minSdk = 29
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Эмулятор — не эталон абсолютных чисел, но сравнивать «до/после» на нём можно: без
        // этого флага библиотека отказывается мерить на эмуляторе вовсе.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    buildTypes {
        // Вариант теста под тип сборки app `benchmark`: AGP сводит их по имени.
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = JvmTarget.JVM_17.target }
}

// Модулю нужен только вариант benchmark — debug-тест к debuggable-сборке мерил бы не то.
androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.enable = variant.buildType == "benchmark"
    }
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.uiautomator)
    implementation(libs.androidx.test.junit)
}
