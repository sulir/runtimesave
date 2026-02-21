plugins {
    id("de.fayard.refreshVersions") version "0.60.6"
}

refreshVersions {
    rejectVersionIf {
        @Suppress("UnstableApiUsage")
        candidate.stabilityLevel != de.fayard.refreshVersions.core.StabilityLevel.Stable
    }
}

includeBuild("build-logic")

rootProject.name = "runtimesave"
include("instrument", "starter", "plugin")
project(":instrument").name = "runtimesave-instrument"
project(":starter").name = "runtimesave-starter"
project(":plugin").name = "runtimesave-plugin"
