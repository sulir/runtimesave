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
    implementation(project(":runtimesave-graph"))
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

tasks.register<Copy>("copyNativeToSandbox") {
    dependsOn(":runtimesave-collect:buildNativeAgent")
    from(rootDir.resolve("dist"))
    include("*.so", "*.dll", "*.dylib")
    into(tasks.prepareSandbox.get().pluginDirectory.dir("lib").get())
}

tasks.prepareSandbox {
    dependsOn(":runtimesave-instrument:jar")
    dependsOn(":runtimesave-starter:jar")
    finalizedBy("copyNativeToSandbox")
}

tasks.prepareJarSearchableOptions {
    dependsOn("copyNativeToSandbox")
}

tasks.buildPlugin {
    destinationDirectory = project.rootProject.file("dist")
}

tasks.runIde {
    jvmArgs("-Didea.load.plugins.id=io.github.sulir.runtimesave,org.jetbrains.idea.maven,com.intellij.gradle," +
            "JUnit,ByteCodeViewer,org.jetbrains.java.decompiler,com.intellij.properties")
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
