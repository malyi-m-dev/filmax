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

include(":app")

include(":core:network")
include(":core:domain")
include(":core:designsystem")
include(":core:tv-designsystem")
include(":core:ui")
include(":core:presentation")

include(":data:auth")
include(":data:catalog")
include(":data:search")
include(":data:user")
include(":data:watching")

include(":feature:onboarding")
include(":feature:onboarding:mobile")
include(":feature:onboarding:tv")
include(":feature:home")
include(":feature:home:mobile")
include(":feature:home:tv")
include(":feature:search")
include(":feature:search:mobile")
include(":feature:search:tv")
include(":feature:collections")
include(":feature:collections:mobile")
include(":feature:collections:tv")
include(":feature:library")
include(":feature:library:mobile")
include(":feature:library:tv")
include(":feature:profile")
include(":feature:profile:mobile")
include(":feature:profile:tv")
include(":feature:details")
include(":feature:details:mobile")
include(":feature:details:tv")
include(":feature:player")
include(":feature:player:mobile")
include(":feature:player:tv")
include(":feature:designsystem")

include(":shared")
