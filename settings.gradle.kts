dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("build-logic")

rootProject.name = "runtimesave"
include("instrument", "collect", "starter", "plugin")
project(":instrument").name = "runtimesave-instrument"
project(":collect").name = "runtimesave-collect"
project(":starter").name = "runtimesave-starter"
project(":plugin").name = "runtimesave-plugin"
