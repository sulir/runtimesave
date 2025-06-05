plugins {
    id("org.jetbrains.intellij.platform")
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("_")
        bundledPlugin("com.intellij.java")
    }
    implementation("org.neo4j.driver:neo4j-java-driver:_")

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

configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor(7, "days")
        cacheChangingModulesFor(7, "days")
    }
}
