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

    implementation(project(":runtimesave-shared"))
    runtimeOnly(project(":runtimesave-starter")) {
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
    dependsOn(":runtimesave-starter:jar")
    destinationDirectory = project.rootProject.file("dist")
}

tasks.runIde {
    dependsOn(":runtimesave-starter:jar")
}

tasks.clean {
    delete(tasks.buildPlugin.get().archiveFile)
}
