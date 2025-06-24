import org.gradle.kotlin.dsl.dir
import org.gradle.kotlin.dsl.flatDir

include(":mnn-base")


include(":sherpa")


include(":mnnui")


include(":webserver")


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

rootProject.name = "MNN Server"
include(":app")
include(":server")
include(":webserver")
include(":mnnui")
