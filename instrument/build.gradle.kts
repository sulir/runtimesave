plugins {
    id("java-agent")
}

dependencies {
    implementation(libs.asm.analysis)
}

tasks.shadowJar {
    manifest {
        attributes(mapOf(
            "Premain-Class" to "com.github.sulir.runtimesave.instrument.InstrumentAgent"
        ))
    }
}
