plugins {
    id("java-agent")
}

tasks.shadowJar {
    addAgentPackage("io.github.sulir.runtimesave")

    manifest {
        attributes(mapOf(
            "Premain-Class" to "io.github.sulir.runtimesave.starter.StarterAgent"
        ))
    }
}
