@file:Suppress("UnstableApiUsage")

rootProject.name = "Aevum"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        mavenCentral()
        includeBuild("build-logic")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":app")
include(":db")
include(":common")
include(":ui")
include(":domain")
include(":network")

