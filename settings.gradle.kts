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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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
include(":wear")

