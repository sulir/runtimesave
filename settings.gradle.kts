dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("build-logic")

rootProject.name = "runtimesave"
include("instrument", "starter", "plugin")
project(":instrument").name = "runtimesave-instrument"
project(":starter").name = "runtimesave-starter"
project(":plugin").name = "runtimesave-plugin"
