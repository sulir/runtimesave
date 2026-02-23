plugins {
    java
    id("com.gradleup.shadow")
}

java.sourceCompatibility = JavaVersion.VERSION_21

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

    from(resources.text.fromUri(javaClass.getResource("/LICENSE-ASM.txt")!!.toURI())) {
        rename { "META-INF/LICENSE-ASM.txt" }
    }
}

tasks.jar {
    enabled = false
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

tasks.clean {
    delete(distDir.resolve(agentArchive))
}
