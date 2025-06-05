plugins {
    id("de.fayard.refreshVersions") version "0.60.5"
}

refreshVersions {
    rejectVersionIf {
        @Suppress("UnstableApiUsage")
        candidate.stabilityLevel != de.fayard.refreshVersions.core.StabilityLevel.Stable
    }
}

rootProject.name = "runtimesave"
include("starter", "plugin")
project(":starter").name = "runtimesave-starter"
project(":plugin").name = "runtimesave-plugin"
