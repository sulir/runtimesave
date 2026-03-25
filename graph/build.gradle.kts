plugins {
    java
}

java.sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())

dependencies {
    implementation(libs.neo4j.driver)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    compileOnly(libs.jetbrains.annotations)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<DefaultTask>("printTestClasspath") {
    doLast{
        println(project.extensions.getByType(SourceSetContainer::class.java)["test"].runtimeClasspath.asPath)
    }
}
