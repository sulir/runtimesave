plugins {
    java
    alias(libs.plugins.intellij.platform)
}

java.sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.intellij.idea)
        bundledPlugin("com.intellij.java")
    }

    implementation(libs.neo4j.driver)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit4)

    runtimeOnly(project(":runtimesave-instrument")) { isTransitive = false }
    runtimeOnly(project(":runtimesave-starter")) { isTransitive = false }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.assemble {
    dependsOn(tasks.buildPlugin)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(rootProject.file("LICENSE.txt")) {
        into("META-INF/")
    }
}

tasks.buildPlugin {
    dependsOn(":runtimesave-instrument:jar")
    dependsOn(":runtimesave-starter:jar")
    destinationDirectory = project.rootProject.file("dist")
}

tasks.runIde {
    dependsOn(":runtimesave-instrument:jar")
    dependsOn(":runtimesave-starter:jar")
    jvmArgs("-Didea.load.plugins.id=com.github.sulir.runtimesave")
}

tasks.test {
    useJUnitPlatform()
}

tasks.clean {
    delete(tasks.buildPlugin.get().archiveFile)
}

tasks.register<DefaultTask>("printTestClasspath") {
    doLast{
        println(project.extensions.getByType(SourceSetContainer::class.java)["test"].runtimeClasspath.asPath)
    }
}

configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor(7, "days")
        cacheChangingModulesFor(7, "days")
    }
}
