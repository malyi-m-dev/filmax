import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("filmax.detekt")
}

// Firebase (Crashlytics) включается только при наличии конфига: локальная сборка и PR-CI без
// секретов не должны падать. Файл содержит ключи проекта Firebase — в публичном репо его не
// держим (.gitignore); локально кладётся в app/, в CI декодируется из секрета
// GOOGLE_SERVICES_JSON_BASE64. Без файла сборка проходит, репортинг остаётся no-op.
val googleServicesConfig = file("google-services.json")
if (googleServicesConfig.exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)

    // google-services строго сверяет applicationId с client-списком json и роняет сборку
    // вариантов, чьих пакетов там нет («No matching client found»). Пока в Firebase
    // зарегистрирован только боевой com.filmax.app — для остальных вариантов выключаем
    // задачи Firebase; их рантайм и так no-op (FirebaseApp.initializeApp вернёт null).
    // Зарегистрируешь .debug/.demo и обновишь json — фильтр сам перестанет что-либо выключать.
    val registeredPackages = Regex("\"package_name\"\\s*:\\s*\"([^\"]+)\"")
        .findAll(googleServicesConfig.readText())
        .map { it.groupValues[1] }
        .toSet()
    val variantPackages = mapOf(
        "Debug" to "com.filmax.app.debug",
        "Demo" to "com.filmax.app.demo",
        "Mock" to "com.filmax.app.mock",
        "Benchmark" to "com.filmax.app.bench",
        "Release" to "com.filmax.app",
    )
    val unregisteredVariants = variantPackages.filterValues { it !in registeredPackages }.keys
    tasks.matching { task ->
        val firebaseTask = task.name.contains("GoogleServices") || task.name.contains("Crashlytics")
        firebaseTask && unregisteredVariants.any { variantName -> task.name.contains(variantName) }
    }.configureEach { enabled = false }
}

// Секреты подписи release: локально из keystore.properties (в .gitignore),
// в CI — из env-переменных (GitHub Secrets). env имеет приоритет над файлом.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use(::load)
}
fun signingSecret(envName: String, propName: String): String? =
    System.getenv(envName) ?: keystoreProps.getProperty(propName)

// Ключ TMDB (фото актёров): из local.properties (в .gitignore) либо env в CI. Пусто — фото
// просто не загрузятся, приложение работает как обычно.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val tmdbApiKey: String = (System.getenv("TMDB_API_KEY") ?: localProps.getProperty("tmdb.apiKey") ?: "").trim()

// Токены для demo-сборки (ТОЛЬКО build type `demo`): из local.properties (в .gitignore). Зашиваются
// в APK, чтобы demo-билд открывался авторизованным на любом устройстве без входа. В release/debug —
// пусто, поэтому обычные сборки токен не несут.
val demoAccessToken: String = (localProps.getProperty("demo.accessToken") ?: "").trim()
val demoRefreshToken: String = (localProps.getProperty("demo.refreshToken") ?: "").trim()

// In-app update читает GitHub Releases ПРИВАТНОГО репозитория, поэтому в сборку зашивается
// fine-grained токен ТОЛЬКО на чтение contents этого репо (local.properties → github.updateToken,
// в CI — env UPDATE_GITHUB_TOKEN). Пусто — проверка обновлений молча не находит релизов.
val updateGithubToken: String =
    (System.getenv("UPDATE_GITHUB_TOKEN") ?: localProps.getProperty("github.updateToken") ?: "").trim()

// versionName ← последний git-тег vX.Y.Z (без «v»); нет тегов → 1.0.0.
fun gitVersionName(): String =
    providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().removePrefix("v").ifEmpty { "1.0.0" }

// versionCode ← число коммитов в HEAD: монотонно растёт от релиза к релизу.
// В CI требуется полная история (checkout fetch-depth: 0), иначе вернёт 1.
fun gitCommitCount(): Int =
    providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().toIntOrNull() ?: 1

