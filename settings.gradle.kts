plugins {
    id("de.fayard.refreshVersions") version "0.60.5"
}

rootProject.name = "runtimesave"
include("shared", "starter", "plugin")
project(":shared").name = "runtimesave-shared"
project(":starter").name = "runtimesave-starter"
project(":plugin").name = "runtimesave-plugin"