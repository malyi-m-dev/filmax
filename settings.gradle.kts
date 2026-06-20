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
include(":core:ui")

include(":data:auth")
include(":data:catalog")
include(":data:search")
include(":data:user")
include(":data:watching")

include(":feature:onboarding")
include(":feature:home")
include(":feature:search")
include(":feature:categories")
include(":feature:library")
include(":feature:profile")
include(":feature:details")
include(":feature:player")
