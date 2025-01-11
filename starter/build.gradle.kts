plugins {
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    implementation("org.neo4j.driver:neo4j-java-driver:${project.ext["neo4jVersion"]}")
    implementation("org.ow2.asm:asm:${project.ext["asmVersion"]}")
    implementation("org.ow2.asm:asm-tree:${project.ext["asmVersion"]}")

    implementation(project(":runtimesave-shared"))
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