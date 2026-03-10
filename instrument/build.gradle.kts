plugins {
    id("java-agent")
}

tasks.shadowJar {
    manifest {
        attributes(mapOf(
            "Premain-Class" to "com.github.sulir.runtimesave.instrument.InstrumentAgent"
        ))
    }
}
