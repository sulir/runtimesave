plugins {
    id("java-agent")
}

tasks.shadowJar {
    addAgentPackage("com.github.sulir.runtimesave")

    manifest {
        attributes(mapOf(
            "Premain-Class" to "com.github.sulir.runtimesave.starter.StarterAgent"
        ))
    }
}
