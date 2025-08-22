group = project.property("project.group") as String
version = project.property("project.version") as String

plugins {
    java
    id("org.jetbrains.intellij.platform")
}

apply(plugin = "java")
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
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
    testImplementation("org.junit.jupiter:junit-jupiter:_")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:_")
    testRuntimeOnly("junit:junit:_")

    runtimeOnly(project(":runtimesave-starter")) {
        isTransitive = false
    }
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
    dependsOn(":runtimesave-starter:jar")
    destinationDirectory = project.rootProject.file("dist")
}

tasks.runIde {
    dependsOn(":runtimesave-starter:jar")
    maxHeapSize = "12g"
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
