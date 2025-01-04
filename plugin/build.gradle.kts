plugins {
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("com.intellij.java")
    }

    implementation("org.neo4j.driver:neo4j-java-driver:${project.ext["neo4jVersion"]}")

    implementation(project(":runtimesave-shared"))
    runtimeOnly(project(":runtimesave-agent")) {
        isTransitive = false
    }
}

intellijPlatform {
    buildSearchableOptions = false
}

tasks.assemble {
    dependsOn(tasks.buildPlugin)
}

tasks.buildPlugin {
    dependsOn(":runtimesave-agent:jar")
    destinationDirectory = project.rootProject.file("dist")
}

tasks.runIde {
    dependsOn(":runtimesave-agent:jar")
}

tasks.clean {
    delete(tasks.buildPlugin.get().archiveFile)
}
