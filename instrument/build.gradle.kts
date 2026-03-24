plugins {
    id("java-agent")
}

dependencies {
    implementation(project(":runtimesave-graph"))
}

tasks.shadowJar {
    addAgentPackage("com.github.sulir.runtimesave")
    addAgentPackage("io.netty", "netty")
    addAgentPackage("org.neo4j", "neo4j")
    addAgentPackage("org.reactivestreams", "reactivestreams")
    addAgentPackage("reactor", "reactor")

    exclude("META-INF/native-image/**")
    exclude("META-INF/services/**")
    exclude("META-INF/*.versions.properties")

    manifest {
        attributes(mapOf(
            "Premain-Class" to "com.github.sulir.runtimesave.instrument.InstrumentAgent"
        ))
    }
}
