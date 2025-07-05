group = project.property("project.group") as String
version = project.property("project.version") as String

plugins {
    java
    id("com.gradleup.shadow")
}

apply(plugin = "java")
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:_")
    implementation("org.ow2.asm:asm-tree:_")
    implementation("org.ow2.asm:asm-util:_")
    testImplementation("org.junit.jupiter:junit-jupiter:_")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:_")
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

tasks.test {
    useJUnitPlatform()
}

tasks.clean {
    delete(distDir.resolve(agentArchive))
}
