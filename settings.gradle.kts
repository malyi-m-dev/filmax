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
include(":app-tv")

include(":core:network")
include(":core:domain")
include(":core:designsystem")
include(":core:tv-designsystem")
include(":core:ui")
include(":core:presentation")

include(":feature-tv:onboarding")
include(":feature-tv:search")
include(":feature-tv:categories")
include(":feature-tv:library")
include(":feature-tv:profile")
include(":feature-tv:details")
include(":feature-tv:player")

include(":data:auth")
include(":data:catalog")
include(":data:search")
include(":data:user")
include(":data:watching")

include(":feature:onboarding")
include(":feature:home")
include(":feature:home:mobile")
include(":feature:home:tv")
include(":feature:search")
include(":feature:collections")
include(":feature:library")
include(":feature:profile")
include(":feature:details")
include(":feature:player")
include(":feature:designsystem")

include(":shared")
