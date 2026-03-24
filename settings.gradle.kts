dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("build-logic")

rootProject.name = "runtimesave"
include("collect", "graph", "instrument", "starter", "plugin")
project(":collect").name = "runtimesave-collect"
project(":graph").name = "runtimesave-graph"
project(":instrument").name = "runtimesave-instrument"
project(":starter").name = "runtimesave-starter"
project(":plugin").name = "runtimesave-plugin"
