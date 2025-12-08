pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "Echo"

// App module
include(":app")

// Core modules
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:ui")
include(":core:media")

// Feature modules
include(":feature:server")
include(":feature:auth")
include(":feature:home")
include(":feature:albums")
include(":feature:artists")
include(":feature:player")
include(":feature:playlists")
include(":feature:search")
