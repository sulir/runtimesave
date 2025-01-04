rootProject.name = "runtimesave"
include("shared", "agent", "plugin")
project(":shared").name = "runtimesave-shared"
project(":agent").name = "runtimesave-agent"
project(":plugin").name = "runtimesave-plugin"