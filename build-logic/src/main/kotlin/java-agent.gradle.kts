val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

plugins {
    java
    id("com.gradleup.shadow")
}

java.sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())

dependencies {
    implementation(libs.asm)
    implementation(libs.asm.tree)
    implementation(libs.asm.util)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
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
