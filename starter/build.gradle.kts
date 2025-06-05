plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation("org.ow2.asm:asm:_")
    implementation("org.ow2.asm:asm-tree:_")
    implementation("org.ow2.asm:asm-util:_")
}

val distDir = project.rootProject.file("dist")
val agentArchive = project.name + ".jar"

tasks.shadowJar {
    destinationDirectory = distDir
    archiveFileName = agentArchive
    relocate("org.objectweb.asm", "com.github.sulir.runtimesave.renamed.asm")

    manifest {
        attributes(mapOf(
            "Premain-Class" to "com.github.sulir.runtimesave.starter.StarterAgent"
        ))
    }
}

tasks.jar {
    destinationDirectory = distDir
    archiveFileName = agentArchive
    enabled = false
    dependsOn(tasks.shadowJar)
}

tasks.clean {
    delete(distDir.resolve(agentArchive))
}
