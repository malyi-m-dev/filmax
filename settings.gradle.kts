pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "filmax"

// Кастомные detekt-правила (чистый JVM-модуль, подключается через detektPlugins).
include(":detekt-rules")

include(":app")
// Замеры кадров и генерация Baseline Profile на сборке app `benchmark`.
include(":benchmark")

include(":core:network")
include(":core:domain")
include(":core:designsystem")
include(":core:tv-designsystem")
include(":core:ui")
include(":core:presentation")
include(":core:navigation")
// Общее для UI-тестов: фейковые картинки и прочие подмены окружения.
include(":core:testing")

include(":data:auth")
include(":data:catalog")
include(":data:search")
include(":data:user")
include(":data:watching")
include(":data:tmdb")
include(":data:feedback")
// Телефон ↔ ТВ в одной сети: поиск, проверка аккаунта, команды пульта.
include(":data:tvlink")
// Фейковый data-слой без сети: сборка `mock` и юнит-тесты ScreenModel'ей.
include(":data:fake")

include(":feature:onboarding:common")
include(":feature:onboarding:mobile")
include(":feature:onboarding:tv")
include(":feature:home:common")
include(":feature:home:mobile")
include(":feature:home:tv")
include(":feature:search:common")
include(":feature:search:mobile")
include(":feature:search:tv")
include(":feature:collections:common")
include(":feature:collections:mobile")
include(":feature:collections:tv")
include(":feature:library:common")
include(":feature:library:mobile")
include(":feature:library:tv")
include(":feature:profile:common")
include(":feature:profile:mobile")
include(":feature:profile:tv")
include(":feature:feedback:common")
include(":feature:feedback:mobile")
include(":feature:feedback:tv")
include(":feature:details:common")
include(":feature:details:mobile")
include(":feature:details:tv")
include(":feature:player:common")
include(":feature:player:mobile")
include(":feature:player:tv")
include(":feature:tvlink:common")
include(":feature:tvlink:mobile")
include(":feature:tvlink:tv")
include(":feature:designsystem")

include(":shared")