android {
    namespace   = "com.filmax.app"
    compileSdk  = 35

    defaultConfig {
        applicationId = "com.filmax.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = gitCommitCount()
        versionName   = gitVersionName()
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        // По умолчанию токена нет — его несёт только build type `demo`.
        buildConfigField("String", "DEMO_ACCESS_TOKEN", "\"\"")
        buildConfigField("String", "DEMO_REFRESH_TOKEN", "\"\"")
        // In-app update: откуда читать релизы и чем авторизоваться (см. GitHubUpdateRepository).
        buildConfigField("String", "UPDATE_GITHUB_REPO", "\"malyi-m-dev/filmax\"")
        buildConfigField("String", "UPDATE_GITHUB_TOKEN", "\"$updateGithubToken\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingSecret("KEYSTORE_FILE", "storeFile")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = signingSecret("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingSecret("KEY_ALIAS", "keyAlias")
                keyPassword = signingSecret("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Debug ставится рядом с release, а не поверх: у них разные подписи, и установка
            // «поверх» требовала бы удалить release вместе с авторизацией. Суффикс даёт
            // отдельный пакет — обе сборки живут на устройстве одновременно.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Подписываем release только когда ключ реально доступен (keystore.properties
            // локально или env в CI). Без ключа оставляем неподписанным, чтобы сборка
            // без секретов (например, PR-проверки) не падала.
            signingConfigs.getByName("release").takeIf { it.storeFile?.exists() == true }
                ?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // Demo-сборка: как release (R8 + подпись), но с зашитым токеном (стартует авторизованной
        // на любом устройстве) и отдельным пакетом/меткой «Filmax Demo» — не путать с боевой.
        create("demo") {
            initWith(getByName("release"))
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            // Библиотечные модули не знают build type `demo` — берём их release-вариант.
            matchingFallbacks += "release"
            signingConfigs.getByName("release").takeIf { it.storeFile?.exists() == true }
                ?.let { signingConfig = it }
            buildConfigField("String", "DEMO_ACCESS_TOKEN", "\"$demoAccessToken\"")
            buildConfigField("String", "DEMO_REFRESH_TOKEN", "\"$demoRefreshToken\"")
        }
        // Mock-сборка: debug без бэкенда — data-слой подменён фейками (:data:fake, см.
        // src/mock), вход не нужен, данные выдуманные. Для проверки UI на эмуляторах, куда живой
        // kino.pub не достаёт или где протухли токены. Свой пакет и метка «Filmax Mock» — ставится
        // рядом с debug и release, не путаясь с ними.
        create("mock") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".mock"
            versionNameSuffix = "-mock"
            // Библиотечные модули не знают build type `mock` — берём их debug-вариант.
            matchingFallbacks += "debug"
        }
        // Benchmark-сборка: как release (R8, не debuggable — замеры кадров на debug врут в разы),
        // но на фейковых данных mock: Macrobenchmark гоняет сценарии пульта без входа и сети, и
        // каждый прогон видит одну и ту же ленту. Подпись debug — ставится на любой эмулятор.
        create("benchmark") {
            initWith(getByName("release"))
            applicationIdSuffix = ".bench"
            versionNameSuffix = "-bench"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            proguardFiles("benchmark-rules.pro")
        }
    }

    // Benchmark берёт подмену Application и строки из mock — фейковый data-слой у них общий.
    sourceSets.getByName("benchmark") {
        java.srcDir("src/mock/kotlin")
        res.srcDir("src/mock/res")
        manifest.srcFile("src/benchmark/AndroidManifest.xml")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = JvmTarget.JVM_17.target }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // UI-тесты ТВ-оболочки (меню, переходы) — на Robolectric: без ресурсов тема и шрифты пустые.
    testOptions { unitTests.isIncludeAndroidResources = true }

    lint {
        // AGP 8.7.3 запускает lint-vital при release-сборке, но его детекторы падают
        // на несовместимости Kotlin-анализатора (KaCallableMemberCall — известный баг
        // lint) → assembleRelease рушится в тулинге, а не на коде. Гейт статического
        // анализа в проекте — detekt (в CI), поэтому vital-lint на release отключаем.
        checkReleaseBuilds = false
    }
}

dependencies {
    // Core
    implementation(project(":core:network"))
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:tv-designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:presentation"))
    implementation(project(":core:navigation"))

    // Data
    implementation(project(":data:auth"))
    implementation(project(":data:catalog"))
    implementation(project(":data:search"))
    implementation(project(":data:user"))
    implementation(project(":data:watching"))
    implementation(project(":data:tmdb"))
    // Фейки только в сборке mock: main-код о них не знает, подключает MockFilmaxApplication.
    "mockImplementation"(project(":data:fake"))
    "benchmarkImplementation"(project(":data:fake"))

    // Baseline Profile ставится и без Google Play: APK раздаётся с GitHub, и без installer'а
    // профиль из APK так бы и не скомпилировался — первые прокрутки шли бы через JIT с рывками.
    implementation(libs.profileinstaller)
    // Рывки кадров по экранам — сводка в logcat (JankMonitor).
    implementation(libs.metrics.performance)

    // Features
    implementation(project(":feature:onboarding:mobile"))
    implementation(project(":feature:home:mobile"))
    implementation(project(":feature:search:mobile"))
    implementation(project(":feature:collections:mobile"))
    implementation(project(":feature:library:mobile"))
    implementation(project(":feature:profile:mobile"))
    implementation(project(":feature:details:mobile"))
    implementation(project(":feature:player:mobile"))
    implementation(project(":feature:designsystem"))

    // TV UI (выбирается в MainActivity по FEATURE_LEANBACK)
    implementation(project(":feature:onboarding:tv"))
    implementation(project(":feature:home:tv"))
    implementation(project(":feature:search:tv"))
    implementation(project(":feature:collections:tv"))
    implementation(project(":feature:library:tv"))
    implementation(project(":feature:profile:tv"))
    implementation(project(":feature:details:tv"))
    implementation(project(":feature:player:tv"))

    // Compose
    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.bundles.compose)
    implementation(libs.activity.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // In-app update: разбор ответа GitHub Releases.
    implementation(libs.kotlinx.serialization.json)

    // Crashlytics: SDK подключён всегда (код компилируется без google-services.json),
    // но без конфига Firebase не инициализируется и репортинг остаётся no-op.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // UI-тесты ТВ-оболочки на JVM: Robolectric + ui-test, данные — из фейкового бэкенда, как
    // в мок-сборке.
    testImplementation(project(":data:fake"))
    testImplementation(bom)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlin.test.junit)
    debugImplementation(libs.compose.ui.test.manifest)
}
