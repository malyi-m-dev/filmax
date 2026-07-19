import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("filmax.detekt")
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

    // Data
    implementation(project(":data:auth"))
    implementation(project(":data:catalog"))
    implementation(project(":data:search"))
    implementation(project(":data:user"))
    implementation(project(":data:watching"))
    implementation(project(":data:tmdb"))

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

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}
